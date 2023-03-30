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
package io.github.microcks.operator;

import io.github.microcks.operator.api.Microcks;
import io.github.microcks.operator.resources.KeycloakDeploymentDependentResource;
import io.github.microcks.operator.resources.KeycloakInstallPrecondition;
import io.github.microcks.operator.resources.KeycloakSecretDependentResource;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow;
import io.javaoperatorsdk.operator.processing.dependent.workflow.builder.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import java.util.Arrays;

/**
 * @author laurent
 */
public class KeycloakDependentResourcesManager {

   private KubernetesClient client;

   private KubernetesDependentResource<Secret, Microcks> secretDR;
   private KubernetesDependentResource<Deployment, Microcks> deploymentDR;

   public KeycloakDependentResourcesManager(KubernetesClient client) {
      this.client = client;
   }

   public Workflow<Microcks> buildReconciliationWorkflow() {
      secretDR = new KeycloakSecretDependentResource();
      deploymentDR = new KeycloakDeploymentDependentResource();

      WorkflowBuilder<Microcks> builder = new WorkflowBuilder<>();
      Condition installedCondition = new KeycloakInstallPrecondition();

      Arrays.asList(secretDR, deploymentDR).forEach(dr -> {
         dr.setKubernetesClient(client);
         builder.addDependentResource(dr).withReconcilePrecondition(installedCondition);
      });

      return builder.build();
   }

   public EventSource[] initEventSources(EventSourceContext<Microcks> context) {
      return new EventSource[]{
            secretDR.initEventSource(context),
            deploymentDR.initEventSource(context)
      };
   }
}
