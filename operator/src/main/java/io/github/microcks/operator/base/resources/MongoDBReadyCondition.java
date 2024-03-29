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

import io.github.microcks.operator.api.base.v1alpha1.Microcks;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * A reconciliation post-condition that is only met if MongoDB module is ready.
 * @author laurent
 */
public class MongoDBReadyCondition implements Condition<Deployment, Microcks> {

   @Override
   public boolean isMet(Microcks primary, Deployment secondary, Context<Microcks> context) {
      return secondary.getStatus().getReadyReplicas() != null && secondary.getStatus().getReadyReplicas().equals(1);
   }
}
