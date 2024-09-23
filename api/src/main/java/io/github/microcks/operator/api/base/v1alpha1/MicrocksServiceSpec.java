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
package io.github.microcks.operator.api.base.v1alpha1;

import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.api.model.LogLevel;
import io.github.microcks.operator.api.model.OpenShiftSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

import java.util.List;
import java.util.Map;

/**
 * Representation of the Microcks webapp part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "url", "replicas", "ingress", "grpcIngress", "resources", "mockInvocationStats", "logLevel",
      "openshift", "env", "extraProperties" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class MicrocksServiceSpec {

   @JsonPropertyDescription("Number of desired pods for Microcks service")
   private int replicas = 1;

   @JsonPropertyDescription("The URL to use for exposing Microcks main ingress")
   private String url;

   @JsonPropertyDescription("Configuration to apply to Ingress if created")
   private IngressSpec ingress;

   @JsonPropertyDescription("Configuration to apply to Ingress for gRPC if creates")
   private IngressSpec grpcIngress;

   @JsonPropertyDescription("Kubernetes resource requirements for Microcks service")
   private ResourceRequirements resources;

   @JsonPropertyDescription("Enable/disable statistics for mocks invocation. Defaults to true")
   private Boolean mockInvocationStats;

   @JsonPropertyDescription("Allow configuration of logging level. Defaults to INFO")
   private LogLevel logLevel;

   @JsonPropertyDescription("Configuration of OpenShift specific settings")
   private OpenShiftSpec openshift;

   @JsonPropertyDescription("Environment variables for Microcks service")
   private List<EnvVar> env;

   @JsonPropertyDescription("Extra properties to integration into application-extra configuration")
   private Map<String, AnyType> extraProperties;

   public int getReplicas() {
      return replicas;
   }

   public void setReplicas(int replicas) {
      this.replicas = replicas;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public IngressSpec getIngress() {
      return ingress;
   }

   public void setIngress(IngressSpec ingress) {
      this.ingress = ingress;
   }

   public IngressSpec getGrpcIngress() {
      return grpcIngress;
   }

   public void setGrpcIngress(IngressSpec grpcIngress) {
      this.grpcIngress = grpcIngress;
   }

   public ResourceRequirements getResources() {
      return resources;
   }

   public void setResources(ResourceRequirements resources) {
      this.resources = resources;
   }

   public Boolean getMockInvocationStats() {
      return mockInvocationStats;
   }

   public void setMockInvocationStats(Boolean mockInvocationStats) {
      this.mockInvocationStats = mockInvocationStats;
   }

   public LogLevel getLogLevel() {
      return logLevel;
   }

   public void setLogLevel(LogLevel logLevel) {
      this.logLevel = logLevel;
   }

   public OpenShiftSpec getOpenshift() {
      return openshift;
   }

   public void setOpenshift(OpenShiftSpec openshift) {
      this.openshift = openshift;
   }

   public List<EnvVar> getEnv() {
      return env;
   }

   public void setEnv(List<EnvVar> env) {
      this.env = env;
   }

   public Map<String, AnyType> getExtraProperties() {
      return extraProperties;
   }

   public void setExtraProperties(Map<String, AnyType> extraProperties) {
      this.extraProperties = extraProperties;
   }
}
