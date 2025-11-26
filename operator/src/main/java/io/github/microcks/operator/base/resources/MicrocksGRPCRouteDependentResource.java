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
package io.github.microcks.operator.base.resources;

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.model.GatewayRouteSpec;
import io.github.microcks.operator.model.GatewayRouteSpecUtil;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.GRPCRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.GRPCRouteBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * A Microcks Kubernetes GRPCRoute dependent resource for gRPC traffic.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class MicrocksGRPCRouteDependentResource extends CRUDKubernetesDependentResource<GRPCRoute, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-grpc";

   /** Default empty constructor. */
   public MicrocksGRPCRouteDependentResource() {
      super(GRPCRoute.class);
   }

   /**
    * Get the GRPC host for the ingress.
    * @param primary The primary Microcks resource
    * @return The host for GRPC traffic
    */
   public static String getGRPCHost(Microcks primary) {
      String microcksUrl = primary.getStatus().getMicrocksUrl();
      String hostname = microcksUrl.substring(0, microcksUrl.indexOf('.'));
      String domain = microcksUrl.substring(microcksUrl.indexOf('.'));
      return hostname + RESOURCE_SUFFIX + domain;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return primary.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   protected GRPCRoute desired(Microcks microcks, Context<Microcks> context) {
      logger.infof("Building desired Microcks GRPCRoute for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();
      final GatewayRouteSpec gatewayRouteSpec = spec.getMicrocks().getGrpcGatewayRoute();

      GRPCRouteBuilder builder = new GRPCRouteBuilder()
            .withNewMetadata()
               .withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("group", "microcks")
               .addToLabels(spec.getCommonLabels())
               .addToAnnotations(spec.getCommonAnnotations())
            .endMetadata()
            .withNewSpec()
               .addNewParentRef()
                  .withName(GatewayRouteSpecUtil.getGatewayName(spec, gatewayRouteSpec))
                  .withSectionName(GatewayRouteSpecUtil.getGatewaySectionName(spec, gatewayRouteSpec))
               .endParentRef()
               .addToHostnames(getGRPCHost(microcks))
               .addNewRule()
                  .addNewBackendRef()
                     .withKind("Service")
                     .withName(MicrocksGRPCServiceDependentResource.getServiceName(microcks))
                     .withPort(9090)
                  .endBackendRef()
               .endRule()
            .endSpec();

      if (GatewayRouteSpecUtil.getGatewayNamespace(spec, gatewayRouteSpec) != null) {
         builder.editSpec()
               .editFirstParentRef()
                  .withNamespace(GatewayRouteSpecUtil.getGatewayNamespace(spec, gatewayRouteSpec))
               .endParentRef().endSpec();
      }

      // Add complementary annotations if any.
      Map<String, String> annotations = GatewayRouteSpecUtil.getAnnotationsIfAny(gatewayRouteSpec);
      if (annotations != null) {
         builder.editMetadata().addToAnnotations(annotations).endMetadata();
      }

      return builder.build();
   }
}
