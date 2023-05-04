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

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.base.v1alpha1.KafkaAuthenticationSpec;
import io.github.microcks.operator.api.base.v1alpha1.KafkaAuthenticationType;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

/**
 * An Async Minion Kubernetes Deployment dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class AsyncMinionDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX  = "-async-minion";

   public AsyncMinionDeploymentDependentResource() {
      super(Deployment.class);
   }

   public static final String getDeploymentName(Microcks microcks) {
      return microcks.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getDeploymentName(primary);
   }

   @Override
   protected Deployment desired(Microcks microcks, Context<Microcks> context) {
      logger.infof("Building desired Async Minion Deployment for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();

      Deployment deployment = ReconcilerUtils.loadYaml(Deployment.class, getClass(), "/k8s/async-minion-deployment.yml");
      DeploymentBuilder builder = new DeploymentBuilder(deployment)
            .editMetadata()
               .withName(getDeploymentName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("app.kubernetes.io/name", getDeploymentName(microcks))
               .addToLabels("app.kubernetes.io/version", microcks.getSpec().getVersion())
               .addToLabels("app.kubernetes.io/part-of", microcksName)
            .endMetadata()
            .editSpec()
               .editSelector().addToMatchLabels("app", microcksName).endSelector()
               .editTemplate()
                  // make sure label selector matches label (which has to be matched by service selector too)
                  .editMetadata().addToLabels("app", microcksName).endMetadata()
                  .editSpec()
                     .editFirstVolume()
                        .editConfigMap()
                           .withName(AsyncMinionConfigMapDependentResource.getConfigMapName(microcks))
                        .endConfigMap()
                     .endVolume()
                  .endSpec()
               .endTemplate()
            .endSpec();

      // We may have additional security config if external Kafka.
      if (!spec.getFeatures().getAsync().getKafka().isInstall()) {
         KafkaAuthenticationSpec authenticationSpec = spec.getFeatures().getAsync().getKafka().getAuthentication();

         // If there's a security mechanism, it has at least a truststore.
         if (authenticationSpec.getType() != KafkaAuthenticationType.NONE) {
            builder.editSpec()
                  .editTemplate()
                     .editSpec()
                        .editFirstContainer()
                           .addNewEnv()
                              .withName("KAFKA_TRUSTSTORE_PASSWORD")
                              .withNewValueFrom()
                                 .withNewSecretKeyRef()
                                    .withName("kafka-truststore")
                                    .withKey(authenticationSpec.getTruststoreSecretRef().getAdditionalProperties().get("passwordKey").toString())
                                 .endSecretKeyRef()
                              .endValueFrom()
                           .endEnv()
                           .addNewVolumeMount()
                              .withName("kafka-truststore")
                              .withMountPath("/deployments/config/kafka/truststore")
                           .endVolumeMount()
                        .endContainer()
                        .addNewVolume()
                           .withName("kafka-truststore")
                           .withNewSecret()
                              .withSecretName(authenticationSpec.getTruststoreSecretRef().getName())
                           .endSecret()
                        .endVolume()
                     .endSpec()
                  .endTemplate().endSpec();
         }
         if (authenticationSpec.getType() == KafkaAuthenticationType.SSL) {
            builder.editSpec()
                  .editTemplate()
                     .editSpec()
                        .editFirstContainer()
                           .addNewEnv()
                              .withName("KAFKA_KEYSTORE_PASSWORD")
                              .withNewValueFrom()
                                 .withNewSecretKeyRef()
                                    .withName("kafka-keystore")
                                    .withKey(authenticationSpec.getKeystoreSecretRef().getAdditionalProperties().get("passwordKey").toString())
                                 .endSecretKeyRef()
                              .endValueFrom()
                           .endEnv()
                           .addNewVolumeMount()
                              .withName("kafka-keystore")
                              .withMountPath("/deployments/config/kafka/keystore")
                           .endVolumeMount()
                        .endContainer()
                        .addNewVolume()
                           .withName("kafka-keystore")
                           .withNewSecret()
                              .withSecretName(authenticationSpec.getKeystoreSecretRef().getName())
                           .endSecret()
                        .endVolume()
                     .endSpec()
                  .endTemplate().endSpec();
         }
      }

      // If using Google PubSub, mount service account token from secret.
      if (spec.getFeatures().getAsync().getGooglepubsub() != null && spec.getFeatures().getAsync().getGooglepubsub().getServiceAccountSecretRef() != null) {
         builder.editSpec()
               .editTemplate()
                  .editSpec()
                     .editFirstContainer()
                        .addNewVolumeMount()
                           .withName("googlepubsub-sa")
                           .withMountPath("/deployments/config/googlepubsub/sa")
                        .endVolumeMount()
                     .endContainer()
                     .addNewVolume()
                        .withName("googlepubsub-sa")
                        .withNewSecret()
                           .withSecretName(spec.getFeatures().getAsync().getGooglepubsub().getServiceAccountSecretRef().getName())
                        .endSecret()
                     .endVolume()
                  .endSpec()
               .endTemplate().endSpec();
      }

      return builder.build();
   }
}
