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
package io.github.microcks.operator.base;

import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.base.resources.MicrocksDeploymentDependentResource;
import io.github.microcks.operator.base.resources.MicrocksReadyCondition;
import io.github.microcks.operator.base.resources.MicrocksServiceDependentResource;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;
import io.github.microcks.operator.base.resources.MicrocksConfigMapDependentResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import java.util.Arrays;

/**
 * A manager of Kubernetes secondary resources for Microcks module defined by a {@type MicrocksSpec} custom
 * resource specification. Takes care of initialising a reconciliation workflow as well as event sources for
 * the different dependent resources.
 * @author laurent
 */
public class MicrocksDependentResourcesManager {

   private KubernetesClient client;

   private KubernetesDependentResource<ConfigMap, Microcks> configMapDR;
   private KubernetesDependentResource<Deployment, Microcks> deploymentDR;
   private KubernetesDependentResource<Service, Microcks> serviceDR;

   public MicrocksDependentResourcesManager(KubernetesClient client) {
      this.client = client;
   }

   /**
    * Build and configure a reconciliation Workflow for all Microcks module dependent resources.
    * @return A JOSDK reconciliation workflow.
    */
   public Workflow<Microcks> buildReconcialiationWorkflow() {
      configMapDR = new MicrocksConfigMapDependentResource();
      deploymentDR = new MicrocksDeploymentDependentResource();
      serviceDR = new MicrocksServiceDependentResource();

      // Build the workflow.
      WorkflowBuilder<Microcks> builder = new WorkflowBuilder<>();

      // Configure the dependent resources.
      Arrays.asList(configMapDR, deploymentDR, serviceDR).forEach(dr -> {
         dr.setKubernetesClient(client);
         if (dr instanceof NamedSecondaryResourceProvider<?>) {
            dr.setResourceDiscriminator(new ResourceIDMatcherDiscriminator<>(
                        p -> new ResourceID(
                              ((NamedSecondaryResourceProvider<Microcks>) dr).getSecondaryResourceName(p),
                              p.getMetadata().getNamespace())
                  )
            );
         }
         builder.addDependentResource(dr);
      });

      builder.addDependentResource(deploymentDR).withReadyPostcondition(new MicrocksReadyCondition());

      return builder.build();
   }

   /**
    * Initialize event sources for all the Microcks module dependent resources.
    * @param context The event source context for the Microcks primary resource
    * @return An array of configured EventSources.
    */
   public EventSource[] initEventSources(EventSourceContext<Microcks> context) {
      return new EventSource[]{
            configMapDR.initEventSource(context),
            deploymentDR.initEventSource(context),
            serviceDR.initEventSource(context)
      };
   }
}
