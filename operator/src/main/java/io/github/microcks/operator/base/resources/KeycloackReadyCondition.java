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

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import java.util.Optional;

/**
 * A reconciliation post-condition that is only met if Keycloak module is ready.
 * @author laurent
 */
public class KeycloackReadyCondition implements Condition<Deployment, Microcks> {

   @Override
   public boolean isMet(DependentResource<Deployment, Microcks> dependentResource, Microcks microcks,
         Context<Microcks> context) {
      Optional<Deployment> dep = dependentResource.getSecondaryResource(microcks, context);
      return dep.isPresent() && dep.get().getStatus().getReadyReplicas() != null
            && dep.get().getStatus().getReadyReplicas().equals(1);
   }
}
