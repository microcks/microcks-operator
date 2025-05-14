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
package io.github.microcks.operator.api.test.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

import java.util.List;
import java.util.Map;

/**
 * This the {@code specification} of a {@link Test} custom resource.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "serviceId", "testEndpoint", "runnerType", "timeout", "secretRef", "retentionPolicy" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class TestSpec {

   @JsonPropertyDescription("The API/Service identifier to test")
   private String serviceId;

   @JsonPropertyDescription("The implementation endpoint of API/Service to test")
   private String testEndpoint;

   @JsonPropertyDescription("The runner to use for this test")
   private String runnerType;

   @JsonPropertyDescription("The timeout to use for this test in milliseconds. Default is 5000.")
   private long timeout = 5000L;

   @JsonPropertyDescription("Reference to a Secret for accessing the test endpoint")
   private String secretRef;

   @JsonPropertyDescription("An optional list of filtered operations to test")
   private List<String> filteredOperations;

   @JsonPropertyDescription("An optional 2-levels map of headers to set for each operation")
   private Map<String, Map<String, String>> operationsHeaders;

   @JsonProperty("oAuth2Context")
   @JsonPropertyDescription("The OAuth2 context to use for this test")
   private OAuth2ClientContext oAuth2Context;

   @JsonPropertyDescription("The retention policy to use for this Test resource in Kubernetes. Default is Retain.")
   private RetentionPolicy retentionPolicy = RetentionPolicy.Retain;


   public String getServiceId() {
      return serviceId;
   }

   public void setServiceId(String serviceId) {
      this.serviceId = serviceId;
   }

   public String getTestEndpoint() {
      return testEndpoint;
   }

   public void setTestEndpoint(String testEndpoint) {
      this.testEndpoint = testEndpoint;
   }

   public String getRunnerType() {
      return runnerType;
   }

   public void setRunnerType(String runnerType) {
      this.runnerType = runnerType;
   }

   public long getTimeout() {
      return timeout;
   }

   public void setTimeout(long timeout) {
      this.timeout = timeout;
   }

   public String getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(String secretRef) {
      this.secretRef = secretRef;
   }

   public List<String> getFilteredOperations() {
      return filteredOperations;
   }

   public void setFilteredOperations(List<String> filteredOperations) {
      this.filteredOperations = filteredOperations;
   }

   public Map<String, Map<String, String>> getOperationsHeaders() {
      return operationsHeaders;
   }

   public void setOperationsHeaders(Map<String, Map<String, String>> operationsHeaders) {
      this.operationsHeaders = operationsHeaders;
   }

   public OAuth2ClientContext getOAuth2Context() {
      return oAuth2Context;
   }

   public void setOAuth2Context(OAuth2ClientContext oAuth2Context) {
      this.oAuth2Context = oAuth2Context;
   }

   public RetentionPolicy getRetentionPolicy() {
      return retentionPolicy;
   }

   public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
      this.retentionPolicy = retentionPolicy;
   }
}
