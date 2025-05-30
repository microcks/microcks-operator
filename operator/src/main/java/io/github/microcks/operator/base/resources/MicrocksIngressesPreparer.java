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

import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.model.GatewayRouteSpec;
import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.OpenShiftRouteSpec;
import io.github.microcks.operator.model.GatewayRouteSpecUtil;
import io.github.microcks.operator.model.IngressSpecUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * A Microcks Kubernetes Ingress or OpenShift Route resources preparer.
 * @author laurent
 */
public class MicrocksIngressesPreparer {

   /** Get a JBoss logging logger. */
   private static final Logger logger = Logger.getLogger(MicrocksIngressesPreparer.class);

   private static final String RESOURCE_SUFFIX = "-ingress";

   /**
    * Get the Route resource name given the primary microcks.
    * @param microcks The primary Microcks resource
    * @return The name of OpenShift Route
    */
   public static final String getRouteName(Microcks microcks) {
      return microcks.getMetadata().getName();
   }

   /**
    * Get the Secret name where ingress TLS props are stored given the primary microcks.
    * @param microcks The primary Microcks resource
    * @return The name of Kubernetes secret for Ingress
    */
   public static final String getIngressSecretName(Microcks microcks) {
      return getRouteName(microcks) + RESOURCE_SUFFIX;
   }

   /**
    * Prepare a Route resource giving the primary microcks.
    * @param microcks The primary Microcks resource
    * @param context  The reconciliation context
    * @return An OpenShift Route resource
    */
   public static Route prepareRoute(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Preparing desired Microcks Route for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      RouteBuilder builder = new RouteBuilder()
            .withNewMetadata().
               withName(getRouteName(microcks))
               .addToLabels("app", microcksName)
               .addToLabels("group", "microcks")
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .withNewSpec()
               .withNewTo()
                  .withKind("Service")
                  .withName(MicrocksServiceDependentResource.getServiceName(microcks))
               .endTo()
               .withNewPort()
                  .withNewTargetPort("spring")
               .endPort()
               .withNewTls()
                  .withTermination(microcks.getSpec().getMicrocks().getOpenshift().getRoute().getTlsTermination())
               .endTls()
            .endSpec();

      // Add custom url is present in the spec.
      if (microcks.getSpec().getMicrocks().getUrl() != null) {
         builder.editSpec().withHost(microcks.getSpec().getMicrocks().getUrl()).endSpec();
      }

      // Add optional TLS configuration if specified in route spec.
      final OpenShiftRouteSpec routeSpec = microcks.getSpec().getMicrocks().getOpenshift().getRoute();
      if (routeSpec.getTlsKey() != null) {
         builder.editSpec().editTls().withKey(routeSpec.getTlsKey()).endTls().endSpec();
      }
      if (routeSpec.getTlsCertificate() != null) {
         builder.editSpec().editTls().withCertificate(routeSpec.getTlsCertificate()).endTls().endSpec();
      }
      if (routeSpec.getTlsCertificate() != null) {
         builder.editSpec().editTls().withCertificate(routeSpec.getTlsCertificate()).endTls().endSpec();
      }
      if (routeSpec.getTlsDestinationCaCertificate() != null) {
         builder.editSpec().editTls().withDestinationCACertificate(routeSpec.getTlsDestinationCaCertificate()).endTls()
               .endSpec();
      }
      if (routeSpec.getTlsInsecureEdgeTerminationPolicy() != null) {
         builder.editSpec().editTls().withInsecureEdgeTerminationPolicy(routeSpec.getTlsInsecureEdgeTerminationPolicy())
               .endTls().endSpec();
      }

      return builder.build();
   }

   /**
    * Prepare an Ingress resource giving the primary microcks.
    * @param microcks The primary Microcks resource
    * @param context  The reconciliation context
    * @return A vanilla Kubernetes Ingress resource
    */
   public static Ingress prepareIngress(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Preparing desired Microcks Ingress for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final IngressSpec spec = microcks.getSpec().getMicrocks().getIngress();

      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getRouteName(microcks))
               .addToLabels("app", microcksName)
               .addToLabels("group", "microcks")
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .withNewSpec()
               .addNewTl()
                  .addToHosts(microcks.getSpec().getMicrocks().getUrl())
                  .withSecretName(IngressSpecUtil.getSecretName(spec, getIngressSecretName(microcks)))
               .endTl()
               .addNewRule()
                  .withHost(microcks.getSpec().getMicrocks().getUrl())
                  .withNewHttp()
                     .addNewPath()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                           .withNewService()
                              .withName(MicrocksServiceDependentResource.getServiceName(microcks))
                              .withNewPort()
                                 .withNumber(8080)
                              .endPort()
                           .endService()
                        .endBackend()
                     .endPath()
                  .endHttp()
               .endRule()
            .endSpec();

      // Add ingress classname if specified.
      if (spec.getClassName() != null) {
         builder.editSpec().withIngressClassName(spec.getClassName()).endSpec();
      }

      // Add complementary annotations if any.
      Map<String, String> annotations = IngressSpecUtil.getAnnotationsIfAny(spec);
      if (annotations != null) {
         builder.editMetadata().addToAnnotations(annotations).endMetadata();
      }

      return builder.build();
   }

   /**
    * Prepare a HTTPRoute resource giving the primary microcks.
    * @param microcks The primary Microcks resource
    * @param context  The reconciliation context
    * @return A vanilla Kubernetes HTTPRoute resource
    */
   public static HTTPRoute prepareHTTPRoute(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Preparing desired Microcks HTTPRoute for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();
      final GatewayRouteSpec gatewayRouteSpec = spec.getMicrocks().getGatewayRoute();

      HTTPRouteBuilder builder = new HTTPRouteBuilder()
            .withNewMetadata()
               .withName(getRouteName(microcks))
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
                     .withName(MicrocksServiceDependentResource.getServiceName(microcks))
                     .withPort(8080)
                  .endBackendRef()
               .endRule()
            .endSpec();

      // Add gateway namespace if specified.
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
