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
package io.github.microcks.operator.artifact;

import io.github.microcks.client.ApiClient;
import io.github.microcks.client.ApiException;
import io.github.microcks.client.api.DefaultApi;
import io.github.microcks.client.api.JobApi;
import io.github.microcks.client.api.MockApi;
import io.github.microcks.client.model.ImportJob;
import io.github.microcks.client.model.Metadata;
import io.github.microcks.client.model.SecretRef;
import io.github.microcks.operator.AbstractMicrocksDependantReconciler;
import io.github.microcks.operator.KeycloakHelper;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.Condition;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.api.artifact.v1alpha1.APISource;
import io.github.microcks.operator.api.artifact.v1alpha1.APISourceSpec;
import io.github.microcks.operator.api.artifact.v1alpha1.APISourceStatus;
import io.github.microcks.operator.api.artifact.v1alpha1.ArtifactSpec;
import io.github.microcks.operator.api.artifact.v1alpha1.ImporterSpec;
import io.github.microcks.operator.model.ConditionUtil;
import io.github.microcks.operator.model.ResourceMerger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

/**
 * Reconciliation entry point for the {@code APISource} Kubernetes custom resource.
 * @author laurent
 */
@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE)
@SuppressWarnings("unused")
@ApplicationScoped
public class APISourceReconciler extends AbstractMicrocksDependantReconciler<APISource, APISourceSpec, APISourceStatus>
      implements Reconciler<APISource>, Cleaner<APISource> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private final ResourceMerger merger = new ResourceMerger();

   private final ObjectMapper mapper;

   /**
    * Default constructor with injected Kubernetes client.
    * @param client A Kubernetes client for interacting with the cluster
    */
   public APISourceReconciler(KubernetesClient client) {
      this.client = client;
      this.mapper = new ObjectMapper();
      this.keycloakHelper = new KeycloakHelper(client);
   }

   @Override
   public UpdateControl<APISource> reconcile(APISource apiSource, Context<APISource> context) throws Exception {
      final String ns = apiSource.getMetadata().getNamespace();
      final APISourceSpec spec = apiSource.getSpec();

      boolean updateStatus = false;
      // Set a minimal status if not present.
      if (apiSource.getStatus() == null) {
         apiSource.setStatus(new APISourceStatus());
         updateStatus = true;
      }

      logger.infof("Starting reconcile operation for '%s'", apiSource.getMetadata().getName());

      // Check that microcks instance specification is there.
      UpdateControlOrMicrocks<APISource> preparationControl = prepareReconciliationWithMicrocksInstance(apiSource);
      if (preparationControl.updateControl() != null) {
         return preparationControl.updateControl();
      }

      // Now we have a Microcks instance that is ready to receive API requests.
      Microcks microcks = preparationControl.microcks();

      // Build an ApiClient for Microcks instance.
      UpdateControlOrApiClient<APISource> apiClientControl = buildApiClient(apiSource, microcks);
      if (apiClientControl.updateControl() != null) {
         return apiClientControl.updateControl();
      }

      // Now we have an authenticated & ready to use ApiClient for Microcks instance.
      ApiClient apiClient = apiClientControl.apiClient();

      // Deal with artifact specifications.
      for (ArtifactSpec artifactSpec : spec.getArtifacts()) {
         Condition condition = ConditionUtil.getOrCreateCondition(apiSource.getStatus(), artifactSpec.getUrl());

         try {
            String serviceId = ensureArtifactIsLoaded(apiClient, artifactSpec);
            condition.setStatus(Status.READY);
            // TODO: Store API | Service identifier in condition additional property instead.
            condition.setMessage(serviceId);
         } catch (ApiException e) {
            logger.errorf("Error while loading artifact '%s' for APISource '%s'", artifactSpec.getUrl(), apiSource.getMetadata().getName());
            logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
            apiSource.getStatus().setStatus(Status.ERROR);
            condition.setStatus(Status.ERROR);
         }

         ConditionUtil.touchConditionTime(condition);
         updateStatus = true;
      }

      // Deal with importer specifications.
      for (ImporterSpec importerSpec : spec.getImporters()) {
         Condition condition = ConditionUtil.getOrCreateCondition(apiSource.getStatus(), importerSpec.getRepository().getUrl());

         try {
            // Previously created job id may be stored within condition message.
            String previousId = getImporterIdOrNull(condition);
            String importerId = ensureImporterIsPresent(apiClient, importerSpec, previousId);
            condition.setStatus(Status.READY);
            // TODO: Store importerId in condition additional property instead.
            condition.setMessage(importerId);
         } catch (ApiException e) {
            logger.errorf("Error while creating importer '%s' for APISource '%s'", importerSpec.getName(), apiSource.getMetadata().getName());
            logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
            apiSource.getStatus().setStatus(Status.ERROR);
            condition.setStatus(Status.ERROR);
         }

         ConditionUtil.touchConditionTime(condition);
         updateStatus = true;
      }

      logger.infof("Finishing reconcile operation for '%s'", apiSource.getMetadata().getName());

      if (updateStatus) {
         logger.info("Returning an updateStatus control. ========================");
         checkIfGloballyReady(apiSource);
         return UpdateControl.updateStatus(apiSource);
      }

      logger.info("Returning a noUpdate control. =============================");
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(APISource apiSource, Context<APISource> context) {
      final String ns = apiSource.getMetadata().getNamespace();
      final APISourceSpec spec = apiSource.getSpec();
      logger.infof("Starting cleanup operation for '%s'", apiSource.getMetadata().getName());

      // Check that microcks instance specification is there.
      DeleteControlOrMicrocks preparationControl = prepareCleanupWithMicrocksInstance(apiSource);
      if (preparationControl.deleteControl() != null) {
         return preparationControl.deleteControl();
      }

      // Now we have a Microcks instance that is at least present.
      Microcks microcks = preparationControl.microcks();

      // Check that microcks instance is in ready status and we have a status for APISource.
      if (microcks.getStatus().getStatus() == Status.READY && apiSource.getStatus() != null) {

         // Build an ApiClient for Microcks instance.
         UpdateControlOrApiClient<APISource> apiClientControl = buildApiClient(apiSource, microcks);
         if (apiClientControl.updateControl() != null) {
            logger.error("Rescheduling cleanup operation in 30 seconds");
            return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(30));
         }

         // Now we have an authenticated & ready to use ApiClient for Microcks instance.
         ApiClient apiClient = apiClientControl.apiClient();
         JobApi jobApi = new JobApi(apiClient);

         // Remove artifacts related API | Service from Microcks instance unless keepAPIOnDelete is set.
         if (!spec.isKeepAPIOnDelete()) {
            MockApi mockApi = new MockApi(apiClient);
            for (ArtifactSpec artifactSpec : spec.getArtifacts()) {
               Condition condition = ConditionUtil.getOrCreateCondition(apiSource.getStatus(), artifactSpec.getUrl());
               String serviceId = condition.getMessage();

               if (serviceId != null) {
                  try {
                     mockApi.deleteService(serviceId);
                  } catch (ApiException e) {
                     logger.errorf("Error while deleting service '%s' for APISource '%s' artifact '%s'", serviceId,
                           apiSource.getMetadata().getName(), artifactSpec.getUrl());
                     logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
                     return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(30));
                  }
               }
            }
         }

         // Remove importJobs from Microcks instance.
         for (ImporterSpec importerSpec : spec.getImporters()) {
            Condition condition = ConditionUtil.getOrCreateCondition(apiSource.getStatus(), importerSpec.getRepository().getUrl());
            String importerId = getImporterIdOrNull(condition);

            if (importerId != null) {
               try {
                  jobApi.deleteImportJob(importerId);
               } catch (ApiException e) {
                  logger.errorf("Error while deleting importer '%s' for APISource '%s'", importerSpec.getName(),
                        apiSource.getMetadata().getName());
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

   /** Get an importer id (or null if not exists) */
   protected String getImporterIdOrNull(Condition condition) {
      return condition.getMessage();
   }

   /** Ensure an artifact is loaded by reloading in Microcks instance. */
   protected String ensureArtifactIsLoaded(ApiClient apiClient, ArtifactSpec artifactSpec) throws ApiException {
      // Use the apiClient to download the artifact.
      JobApi jobApi = new JobApi(apiClient);
      String result =  jobApi.downloadArtifact(artifactSpec.getUrl(), artifactSpec.getMainArtifact(), artifactSpec.getSecretRef());
      try {
         JsonNode resultNode = mapper.readTree(result);
         if (resultNode.has("name")) {
            return resultNode.get("name").asText();
         }
      } catch (JsonProcessingException e) {
         logger.errorf("Error while parsing artifact download response: %s", result);
         throw new ApiException("Error while parsing artifact download JSON response");
      }
      throw new ApiException("No 'name' property found in response");
   }

   /** Ensure an importer (ImportJob) exists by checking by id, updating if found or cre-creating if not found. */
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

   protected void checkIfGloballyReady(APISource apiSource) {
      boolean allReady = true;
      for (Condition condition : apiSource.getStatus().getConditions()) {
         if (condition.getStatus() != Status.READY) {
            allReady = false;
            break;
         }
      }
      if (allReady) {
         apiSource.getStatus().setStatus(Status.READY);
      }
   }
}
