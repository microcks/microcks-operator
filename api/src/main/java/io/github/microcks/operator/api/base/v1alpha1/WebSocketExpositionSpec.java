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

import io.github.microcks.operator.api.model.GatewayRouteSpec;
import io.github.microcks.operator.api.model.IngressSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the WebSocket exposition part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "async", "repositoryFilter", "repositoryTenancy", "microcksHub" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class WebSocketExpositionSpec {

   @JsonPropertyDescription("Configuration to apply to Ingress if created")
   private IngressSpec ingress;

   @JsonPropertyDescription("Configuration to apply to HTTPRoute if created")
   private GatewayRouteSpec gatewayRoute;

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
