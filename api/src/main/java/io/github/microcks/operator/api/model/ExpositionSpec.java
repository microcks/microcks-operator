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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of a Kubernetes exposition configuration of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "ingress", "gatewayRoute" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class ExpositionSpec {

   @JsonPropertyDescription("Type of exposition for Microcks services. Default is 'INGRESS'.")
   private ExpositionType type;

   @JsonPropertyDescription("Common ingress specification (such as class name, etc.)")
   private IngressSpec ingress;

   @JsonPropertyDescription("Common Gateway route specification (such as reference name, reference namespace etc.). Default reference name is 'default'.")
   private GatewayRouteSpec gatewayRoute;

   public ExpositionSpec() {
   }

   public ExpositionType getType() {
      return type;
   }

   public void setType(ExpositionType type) {
      this.type = type;
   }

   public IngressSpec getIngress() {
      return ingress;
   }

   public void setIngress(IngressSpec ingress) {
      this.ingress = ingress;
   }

   public GatewayRouteSpec getGatewayRoute() {
      return gatewayRoute;
   }

   public void setGatewayRoute(GatewayRouteSpec gatewayRoute) {
      this.gatewayRoute = gatewayRoute;
   }
}
