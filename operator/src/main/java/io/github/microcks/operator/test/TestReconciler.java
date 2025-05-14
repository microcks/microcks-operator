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
package io.github.microcks.operator.test;

import io.github.microcks.client.ApiClient;
import io.github.microcks.client.ApiException;
import io.github.microcks.client.api.TestApi;
import io.github.microcks.client.model.HeaderDTO;
import io.github.microcks.client.model.OAuth2ClientContext;
import io.github.microcks.client.model.TestRequest;
import io.github.microcks.client.model.TestResult;
import io.github.microcks.client.model.TestRunnerType;
import io.github.microcks.operator.AbstractMicrocksDependantReconciler;
import io.github.microcks.operator.KeycloakHelper;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.api.test.v1alpha1.Result;
import io.github.microcks.operator.api.test.v1alpha1.RetentionPolicy;
import io.github.microcks.operator.api.test.v1alpha1.Test;
import io.github.microcks.operator.api.test.v1alpha1.TestSpec;
import io.github.microcks.operator.api.test.v1alpha1.TestStatus;
import io.github.microcks.operator.model.ResourceMerger;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

/**
 * Reconciliation entry point for the {@code Test} Kubernetes custom resource.
 * @author laurent
 */
@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE)
@SuppressWarnings("unused")
@ApplicationScoped
public class TestReconciler extends AbstractMicrocksDependantReconciler<Test, TestSpec, TestStatus>
      implements Reconciler<Test>, Cleaner<Test> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private final ResourceMerger merger = new ResourceMerger();

   /**
    * Default constructor with injected Kubernetes client.
    * @param client A Kubernetes client for interacting with the cluster
    */
   public TestReconciler(KubernetesClient client) {
      this.client = client;
      this.keycloakHelper = new KeycloakHelper(client);
   }

   @Override
   public UpdateControl<Test> reconcile(Test test, Context<Test> context) throws Exception {

      // Check that microcks instance specification is there.
      UpdateControlOrMicrocks<Test> preparationControl = prepareReconciliationWithMicrocksInstance(test);
      if (preparationControl.updateControl() != null) {
         return preparationControl.updateControl();
      }

      // Now we have a Microcks instance that is ready to receive API requests.
      Microcks microcks = preparationControl.microcks();

      // Build an ApiClient for Microcks instance.
      UpdateControlOrApiClient<Test> apiClientControl = buildApiClient(test, microcks);
      if (apiClientControl.updateControl() != null) {
         return apiClientControl.updateControl();
      }

      // Now we have an authenticated & ready to use ApiClient for Microcks instance.
      ApiClient apiClient = apiClientControl.apiClient();
      TestApi testApi = new TestApi(apiClient);

      TestSpec testSpec = test.getSpec();
      TestStatus testStatus = test.getStatus();

      try {
         if (testStatus == null) {
            // First reconciliation for this test. We need to launch it.
            // Build a new status and tracking test identifiers and progress.
            testStatus = new TestStatus();
            test.setStatus(testStatus);

            TestResult testResult = launchTest(testApi, testSpec);

            // Ttracking test identifiers and progress in new status.
            testStatus.setId(testResult.getId());
            testStatus.setUrl("https://" + microcks.getStatus().getMicrocksUrl() + "/#/tests/" + testResult.getId());
            testStatus.setStatus(Status.DEPLOYING);
            testStatus.setResult(Result.IN_PROGRESS);

            // Schedule a new reconciliation in 4 sec.
            return UpdateControl.updateStatus(test).rescheduleAfter(Duration.ofSeconds(4));

         } else if (testStatus.getStatus() == Status.DEPLOYING) {
            // Reconciliation is in progress as well as the test.
            // We need to check if the test has completed.
            TestResult testResult = refreshTestResult(testApi, testSpec, testStatus.getId());

            if (!testResult.getInProgress()) {
               logger.infof("Test '%s' has completed. Success? %s", testStatus.getId(), testResult.getSuccess());
               if (testResult.getSuccess()) {
                  testStatus.setResult(Result.SUCCESS);
               } else {
                  testStatus.setResult(Result.FAILURE);
               }
               testStatus.setStatus(Status.READY);
            } else {
               logger.infof("Test '%s' is still in progress, scheduling a new check in 2 sec", testStatus.getId());
               return UpdateControl.updateStatus(test).rescheduleAfter(Duration.ofSeconds(2));
            }

            // Tets has completed. We must update the status, and we may re-schedule a deletion based on retention policy.
            if (testSpec.getRetentionPolicy() != RetentionPolicy.Retain) {
               // Reschedule must not be too fast to allow event-driven workflows (like Argo Events) to be triggered.
               return UpdateControl.updateStatus(test).rescheduleAfter(Duration.ofSeconds(10));
            }
            return UpdateControl.updateStatus(test);

         } else if (testStatus.getStatus() == Status.READY){
            // We have finished the reconciliation and the test result is known.
            // Delete the CR depending on retention policy.
            if (testSpec.getRetentionPolicy() == RetentionPolicy.Delete ||
                  (testSpec.getRetentionPolicy() == RetentionPolicy.DeleteOnSuccess && testStatus.getResult() == Result.SUCCESS)) {
               logger.infof("Marking Test CR '%s' for deletion", test.getMetadata().getName());
               client.resource(test).inNamespace(test.getMetadata().getNamespace()).delete();
            }
         }
      } catch (ApiException e) {
         logger.errorf("Error while calling the test API on Microcks instance '%s'", microcks.getMetadata().getName());
         logger.errorf(API_EXCEPTION_ERROR_LOG, e.getMessage(), e.getResponseBody());
         test.getStatus().setStatus(Status.ERROR);
         return UpdateControl.updateStatus(test).rescheduleAfter(Duration.ofSeconds(10));
      }

      logger.info("Returning a noUpdate control. =============================");
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(Test test, Context<Test> context) {
      logger.infof("Starting cleanup operation for '%s'", test.getMetadata().getName());

      // If here then every test has been removed!
      return DeleteControl.defaultDelete();
   }

   /** Launch a new test and get first results from Microcks instance. */
   protected TestResult launchTest(TestApi testApi, TestSpec testSpec) throws ApiException {
      logger.infof("Launching test for service '%s' on endpoint '%s'",
            testSpec.getServiceId(), testSpec.getTestEndpoint());

      TestRequest testRequest = new TestRequest();
      testRequest.setServiceId(testSpec.getServiceId());
      testRequest.setTestEndpoint(testSpec.getTestEndpoint());
      try {
         testRequest.setRunnerType(TestRunnerType.fromValue(testSpec.getRunnerType()));
      } catch (IllegalArgumentException iae) {
         logger.warnf("Test runner type '%s' is not supported. Defaulting to 'OPEN_API_SCHEMA'",
               testSpec.getRunnerType());
         testRequest.setRunnerType(TestRunnerType.OPEN_API_SCHEMA);
      }
      testRequest.setTimeout(testSpec.getTimeout());

      // Now deal with optional parameters.
      if (testSpec.getSecretRef() != null) {
         testRequest.setSecretName(testSpec.getSecretRef());
      }
      if (testSpec.getFilteredOperations() != null) {
         testRequest.setFilteredOperations(testSpec.getFilteredOperations());
      }
      if (testSpec.getOperationsHeaders() != null) {
         Map<String, List<HeaderDTO>> headers = new HashMap<>();
         testSpec.getOperationsHeaders().entrySet().forEach(entry -> {
            String operation = entry.getKey();
            Map<String, String> headersMap = entry.getValue();
            List<HeaderDTO> headerDTOs = headersMap.entrySet().stream()
                  .map(header -> new HeaderDTO()
                        .name(header.getKey())
                        .values(header.getValue()))
                  .toList();
            headers.put(operation, headerDTOs);
         });
         testRequest.setOperationsHeaders(headers);
      }
      if (testSpec.getOAuth2Context() != null) {
         // Build the OAuth2 context for API client.
         OAuth2ClientContext oAuth2Context = new OAuth2ClientContext();
         oAuth2Context.setClientId(testSpec.getOAuth2Context().getClientId());
         oAuth2Context.setClientSecret(testSpec.getOAuth2Context().getClientSecret());
         oAuth2Context.setTokenUri(testSpec.getOAuth2Context().getTokenUri());
         try {
            oAuth2Context.setGrantType(OAuth2ClientContext.GrantTypeEnum.fromValue(testSpec.getOAuth2Context().getGrantType()));
         } catch (IllegalArgumentException iae) {
            logger.warnf("Test OAuth2 grant type '%s' is not supported. Defaulting to 'CLIENT_CREDENTIALS'",
                  testSpec.getOAuth2Context().getGrantType());
            oAuth2Context.setGrantType(OAuth2ClientContext.GrantTypeEnum.CLIENT_CREDENTIALS);
         }
         // Following fields can be set to null.
         oAuth2Context.setRefreshToken(testSpec.getOAuth2Context().getRefreshToken());
         oAuth2Context.setUsername(testSpec.getOAuth2Context().getUsername());
         oAuth2Context.setPassword(testSpec.getOAuth2Context().getPassword());

         testRequest.setoAuth2Context(oAuth2Context);
      }

      return testApi.createTest(testRequest);
   }

   /** Refresh the test result from Microcks instance. */
   protected TestResult refreshTestResult(TestApi testApi, TestSpec testSpec, String testId) throws ApiException {
      logger.infof("Refreshing test result for service '%s' on endpoint '%s'",
            testSpec.getServiceId(), testSpec.getTestEndpoint());

      return testApi.getTestResult(testId);
   }
}
