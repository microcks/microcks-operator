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
 * A Microcks Kubernetes Ingress dependent resource for gRPC traffic.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class MicrocksGRPCIngressDependentResource extends CRUDKubernetesDependentResource<Ingress, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-grpc";

   /** Default empty constructor. */
   public MicrocksGRPCIngressDependentResource() {
      super(Ingress.class);
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
   protected Ingress desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Microcks GRPC Ingress for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final IngressSpec spec = microcks.getSpec().getMicrocks().getGrpcIngress();

      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("group", "microcks")
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
               .addToAnnotations(Map.of("nginx.ingress.kubernetes.io/backend-protocol", "GRPC",
                  "nginx.ingress.kubernetes.io/ssl-passthrough", "true"))
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .withNewSpec()
               .addNewTl()
                  .addToHosts(getGRPCHost(microcks))
                  .withSecretName(MicrocksGRPCSecretDependentResource.getSecretName(microcks))
               .endTl()
               .addNewRule()
                  .withHost(getGRPCHost(microcks))
                  .withNewHttp()
                     .addNewPath()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                           .withNewService()
                              .withName(MicrocksGRPCServiceDependentResource.getServiceName(microcks))
                              .withNewPort()
                                 .withNumber(9090)
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
