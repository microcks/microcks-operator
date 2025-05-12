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
import io.github.microcks.operator.model.IngressSpecUtil;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * An Async Minion Kubernetes Secret dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class AsyncMinionWSSecretDependentResource extends KubernetesDependentResource<Secret, Microcks>
      implements Creator<Secret, Microcks>, Deleter<Microcks>, NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-ws-ingress";

   /** Default empty constructor. */
   public AsyncMinionWSSecretDependentResource() {
      super(Secret.class);
   }

   /**
    * Get the name of Secret given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of Secret
    */
   public static final String getSecretName(Microcks microcks) {
      return IngressSpecUtil.getSecretName(microcks.getSpec().getFeatures().getAsync().getWs().getIngress(),
            microcks.getMetadata().getName() + RESOURCE_SUFFIX);
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getSecretName(primary);
   }

   @Override
   protected Secret desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Microcks WebSocket Secret for '%s'", microcks.getMetadata().getName());

      // Prepare labels and hosts.
      Map<String, String> labels = Map.of("app", microcks.getMetadata().getName(),
            "group", "microcks");
      List<String> hosts = List.of(
            AsyncMinionWSIngressDependentResource.getWSHost(microcks),
            AsyncMinionServiceDependentResource.getServiceName(microcks) + ".svc.cluster.local",
            "localhost");

      return IngressSpecUtil.generateSelfSignedCertificateSecret(getSecretName(microcks), labels, hosts);
   }
}
