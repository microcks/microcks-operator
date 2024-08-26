/*
 * Copyright The Microcks Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.microcks.operator.secret;

import io.github.microcks.client.ApiClient;
import io.github.microcks.client.ApiException;
import io.github.microcks.client.api.ConfigApi;
import io.github.microcks.client.model.Secret;
import io.github.microcks.operator.AbstractMicrocksDependantReconciler;
import io.github.microcks.operator.KeycloakHelper;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.Condition;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.api.secret.v1alpha1.SecretSource;
import io.github.microcks.operator.api.secret.v1alpha1.SecretSourceSpec;
import io.github.microcks.operator.api.secret.v1alpha1.SecretSourceStatus;
import io.github.microcks.operator.api.secret.v1alpha1.SecretSpec;
import io.github.microcks.operator.api.secret.v1alpha1.SecretValuesFromSpec;
import io.github.microcks.operator.model.ConditionUtil;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

/**
 * Reconciliation entry point for the {@code SecretSource} Kubernetes custom resource.
 * @author laurent
 */
@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE)
@SuppressWarnings("unused")
@ApplicationScoped
public class SecretSourceReconciler extends AbstractMicrocksDependantReconciler<SecretSource, SecretSourceSpec, SecretSourceStatus>
      implements Reconciler<SecretSource>, Cleaner<SecretSource>, EventSourceInitializer<SecretSource> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   /**
    * Default constructor with injected Kubernetes client.
    * @param client A Kubernetes client for interacting with the cluster
    */
   public SecretSourceReconciler(KubernetesClient client) {
      this.client = client;
      this.keycloakHelper = new KeycloakHelper(client);
   }

   @Override
   public Map<String, EventSource> prepareEventSources(EventSourceContext<SecretSource> context) {
      /*
       * To create an event to a related SecretSource resource and trigger the reconciliation we need to
       * find which SecretSource this Secret custom resource is related to. To find the related customResourceId
       * of the SecretSource resource we traverse the cache and identify it based on naming convention.
       */
      final SecondaryToPrimaryMapper<io.fabric8.kubernetes.api.model.Secret> secretSourcesMatchingSecretName =
            (io.fabric8.kubernetes.api.model.Secret kubeSecret) -> context.getPrimaryCache()
                  .list(secretSource -> secretSource.getSpec().getSecrets().stream()
                        .filter(secretSpec -> secretSpec.getValuesFrom() != null)
                        .map(secretSpec -> secretSpec.getValuesFrom().getSecretRef())
                        .toList()
                        .contains(kubeSecret.getMetadata().getName()))
                  .map(ResourceID::fromResource)
                  .collect(Collectors.toSet());

      InformerConfiguration<io.fabric8.kubernetes.api.model.Secret> configuration =
            InformerConfiguration.from(io.fabric8.kubernetes.api.model.Secret.class, context)
                  .withSecondaryToPrimaryMapper(secretSourcesMatchingSecretName)
                  .withPrimaryToSecondaryMapper(
                        (SecretSource primary) -> primary.getSpec().getSecrets().stream()
                              .filter(secretSpec -> secretSpec.getValuesFrom() != null)
                              .map(secretSpec -> new ResourceID(secretSpec.getValuesFrom().getSecretRef(), primary.getMetadata().getNamespace()))
                              .collect(Collectors.toSet())
                  )
                  .build();

      return EventSourceInitializer
            .nameEventSources(new InformerEventSource<>(configuration, context));
   }

   @Override
   public UpdateControl<SecretSource> reconcile(SecretSource secretSource, Context<SecretSource> context) throws Exception {
      final String ns = secretSource.getMetadata().getNamespace();
      final SecretSourceSpec spec = secretSource.getSpec();

      boolean updateStatus = false;
      // Set a minimal status if not present.
      if (secretSource.getStatus() == null) {
         secretSource.setStatus(new SecretSourceStatus());
         updateStatus = true;
      }

      logger.infof("Starting reconcile operation for '%s'", secretSource.getMetadata().getName());

      // Check that microcks instance specification is there.
      UpdateControlOrMicrocks<SecretSource> preparationControl = prepareReconciliationWithMicrocksInstance(secretSource);
      if (preparationControl.updateControl() != null) {
         return preparationControl.updateControl();
      }

      // Now we have a Microcks instance that is ready to receive API requests.
      Microcks microcks = preparationControl.microcks();

      // Build an ApiClient for Microcks instance.
      UpdateControlOrApiClient<SecretSource> apiClientControl = buildApiClient(secretSource, microcks);
      if (apiClientControl.updateControl() != null) {
         return apiClientControl.updateControl();
      }

      // Now we have an authenticated & ready to use ApiClient for Microcks instance.
      ApiClient apiClient = apiClientControl.apiClient();

      // Deal with secrets specifications.
      for (SecretSpec secretSpec : spec.getSecrets()) {
         Condition condition = ConditionUtil.getOrCreateCondition(secretSource.getStatus(), secretSpec.getName());

         // Check if we have to load values from a Kubernetes secret.
         io.fabric8.kubernetes.api.model.Secret kubeSecret = null;
         if (secretSpec.getValuesFrom() != null) {
            SecretValuesFromSpec valuesFromSpec = secretSpec.getValuesFrom();
            kubeSecret = client.secrets().inNamespace(ns).withName(valuesFromSpec.getSecretRef()).get();

            if (kubeSecret == null) {
               logger.errorf("Kubernetes secret '%s' not found for '%s' in SecretSource '%s'",
                     valuesFromSpec.getSecretRef(), secretSpec.getName(), secretSource.getMetadata().getName());
               condition.setStatus(Status.ERROR);
               condition.setMessage("Kubernetes secret '" + valuesFromSpec.getSecretRef() + "' not found");
               secretSource.getStatus().setStatus(Status.ERROR);

               // Don't forget to touch condition time before jumping to next iteration.
               ConditionUtil.touchConditionTime(condition);
               updateStatus = true;
               continue;
            }
         }

         try {
            // Previously created secret id may be stored within condition message.
            String previousId = getSecretIdOrNull(condition);
            String secretId = ensureSecretIsPresent(apiClient, secretSpec, kubeSecret, previousId);
            condition.setStatus(Status.READY);
            // TODO: Store secretId in condition additional property instead.
            condition.setMessage(secretId);
         } catch (ApiException e) {
            logger.errorf("Error while loading secret '%s' for SecretSource '%s'", secretSpec.getName(), secretSource.getMetadata().getName());
            logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
            secretSource.getStatus().setStatus(Status.ERROR);
            condition.setStatus(Status.ERROR);
         }

         ConditionUtil.touchConditionTime(condition);
         updateStatus = true;
      }

      logger.infof("Finishing reconcile operation for '%s'", secretSource.getMetadata().getName());

      if (updateStatus) {
         logger.info("Returning an updateStatus control. ========================");
         checkIfGloballyReady(secretSource);
         return UpdateControl.updateStatus(secretSource);
      }

      logger.info("Returning a noUpdate control. =============================");
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(SecretSource secretSource, Context<SecretSource> context) {
      final String ns = secretSource.getMetadata().getNamespace();
      final SecretSourceSpec spec = secretSource.getSpec();
      logger.infof("Starting cleanup operation for '%s'", secretSource.getMetadata().getName());

      // Check that microcks instance specification is there.
      DeleteControlOrMicrocks preparationControl = prepareCleanupWithMicrocksInstance(secretSource);
      if (preparationControl.deleteControl() != null) {
         return preparationControl.deleteControl();
      }

      // Now we have a Microcks instance that is at least present.
      Microcks microcks = preparationControl.microcks();

      // Check that microcks instance is in ready status and we have a status for SecretSourxe.
      if (microcks.getStatus().getStatus() == Status.READY && secretSource.getStatus() != null) {

         // Build an ApiClient for Microcks instance.
         UpdateControlOrApiClient<SecretSource> apiClientControl = buildApiClient(secretSource, microcks);
         if (apiClientControl.updateControl() != null) {
            logger.error("Rescheduling cleanup operation in 30 seconds");
            return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(30));
         }

         // Now we have an authenticated & ready to use ApiClient for Microcks instance.
         ApiClient apiClient = apiClientControl.apiClient();
         ConfigApi configApi = new ConfigApi(apiClient);

         // remove secrets from Microcks instance.
         for (SecretSpec secretSpec : spec.getSecrets()) {
            Condition condition = ConditionUtil.getOrCreateCondition(secretSource.getStatus(), secretSpec.getName());
            String secretId = getSecretIdOrNull(condition);

            if (secretId != null) {
               try {
                  configApi.deleteSecret(secretId);
               } catch (ApiException e) {
                  logger.errorf("Error while deleting secret '%s' for SecretSource '%s'", secretSpec.getName(), secretSource.getMetadata().getName());
                  logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
                  return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(30));
               }
            }
         }

         // If here then every importer has been removed!
         return DeleteControl.defaultDelete();
      }

      // Re-schedule cleanup operation in 30 seconds to wait for Microcks to be ready.
      logger.error("Rescheduling cleanup operation in 30 seconds");
      return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(30));
   }

   /** Get a secret id (or null if not exists) */
   protected String getSecretIdOrNull(Condition condition) {
      return condition.getMessage();
   }

   /** Ensure a secret exists by checking by id, updating if found or cre-creating if not found. */
   protected String ensureSecretIsPresent(ApiClient apiClient, SecretSpec secretSpec,
                                          io.fabric8.kubernetes.api.model.Secret kubeSecret, String previousId) throws ApiException {
      if (previousId != null) {
         // We have a previous secret id, we should check if it's still there.
         ConfigApi configApi = new ConfigApi(apiClient);
         Secret secret = configApi.getSecret(previousId);
         if (secret != null) {
            updateWithSecretSpec(secret, secretSpec, kubeSecret);
            configApi.updateSecret(previousId, secret);
            return previousId;
         }
      }
      return createSecret(apiClient, secretSpec, kubeSecret);
   }

   protected String createSecret(ApiClient apiClient, SecretSpec secretSpec, io.fabric8.kubernetes.api.model.Secret kubeSecret) throws ApiException {
      // Move SecretSpec into Microcks API model.
      Secret secret = new Secret();
      updateWithSecretSpec(secret, secretSpec, kubeSecret);
      // Use the apiClient to create the secret.
      ConfigApi configApi = new ConfigApi(apiClient);
      secret = configApi.createSecret(secret);
      return secret.getId();
   }

   protected void updateWithSecretSpec(Secret secret, SecretSpec secretSpec, io.fabric8.kubernetes.api.model.Secret kubeSecret) {
      secret.setName(secretSpec.getName());
      secret.setDescription(secretSpec.getDescription());
      if (secretSpec.getValuesFrom() != null) {
         SecretValuesFromSpec valuesFromSpec = secretSpec.getValuesFrom();

         // Copy from Kubernetes secret if key is present. Don't forget to decode it.
         if (shouldCopyFromSecret(valuesFromSpec.getUsernameKey(), kubeSecret)) {
            secret.setUsername(decodeSecretValue(kubeSecret.getData().get(valuesFromSpec.getUsernameKey())));
         }
         if (shouldCopyFromSecret(valuesFromSpec.getPasswordKey(), kubeSecret)) {
            secret.setPassword(decodeSecretValue(kubeSecret.getData().get(valuesFromSpec.getPasswordKey())));
         }
         if (shouldCopyFromSecret(valuesFromSpec.getTokenKey(), kubeSecret)) {
            secret.setToken(decodeSecretValue(kubeSecret.getData().get(valuesFromSpec.getTokenKey())));
         }
         if (shouldCopyFromSecret(valuesFromSpec.getTokenHeaderKey(), kubeSecret)) {
            secret.setTokenHeader(decodeSecretValue(kubeSecret.getData().get(valuesFromSpec.getTokenHeaderKey())));
         }
         if (shouldCopyFromSecret(valuesFromSpec.getCaCertPermKey(), kubeSecret)) {
            secret.setCaCertPem(decodeSecretValue(kubeSecret.getData().get(valuesFromSpec.getCaCertPermKey())));
         }
      } else {
         // Simply copy values from specification.
         secret.setUsername(secretSpec.getUsername());
         secret.setPassword(secretSpec.getPassword());
         secret.setToken(secretSpec.getToken());
         secret.setTokenHeader(secretSpec.getTokenHeader());
         secret.setCaCertPem(secretSpec.getCaCertPem());
      }
   }

   protected void checkIfGloballyReady(SecretSource secretSource) {
      boolean allReady = true;
      for (Condition condition : secretSource.getStatus().getConditions()) {
         if (condition.getStatus() != Status.READY) {
            allReady = false;
            break;
         }
      }
      if (allReady) {
         secretSource.getStatus().setStatus(Status.READY);
      }
   }

   private static boolean shouldCopyFromSecret(String key, io.fabric8.kubernetes.api.model.Secret kubeSecret) {
      return key != null && kubeSecret.getData().containsKey(key);
   }

   private static String decodeSecretValue(String encodedValue) {
      return new String(Base64.getDecoder().decode(encodedValue));
   }
}
