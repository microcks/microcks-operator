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
package io.github.microcks.operator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

import java.util.Map;

/**
 * Representation of a Kubernetes HTTPRoute/GRPCRoute/TLSRoute configuration of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class GatewayRouteSpec {

   @JsonPropertyDescription("Whether a Gateway Route should be exposed (ie. created)")
   private boolean expose = true;

   @JsonPropertyDescription("The parent Gateway reference name, sets the parentRefs.name property")
   private String gatewayRefName;

   @JsonPropertyDescription("The parent Gateway reference namespace, sets the parentRefs.namespace property")
   private String gatewayRefNamespace;

   @JsonPropertyDescription("The parent Gateway reference section name, sets the parentRefs.sectionName property")
   private String gatewayRefSectionName;

   @JsonPropertyDescription("Annotations to apply to Route if created")
   private Map<String, String> annotations;

   public GatewayRouteSpec() {
   }

   public GatewayRouteSpec(String gatewayRefName) {
      this.gatewayRefName = gatewayRefName;
   }

   public boolean isExpose() {
      return expose;
   }

   public void setExpose(boolean expose) {
      this.expose = expose;
   }

   public String getGatewayRefName() {
      return gatewayRefName;
   }

   public void setGatewayRefName(String gatewayRefName) {
      this.gatewayRefName = gatewayRefName;
   }

   public String getGatewayRefNamespace() {
      return gatewayRefNamespace;
   }

   public void setGatewayRefNamespace(String gatewayRefNamespace) {
      this.gatewayRefNamespace = gatewayRefNamespace;
   }

   public String getGatewayRefSectionName() {
      return gatewayRefSectionName;
   }

   public void setGatewayRefSectionName(String gatewayRefSectionName) {
      this.gatewayRefSectionName = gatewayRefSectionName;
   }

   public Map<String, String> getAnnotations() {
      return annotations;
   }

   public void setAnnotations(Map<String, String> annotations) {
      this.annotations = annotations;
   }
}
