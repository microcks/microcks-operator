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
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

/**
 * A Microcks GRPC Kubernetes Service dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class MicrocksGRPCServiceDependentResource extends CRUDKubernetesDependentResource<Service, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-grpc";

   /** Default empty constructor. */
   public MicrocksGRPCServiceDependentResource() {
      super(Service.class);
   }

   /**
    * Get the name of Service given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of Service
    */
   public static String getServiceName(Microcks microcks) {
      return MicrocksDeploymentDependentResource.getDeploymentName(microcks) + RESOURCE_SUFFIX;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getServiceName(primary);
   }

   @Override
   protected Service desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Microcks GRPC Service for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();

      ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
               .withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("container", "spring")
               .addToLabels("group", "microcks")
            .endMetadata()
            .withNewSpec()
               .addToSelector("app", microcksName)
               .addToSelector("container", "spring")
               .addToSelector("group", "microcks")
               .addNewPort()
                  .withName("spring-grpc")
                  .withPort(9090)
                  .withProtocol("TCP")
                  .withTargetPort(new IntOrString(9090))
               .endPort()
               .withSessionAffinity("None")
               .withType("ClusterIP")
            .endSpec();

      return builder.build();
   }
}
