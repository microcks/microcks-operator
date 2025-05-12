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
package io.github.microcks.operator.model;

import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.model.GatewayRouteSpec;

import java.util.Map;

/**
 * Holds utility methods to handle HTTPRoute from GatewayRouteSpec.
 * @author laurent
 */
public class GatewayRouteSpecUtil {

   /**
    * Get the route annotations if defined, null otherwise.
    * @param spec The GatewayRouteSpec that may be null
    * @return Route annotations to apply
    */
   public static Map<String, String> getAnnotationsIfAny(GatewayRouteSpec spec) {
      if (spec != null && spec.getAnnotations() != null) {
         return spec.getAnnotations();
      }
      return null;
   }

   /**
    * Get the gateway name to use for an HTTPRoute if specified or default to the common one.
    * @param microcks The Microcks resource
    * @param gatewayRouteSpec The GatewayRouteSpec that may be null
    * @return The gateway name to use
    */
   public static String getGatewayName(MicrocksSpec microcks, GatewayRouteSpec gatewayRouteSpec) {
      if (gatewayRouteSpec != null && gatewayRouteSpec.getGatewayRefName() != null) {
         return gatewayRouteSpec.getGatewayRefName();
      }
      return microcks.getCommonExpositions().getGatewayRoute().getGatewayRefName();
   }

   /**
    * Get the gateway namespace to use for an HTTPRoute if specified or default to the common one.
    * @param microcks The Microcks resource
    * @param gatewayRouteSpec The GatewayRouteSpec that may be null
    * @return The gateway namespace to use
    */
   public static String getGatewayNamespace(MicrocksSpec microcks, GatewayRouteSpec gatewayRouteSpec) {
      if (gatewayRouteSpec != null &&gatewayRouteSpec.getGatewayRefNamespace() != null) {
         return gatewayRouteSpec.getGatewayRefNamespace();
      }
      return microcks.getCommonExpositions().getGatewayRoute().getGatewayRefNamespace();
   }

   /**
    * Get the gateway section name to use for an HTTPRoute if specified or default to the common one.
    * @param microcks The Microcks resource
    * @param gatewayRouteSpec The GatewayRouteSpec that may be null
    * @return The gateway section name to use
    */
   public static String getGatewaySectionName(MicrocksSpec microcks, GatewayRouteSpec gatewayRouteSpec) {
      if (gatewayRouteSpec != null && gatewayRouteSpec.getGatewayRefSectionName() != null) {
         return gatewayRouteSpec.getGatewayRefSectionName();
      }
      return microcks.getCommonExpositions().getGatewayRoute().getGatewayRefSectionName();
   }
}
