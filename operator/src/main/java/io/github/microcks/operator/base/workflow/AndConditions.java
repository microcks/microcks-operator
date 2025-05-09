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
package io.github.microcks.operator.base.workflow;

import io.github.microcks.operator.api.base.v1alpha1.Microcks;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import java.util.Arrays;
import java.util.List;

/**
 * A composite reconciliation condition that is met if all the contained conditions are met.
 * @author laurent
 */
public class AndConditions implements Condition<HasMetadata, Microcks> {

   private final List<Condition<HasMetadata, Microcks>> conditions;

   /**
    * Defines a new AndConditions instance with the given conditions.
    * @param conditions The conditions to be met.
    */
   public AndConditions(Condition<HasMetadata, Microcks>... conditions) {
      this.conditions = Arrays.asList(conditions);
   }

   @Override
   public boolean isMet(DependentResource<HasMetadata, Microcks> dependentResource, Microcks primary, Context<Microcks> context) {
      for (Condition<HasMetadata, Microcks> condition : conditions) {
         if (!condition.isMet(dependentResource, primary, context)) {
            return false;
         }
      }
      return true;
   }
}
