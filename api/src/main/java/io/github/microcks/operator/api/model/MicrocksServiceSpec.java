/*
 * Licensed to Laurent Broudoux (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.microcks.operator.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

import java.util.List;
import java.util.Map;

/**
 * Representation of the Microcks webapp part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class MicrocksServiceSpec {

   @JsonPropertyDescription("Number of desired pods for Microcks service")
   private int replicas;
   private String url;

   @JsonPropertyDescription("Annotations to apply to Ingress if created")
   private Map<String, String> ingressAnnotations;

   @JsonPropertyDescription("Kubernetes resource requirements for Microcks service")
   private ResourceRequirements resources;

   @JsonPropertyDescription("Environment variables for Microcks service")
   private List<EnvVar> env;

   @JsonPropertyDescription("Enable/disable statistics for mocks invocation. Defaults to true.")
   private boolean mockInvocationStats;

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

   public Map<String, String> getIngressAnnotations() {
      return ingressAnnotations;
   }

   public void setIngressAnnotations(Map<String, String> ingressAnnotations) {
      this.ingressAnnotations = ingressAnnotations;
   }

   public ResourceRequirements getResources() {
      return resources;
   }

   public void setResources(ResourceRequirements resources) {
      this.resources = resources;
   }

   public List<EnvVar> getEnv() {
      return env;
   }

   public void setEnv(List<EnvVar> env) {
      this.env = env;
   }

   public boolean isMockInvocationStats() {
      return mockInvocationStats;
   }

   public void setMockInvocationStats(boolean mockInvocationStats) {
      this.mockInvocationStats = mockInvocationStats;
   }
}
