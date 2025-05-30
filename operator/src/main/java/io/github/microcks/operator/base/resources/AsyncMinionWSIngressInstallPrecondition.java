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

import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.ExpositionType;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * A reconciliation pre-condition that is only met if WebSocket ingress should be managed.
 * @author laurent
 */
public class AsyncMinionWSIngressInstallPrecondition implements Condition<HasMetadata, Microcks> {

   @Override
   public boolean isMet(DependentResource<HasMetadata, Microcks> dependentResource, Microcks primary, Context<Microcks> context) {
      if (!ExpositionType.INGRESS.equals(primary.getSpec().getCommonExpositions().getType())) {
         return false;
      }
      if (primary.getSpec().getFeatures().getAsync() != null && primary.getSpec().getFeatures().getAsync().getWs() != null
            && primary.getSpec().getFeatures().getAsync().getWs().getIngress() != null) {
         return primary.getSpec().getFeatures().getAsync().getWs().getIngress().isExpose();
      }
      return primary.getSpec().getCommonExpositions().getIngress().isExpose();
   }
}
