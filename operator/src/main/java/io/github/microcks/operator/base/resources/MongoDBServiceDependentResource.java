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
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

/**
 * A MongoDB Kubernetes Service dependent resource.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class MongoDBServiceDependentResource extends CRUDKubernetesDependentResource<Service, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {


   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   /** The port used for exposition of the service. */
   public static final int MONGODB_SERVICE_PORT = 27017;

   /** Default empty constructor. */
   public MongoDBServiceDependentResource() {
      super(Service.class);
   }

   /**
    * Get the name of Service given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of Service
    */
   public static final String getServiceName(Microcks microcks) {
      return MongoDBDeploymentDependentResource.getDeploymentName(microcks);
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getServiceName(primary);
   }

   @Override
   protected Service desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired MongoDB Service for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();

      ServiceBuilder builder = new ServiceBuilder().withNewMetadata().withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("container", "mongodb")
               .addToLabels("group", "microcks")
               .addToLabels(spec.getCommonLabels())
               .addToAnnotations(spec.getCommonAnnotations())
            .endMetadata().withNewSpec()
            .addToSelector("app", microcksName).addToSelector("container", "mongodb").addToSelector("group", "microcks")
            .addNewPort().withName("mongodb").withPort(MONGODB_SERVICE_PORT).withProtocol("TCP")
            .withTargetPort(new IntOrString(MONGODB_SERVICE_PORT)).endPort().withSessionAffinity("None")
            .withType("ClusterIP").endSpec();

      return builder.build();
   }
}
