package io.github.microcks.operator.base.resources;

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.model.GatewayRouteSpec;
import io.github.microcks.operator.model.GatewayRouteSpecUtil;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * An Async Minion Kubernetes HTTPRoute dependent resource for WebSocket traffic.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class AsyncMinionHTTPRouteDependentResource extends CRUDKubernetesDependentResource<HTTPRoute, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-ws";

   /** Default empty constructor. */
   public AsyncMinionHTTPRouteDependentResource() {
      super(HTTPRoute.class);
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return primary.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   protected HTTPRoute desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Microcks WebSocket HTTPRoute for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();
      final GatewayRouteSpec gatewayRouteSpec = spec.getFeatures().getAsync().getWs().getGatewayRoute();

      HTTPRouteBuilder builder = new HTTPRouteBuilder()
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
               .addToHostnames(spec.getMicrocks().getUrl())
               .addNewRule()
                  .addNewMatch()
                     .withNewPath()
                        .withValue("/")
                        .withType("PathPrefix")
                     .endPath()
                  .endMatch()
                  .addNewBackendRef()
                     .withKind("Service")
                     .withName(AsyncMinionServiceDependentResource.getServiceName(microcks))
                     .withPort(8080)
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
