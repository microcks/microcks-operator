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
package io.github.microcks.operator;

import io.github.microcks.client.ApiClient;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.api.model.StatusPreserving;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;

/**
 * Abstract class that provides common methods for reconcilers that depend on a Microcks instance.
 * @param <R> The custom resource type for the reconciler implementation
 * @param <S> The spec type for the custom resource
 * @param <T> The status type for the custom resource - must implement StatusPreserving
 */
public abstract class AbstractMicrocksDependantReconciler<R extends CustomResource<S, T>, S, T extends StatusPreserving> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   protected static final String API_EXCEPTION_ERROR_LOG = "Message '%s' and response body '%s'";

   protected KubernetesClient client;
   protected KeycloakHelper keycloakHelper;

   /**
    * Hook for extensions to provide the concrete implementation of custom resource.
    */
   protected abstract R buildCustomResourceInstance();

   /**
    * For a given custom resource, prepare the reconciliation by checking that the Microcks instance
    * is specified, exists and is ready in the current namespace.
    * @param customResource The custom resource to reconcile
    * @return Either an UpdateControl is something goes wrong or the target Microcks instance.
    */
   public UpdateControlOrMicrocks<R> prepareReconciliationWithMicrocksInstance(R customResource) {
      // Check that microcks instance specification is there.
      Map<String, String> annotations = customResource.getMetadata().getAnnotations();
      String microcksName = annotations.get(MicrocksOperatorConfig.INSTANCE_SELECTOR);
      if (microcksName == null) {
         logger.errorf("No Microcks instance specified for %s '%s'", customResource.getKind(), customResource.getMetadata().getName());
         customResource.getStatus().setStatus(Status.ERROR);
         customResource.getStatus().setMessage("No Microcks instance specified for APISource. Expected annotation 'microcks.io/instance'");
         return new UpdateControlOrMicrocks<>(UpdateControl.patchStatus(prepareCustomResourceForStatusPatch(customResource)), null);
      }

      MixedOperation<Microcks, KubernetesResourceList<Microcks>, Resource<Microcks>> microcksClient = client.resources(Microcks.class);

      // Check that microcks instance is found in current namespace.
      Microcks microcks = microcksClient.inNamespace(customResource.getMetadata().getNamespace()).withName(microcksName).get();
      if (microcks == null) {
         logger.errorf("No Microcks instance found for %s '%s'", customResource.getKind(), customResource.getMetadata().getName());
         customResource.getStatus().setStatus(Status.ERROR);
         customResource.getStatus().setMessage("No Microcks instance found for " + customResource.getKind() + ". Annotation 'microcks.io/instance' doesn't refer an existing instance");
         return new UpdateControlOrMicrocks<>(UpdateControl.patchStatus(prepareCustomResourceForStatusPatch(customResource)), null);
      }

      // Check that microcks instance is in ready status.
      if (microcks.getStatus().getStatus() != Status.READY) {
         logger.errorf("Microcks instance '%s' is not yet ready for %s '%s'", microcksName, customResource.getKind(), customResource.getMetadata().getName());
         customResource.getStatus().setStatus(Status.ERROR);
         customResource.getStatus().setMessage("Microcks instance is not yet ready for " + customResource.getKind() + ". Current status is " + microcks.getStatus().getStatus());
         return new UpdateControlOrMicrocks<>(UpdateControl.patchStatus(prepareCustomResourceForStatusPatch(customResource)).rescheduleAfter(Duration.ofSeconds(5)), null);
      }

      return new UpdateControlOrMicrocks<>(null, microcks);
   }

   /**
    * For a given custom resource, prepare the cleanup by checking that the Microcks instance
    * is specified and exists in the current namespace.
    * @param customResource The custom resource to cleanup
    * @return Either a DeleteControl is something goes wrong or the target Microcks instance.
    */
   public DeleteControlOrMicrocks prepareCleanupWithMicrocksInstance(R customResource) {
      // Check that microcks instance specification is there.
      Map<String, String> annotations = customResource.getMetadata().getAnnotations();
      String microcksName = annotations.get(MicrocksOperatorConfig.INSTANCE_SELECTOR);
      if (microcksName == null) {
         logger.errorf("No Microcks instance specified for %s '%s'", customResource.getKind(), customResource.getMetadata().getName());
         return new DeleteControlOrMicrocks(DeleteControl.defaultDelete(), null);
      }

      MixedOperation<Microcks, KubernetesResourceList<Microcks>, Resource<Microcks>> microcksClient = client.resources(Microcks.class);

      // Check that microcks instance is found in current namespace.
      Microcks microcks = microcksClient.inNamespace(customResource.getMetadata().getNamespace()).withName(microcksName).get();
      if (microcks == null) {
         logger.errorf("No Microcks instance found for %s '%s'", customResource.getKind(), customResource.getMetadata().getName());
         return new DeleteControlOrMicrocks(DeleteControl.defaultDelete(), null);
      }

      return new DeleteControlOrMicrocks(null, microcks);

   }

   /**
    * Build an ApiClient for a given custom resource and Microcks instance.
    * @param customResource The custom resource to get an ApiClient for
    * @param microcks The target Microcks instance
    * @return Either an UpdateControl is something goes wrong or the ApiClient to interact with Microcks API.
    */
   public UpdateControlOrApiClient<R> buildApiClient(R customResource, Microcks microcks) {
      // Retrieve an authentication token from associated Keycloak.
      String oauthToken;
      try {
         oauthToken = keycloakHelper.getOAuthToken(customResource.getMetadata(), microcks);
      } catch (UnsatisfiedRequirementException ure) {
         logger.errorf("Unsatisfied requirement for connecting to Keycloak: %s", ure.getMessage());
         return new UpdateControlOrApiClient<>(UpdateControl.patchStatus(prepareCustomResourceForStatusPatch(customResource)).rescheduleAfter(Duration.ofSeconds(120)), null);
      } catch (Exception e) {
         logger.errorf("Error while getting OAuth token for Keycloak server: %s", e.getMessage());
         return new UpdateControlOrApiClient<>(UpdateControl.patchStatus(prepareCustomResourceForStatusPatch(customResource)).rescheduleAfter(Duration.ofSeconds(10)), null);
      }

      // Build a needed ApiCLient to interact with Microcks API.
      ApiClient apiClient = new ApiClient();
      apiClient.updateBaseUri("http://" + microcks.getMetadata().getName() + "." + microcks.getMetadata().getNamespace()
            + ".svc."  + microcks.getSpec().getClusterDomain() + ":8080/api");
      apiClient.setRequestInterceptor(request -> request.header("Authorization", "Bearer " + oauthToken));

      return new UpdateControlOrApiClient<>(null, apiClient);
   }

   public record UpdateControlOrMicrocks<R extends HasMetadata>(UpdateControl<R> updateControl, Microcks microcks) {
   }

   public record DeleteControlOrMicrocks(DeleteControl deleteControl, Microcks microcks) {
   }

   public record UpdateControlOrApiClient<R extends HasMetadata>(UpdateControl<R> updateControl, ApiClient apiClient) {
   }

   /** Applying the recipe from https://javaoperatorsdk.io/blog/2025/02/25/from-legacy-approach-to-server-side-apply */
   protected R prepareCustomResourceForStatusPatch(R customResource) {
      R customResourcePatch = buildCustomResourceInstance();
      customResourcePatch.setMetadata(new ObjectMetaBuilder()
            .withName(customResource.getMetadata().getName())
            .withNamespace(customResource.getMetadata().getNamespace())
            .withResourceVersion(customResource.getMetadata().getResourceVersion())
            .build());
      customResourcePatch.setStatus(customResource.getStatus());
      return customResourcePatch;
   }
}
