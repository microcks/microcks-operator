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
package io.github.microcks.operator.base.resources;

import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.OpenShiftRouteSpec;
import io.github.microcks.operator.model.IngressSpecUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * A Keycloak Kubernetes Ingress or OpenShift Route resources preparer.
 * @author laurent
 */
public class KeycloakIngressesPreparer {

   /** Get a JBoss logging logger. */
   private static final Logger logger = Logger.getLogger(KeycloakIngressesPreparer.class);

   private static final String RESOURCE_SUFFIX = "-ingress";

   public static final String getRouteName(Microcks microcks) {
      return KeycloakServiceDependentResource.getServiceName(microcks);
   }

   public static final String getIngressSecretName(Microcks microcks) {
      return getRouteName(microcks) + RESOURCE_SUFFIX;
   }

   /**
    *
    * @param microcks
    * @param context
    * @return
    */
   public static final Route prepareRoute(Microcks microcks, Context<Microcks> context) {
      logger.infof("Preparing desired Keycloak Route for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      RouteBuilder builder = new RouteBuilder()
         .withNewMetadata()
            .withName(getRouteName(microcks))
            .addToLabels("app", microcksName)
            .addToLabels("group", "microcks")
         .endMetadata()
         .withNewSpec()
            .withNewTo()
               .withKind("Service")
               .withName(KeycloakServiceDependentResource.getServiceName(microcks))
            .endTo()
            .withNewPort()
               .withNewTargetPort("keycloak")
            .endPort()
            .withNewTls()
               .withTermination(microcks.getSpec().getKeycloak().getOpenshift().getRoute().getTlsTermination())
            .endTls()
         .endSpec();

      // Add custom url is present in the spec.
      if (microcks.getSpec().getKeycloak().getUrl() != null) {
         builder.editSpec().withHost(microcks.getSpec().getKeycloak().getUrl());
      }

      // Add optional TLS configuration if specified in route spec.
      final OpenShiftRouteSpec routeSpec = microcks.getSpec().getKeycloak().getOpenshift().getRoute();
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
         builder.editSpec().editTls().withDestinationCACertificate(routeSpec.getTlsDestinationCaCertificate()).endTls().endSpec();
      }
      if (routeSpec.getTlsInsecureEdgeTerminationPolicy() != null) {
         builder.editSpec().editTls().withInsecureEdgeTerminationPolicy(routeSpec.getTlsInsecureEdgeTerminationPolicy()).endTls().endSpec();
      }

      return builder.build();
   }

   /**
    *
    * @param microcks
    * @param context
    * @return
    */
   public static Ingress prepareIngress(Microcks microcks, Context<Microcks> context) {
      logger.infof("Preparing desired Keycloak Ingress for '%s'", microcks.getMetadata().getName());


      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final IngressSpec spec = microcks.getSpec().getKeycloak().getIngress();

      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getRouteName(microcks))
               .addToLabels("app", microcksName)
               .addToLabels("group", "microcks")
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
            .endMetadata()
            .withNewSpec()
               .addNewTl()
                  .addToHosts(microcks.getSpec().getKeycloak().getUrl())
                  .withSecretName(IngressSpecUtil.getSecretName(spec, getIngressSecretName(microcks)))
               .endTl()
               .addNewRule()
                  .withHost(microcks.getSpec().getKeycloak().getUrl())
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

      // Add complementary annotations if any.
      Map<String, String> annotations = IngressSpecUtil.getAnnotationsIfAny(spec);
      if (annotations != null) {
         builder.editMetadata().addToAnnotations(annotations);
      }

      return builder.build();
   }
}
