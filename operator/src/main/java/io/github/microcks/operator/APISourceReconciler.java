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
import io.github.microcks.client.ApiException;
import io.github.microcks.client.api.JobApi;
import io.github.microcks.client.model.ImportJob;
import io.github.microcks.client.model.Metadata;
import io.github.microcks.client.model.SecretRef;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.model.Condition;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.api.source.v1alpha1.APISource;
import io.github.microcks.operator.api.source.v1alpha1.APISourceSpec;
import io.github.microcks.operator.api.source.v1alpha1.APISourceStatus;
import io.github.microcks.operator.api.source.v1alpha1.ArtifactSpec;
import io.github.microcks.operator.api.source.v1alpha1.ImporterSpec;
import io.github.microcks.operator.model.ConditionUtil;
import io.github.microcks.operator.model.ResourceMerger;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

/**
 * Reconciliation entry point for the {@code APISource} Kubernetes custom resource.
 * @author laurent
 */
@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE)
@SuppressWarnings("unused")
@ApplicationScoped
public class APISourceReconciler implements Reconciler<APISource>, Cleaner<APISource> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String API_EXCEPTION_ERROR_LOG = "Message '%s' and response body '%s'";

   final KubernetesClient client;

   private final ResourceMerger merger = new ResourceMerger();
   private final KeycloakHelper keycloakHelper;

   /**
    * Default constructor with injected Kubernetes client.
    * @param client A Kubernetes client for interacting with the cluster
    */
   public APISourceReconciler(KubernetesClient client) {
      this.client = client;
      this.keycloakHelper = new KeycloakHelper(client);
   }

   @Override
   public UpdateControl<APISource> reconcile(APISource apisource, Context<APISource> context) throws Exception {
      final String ns = apisource.getMetadata().getNamespace();
      final APISourceSpec spec = apisource.getSpec();

      boolean updateStatus = false;
      // Set a minimal status if not present.
      if (apisource.getStatus() == null) {
         apisource.setStatus(new APISourceStatus());
         updateStatus = true;
      }

      logger.infof("Starting reconcile operation for '%s'", apisource.getMetadata().getName());

      // Check that microcks instance specification is there.
      Map<String, String> annotations = apisource.getMetadata().getAnnotations();
      String microcksName = annotations.get(MicrocksOperatorConfig.INSTANCE_SELECTOR);
      if (microcksName == null) {
         logger.errorf("No Microcks instance specified for APISource '%s'", apisource.getMetadata().getName());
         apisource.getStatus().setStatus(Status.ERROR);
         apisource.getStatus().setMessage("No Microcks instance specified for APISource. Expected annotation 'microcks.io/instance'");
         return UpdateControl.updateStatus(apisource);
      }

      MixedOperation<Microcks, KubernetesResourceList<Microcks>, Resource<Microcks>> microcksClient = client.resources(Microcks.class);

      // Check that microcks instance is found in current namespace.
      Microcks microcks = microcksClient.inNamespace(ns).withName(microcksName).get();
      if (microcks == null) {
         logger.errorf("No Microcks instance found for APISource '%s'", apisource.getMetadata().getName());
         apisource.getStatus().setStatus(Status.ERROR);
         apisource.getStatus().setMessage("No Microcks instance found for APISource. Annotation 'microcks.io/instance' doesn't refer an existing instance");
         return UpdateControl.updateStatus(apisource);
      }

      // Check that microcks instance is in ready status.
      if (microcks.getStatus().getStatus() != Status.READY) {
         logger.errorf("Microcks instance '%s' is not yet ready for APISource '%s'", microcksName, apisource.getMetadata().getName());
         apisource.getStatus().setStatus(Status.ERROR);
         apisource.getStatus().setMessage("Microcks instance is not yet ready for APISource. Current status is " + microcks.getStatus().getStatus());
         return UpdateControl.updateStatus(apisource).rescheduleAfter(Duration.ofSeconds(5));
      }

      // Load default values for CR and build a complete representation.
      Microcks defaultCR = loadDefaultMicrocksCR();

      MicrocksSpec completeSpec = merger.mergeResources(defaultCR.getSpec(), microcks.getSpec());
      Microcks completeCR = new Microcks();
      completeCR.setKind(microcks.getKind());
      completeCR.setMetadata(microcks.getMetadata());
      completeCR.setSpec(completeSpec);
      completeCR.setStatus(microcks.getStatus());

      // Retrieve an authentication token from associated Keycloak.
      String oauthToken;
      try {
         oauthToken = keycloakHelper.getOAuthToken(apisource.getMetadata(), completeCR);
      } catch (UnsatisfiedRequirementException ure) {
         logger.errorf("Unsatisfied requirement for connecting to Keycloak: %s", ure.getMessage());
         return UpdateControl.updateStatus(apisource).rescheduleAfter(Duration.ofSeconds(120));
      } catch (Exception e) {
         logger.errorf("Error while getting OAuth token for Keycloak server: %s", e.getMessage());
         return UpdateControl.updateStatus(apisource).rescheduleAfter(Duration.ofSeconds(10));
      }

      // Build a needed ApiCLient to interact with Microcks API.
      ApiClient apiClient = new ApiClient();
      apiClient.updateBaseUri("http://" + microcks.getMetadata().getName() + "." + microcks.getMetadata().getNamespace() + ".svc.cluster.local:8080/api");
      apiClient.setRequestInterceptor(request -> request.header("Authorization", "Bearer " + oauthToken));

      // Deal with artifact specifications.
      for (ArtifactSpec artifactSpec : spec.getArtifacts()) {
         Condition condition = ConditionUtil.getOrCreateCondition(apisource.getStatus(), artifactSpec.getUrl());

         try {
            ensureArtifactIsLoaded(apiClient, artifactSpec);
            condition.setStatus(Status.READY);
         } catch (ApiException e) {
            logger.errorf("Error while loading artifact '%s' for APISource '%s'", artifactSpec.getUrl(), apisource.getMetadata().getName());
            logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
            apisource.getStatus().setStatus(Status.ERROR);
            condition.setStatus(Status.ERROR);
         }

         ConditionUtil.touchConditionTime(condition);
         updateStatus = true;
      }

      // Deal with importer specifications.
      for (ImporterSpec importerSpec : spec.getImporters()) {
         Condition condition = ConditionUtil.getOrCreateCondition(apisource.getStatus(), importerSpec.getRepository().getUrl());

         try {
            // Previously created job id may be stored within condition message.
            String previousId = getImporterIdOrNull(condition);
            String importerId = ensureImporterIsPresent(apiClient, importerSpec, previousId);
            condition.setStatus(Status.READY);
            // TODO: Store importerId in condition additional porperty
            //condition.setAdditionalProperty("importerId", importerId);
            condition.setMessage(importerId);
         } catch (ApiException e) {
            logger.errorf("Error while creating importer '%s' for APISource '%s'", importerSpec.getName(), apisource.getMetadata().getName());
            logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
            apisource.getStatus().setStatus(Status.ERROR);
            condition.setStatus(Status.ERROR);
         }

         ConditionUtil.touchConditionTime(condition);
         updateStatus = true;
      }

      logger.infof("Finishing reconcile operation for '%s'", apisource.getMetadata().getName());

      if (updateStatus) {
         logger.info("Returning an updateStatus control. ========================");
         checkIfGloballyReady(apisource);
         return UpdateControl.updateStatus(apisource);
      }

      logger.info("Returning a noUpdate control. =============================");
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(APISource apisource, Context<APISource> context) {
      final String ns = apisource.getMetadata().getNamespace();
      final APISourceSpec spec = apisource.getSpec();
      logger.infof("Starting cleanup operation for '%s'", apisource.getMetadata().getName());

      // Check that microcks instance specification is there.
      Map<String, String> annotations = apisource.getMetadata().getAnnotations();
      String microcksName = annotations.get(MicrocksOperatorConfig.INSTANCE_SELECTOR);
      if (microcksName == null) {
         logger.errorf("No Microcks instance specified for APISource '%s'", apisource.getMetadata().getName());
         return DeleteControl.defaultDelete();
      }

      MixedOperation<Microcks, KubernetesResourceList<Microcks>, Resource<Microcks>> microcksClient = client.resources(Microcks.class);

      // Check that microcks instance is found in current namespace.
      Microcks microcks = microcksClient.inNamespace(ns).withName(microcksName).get();
      if (microcks != null && microcks.getStatus().getStatus() == Status.READY && apisource.getStatus() != null) {

         // Retrieve an authentication token from associated Keycloak configuration.
         String oauthToken;
         try {
            // Load default values for CR and build a complete representation.
            Microcks defaultCR = loadDefaultMicrocksCR();

            MicrocksSpec completeSpec = merger.mergeResources(defaultCR.getSpec(), microcks.getSpec());
            Microcks completeCR = new Microcks();
            completeCR.setKind(microcks.getKind());
            completeCR.setMetadata(microcks.getMetadata());
            completeCR.setSpec(completeSpec);
            completeCR.setStatus(microcks.getStatus());

            oauthToken = keycloakHelper.getOAuthToken(apisource.getMetadata(), completeCR);
         } catch (Exception e) {
            logger.errorf("Error while getting OAuth token for Keycloak server");
            return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(10));
         }

         // Build a needed ApiCLient to interact with Microcks API.
         ApiClient apiClient = new ApiClient();
         apiClient.updateBaseUri("http://" + microcks.getMetadata().getName() + "." + microcks.getMetadata().getNamespace() + ".svc.cluster.local:8080/api");
         apiClient.setRequestInterceptor(request -> request.header("Authorization", "Bearer " + oauthToken));
         JobApi jobApi = new JobApi(apiClient);

         // Remove importJobs from Microcks instance.
         for (ImporterSpec importerSpec : spec.getImporters()) {
            Condition condition = ConditionUtil.getOrCreateCondition(apisource.getStatus(), importerSpec.getRepository().getUrl());
            String importerId = getImporterIdOrNull(condition);

            if (importerId != null) {
               try {
                  jobApi.deleteImportJob(importerId);
               } catch (ApiException e) {
                  logger.errorf("Error while deleting importer '%s' for APISource '%s'", importerSpec.getName(), apisource.getMetadata().getName());
                  logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
               }
            }
         }
      }
      return DeleteControl.defaultDelete();
   }

   protected String getImporterIdOrNull(Condition condition) {
      return condition.getMessage();
   }

   protected void ensureArtifactIsLoaded(ApiClient apiClient, ArtifactSpec artifactSpec) throws ApiException {
      // Use the apiCLient to download the artifact.
      JobApi jobApi = new JobApi(apiClient);
      String result = jobApi.downloadArtifact(artifactSpec.getUrl(), artifactSpec.getMainArtifact(), artifactSpec.getSecretRef());
   }

   protected String ensureImporterIsPresent(ApiClient apiClient, ImporterSpec importerSpec, String previousId) throws ApiException {
      if (previousId != null) {
         // We have a previous job id, we should check if it's still there.
         JobApi jobApi = new JobApi(apiClient);
         ImportJob job = jobApi.getImportJob(previousId);
         if (job != null) {
            updateWithImporterSpec(job, importerSpec);
            jobApi.updateImportJob(previousId, job);
            return previousId;
         }
      }
      return createImporterJob(apiClient, importerSpec);
   }

   protected String createImporterJob(ApiClient apiClient, ImporterSpec importerSpec) throws ApiException {
      // Move ImporterSpec into Microcks API model.
      ImportJob job = new ImportJob();
      updateWithImporterSpec(job, importerSpec);
      // Use the apiClient to create the job.
      JobApi jobApi = new JobApi(apiClient);
      job = jobApi.createImportJob(job);
      return job.getId();
   }

   protected void updateWithImporterSpec(ImportJob job, ImporterSpec importerSpec) {
      job.setName(importerSpec.getName());
      job.setRepositoryUrl(importerSpec.getRepository().getUrl());
      job.setRepositoryDisableSSLValidation(importerSpec.getRepository().getDisableSSLValidation());
      job.setMainArtifact(importerSpec.getMainArtifact());
      job.setActive(importerSpec.getActive());
      if (importerSpec.getRepository().getSecretRef() != null) {
         SecretRef secretRef = new SecretRef();
         secretRef.setName(importerSpec.getRepository().getSecretRef());
         job.setSecretRef(secretRef);
      }
      if (importerSpec.getLabels() != null) {
         Metadata metadata = new Metadata();
         metadata.setLabels(importerSpec.getLabels());
         job.setMetadata(metadata);
      }
   }

   /** Load from YAML resource. */
   private Microcks loadDefaultMicrocksCR() {
      return ReconcilerUtils.loadYaml(Microcks.class, getClass(), "/k8s/microcks-default.yml");
   }

   protected void checkIfGloballyReady(APISource apisource) {
      boolean allReady = true;
      for (Condition condition : apisource.getStatus().getConditions()) {
         if (condition.getStatus() != Status.READY) {
            allReady = false;
            break;
         }
      }
      if (allReady) {
         apisource.getStatus().setStatus(Status.READY);
      }
   }
}
