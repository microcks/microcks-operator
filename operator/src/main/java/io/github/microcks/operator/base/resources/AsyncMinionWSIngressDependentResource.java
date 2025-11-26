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
import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.model.IngressSpecUtil;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * An Async Minion Kubernetes Ingress dependent resource for WebSocket traffic.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class AsyncMinionWSIngressDependentResource extends CRUDKubernetesDependentResource<Ingress, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {
   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-ws";

   /** Default empty constructor. */
   public AsyncMinionWSIngressDependentResource() {
      super(Ingress.class);
   }

   /**
    * Get the WS host for the ingress.
    * @param primary The primary Microcks resource
    * @return The host for WS traffic
    */
   public static String getWSHost(Microcks primary) {
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
   protected Ingress desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Microcks WebSocket Ingress for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final IngressSpec spec = microcks.getSpec().getFeatures().getAsync().getWs().getIngress();

      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("group", "microcks")
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
               .addToAnnotations(Map.of("nginx.ingress.kubernetes.io/proxy-read-timeout", "3000",
                  "nginx.ingress.kubernetes.io/proxy-send-timeout", "3000"))
               .addToAnnotations(spec.getAnnotations())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .withNewSpec()
               .addNewRule()
                  .withHost(getWSHost(microcks))
                  .withNewHttp()
                     .addNewPath()
                     .withPath("/api/ws")
                     .withPathType("Prefix")
                     .withNewBackend()
                        .withNewService()
                           .withName(AsyncMinionServiceDependentResource.getServiceName(microcks))
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
      if (spec != null && spec.getClassName() != null) {
         builder.editSpec().withIngressClassName(spec.getClassName()).endSpec();
      }

      // Add complementary annotations if any.
      Map<String, String> annotations = IngressSpecUtil.getAnnotationsIfAny(spec);
      if (annotations != null) {
         builder.editMetadata().addToAnnotations(annotations).endMetadata();
      }

      return builder.build();
   }
}
