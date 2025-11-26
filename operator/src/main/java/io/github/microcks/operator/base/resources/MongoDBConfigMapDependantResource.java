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
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

/**
 * A MongoDB Kubernetes ConfigMap dependent resource.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class MongoDBConfigMapDependantResource extends CRUDKubernetesDependentResource<ConfigMap, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-mongodb-init";

   /** Default empty constructor. */
   public MongoDBConfigMapDependantResource() {
      super(ConfigMap.class);
   }

   /**
    * Get the name of ConfigMap given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of ConfigMap
    */
   public static final String getConfigMapName(Microcks microcks) {
      return microcks.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getConfigMapName(primary);
   }

   @Override
   protected ConfigMap desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired MongoDB ConfigMap for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      ConfigMap configMap = ReconcilerUtils.loadYaml(ConfigMap.class, getClass(), "/k8s/mongodb-configmap.yml");
      ConfigMapBuilder builder = new ConfigMapBuilder(configMap)
            .editMetadata()
               .withName(getConfigMapName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata();

      return builder.build();
   }
}
