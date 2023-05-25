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
import io.github.microcks.operator.base.resources.MongoDBReadyCondition;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;
import io.github.microcks.operator.base.resources.MongoDBDeploymentDependentResource;
import io.github.microcks.operator.base.resources.MongoDBInstallPrecondition;
import io.github.microcks.operator.base.resources.MongoDBPVCDependentResource;
import io.github.microcks.operator.base.resources.MongoDBSecretDependentResource;
import io.github.microcks.operator.base.resources.MongoDBServiceDependentResource;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import java.util.Arrays;

/**
 * A manager of Kubernetes secondary resources for MongoDB module defined by a {@code MicrocksSpec} custom
 * resource specification. Takes care of initialising a reconciliation workflow as well as event sources for
 * the different dependent resources.
 * @author laurent
 */
public class MongoDBDependentResourcesManager {

   private KubernetesClient client;

   private KubernetesDependentResource<Secret, Microcks> secretDR;
   private KubernetesDependentResource<PersistentVolumeClaim, Microcks> dbPersistentVolumeDR;
   private KubernetesDependentResource<Deployment, Microcks> dbDeploymentDR;
   private KubernetesDependentResource<Service, Microcks> dbServiceDR;

   /**
    * Creates a MongoDBDependentResourcesManager.
    * @param client Kubernetes cluster client
    */
   public MongoDBDependentResourcesManager(KubernetesClient client) {
      this.client = client;
   }

   /**
    * Build and configure a reconciliation Workflow for all MongoDB module dependent resources.
    * @return A JOSDK reconciliation workflow.
    */
   public Workflow<Microcks> buildReconciliationWorkflow() {
      secretDR = new MongoDBSecretDependentResource();
      dbPersistentVolumeDR = new MongoDBPVCDependentResource();
      dbDeploymentDR = new MongoDBDeploymentDependentResource();
      dbServiceDR = new MongoDBServiceDependentResource();

      // Build the workflow.
      WorkflowBuilder<Microcks> builder = new WorkflowBuilder<>();
      Condition installedCondition = new MongoDBInstallPrecondition();

      // Configure the dependent resources.
      Arrays.asList(secretDR, dbPersistentVolumeDR, dbDeploymentDR,
            dbServiceDR).forEach( dr -> {
         dr.setKubernetesClient(client);
         if (dr instanceof NamedSecondaryResourceProvider<?>) {
            dr.setResourceDiscriminator(new ResourceIDMatcherDiscriminator<>(
                        p -> new ResourceID(
                              ((NamedSecondaryResourceProvider<Microcks>) dr).getSecondaryResourceName(p),
                              p.getMetadata().getNamespace())
                  )
            );
         }
         builder.addDependentResource(dr).withReconcilePrecondition(installedCondition);
      });

      builder.addDependentResource(dbDeploymentDR).withReadyPostcondition(new MongoDBReadyCondition());

      return builder.build();
   }

   /**
    * Initialize event sources for all the Keycloak module dependent resources.
    * @param context The event source context for the Microcks primary resource
    * @return An array of configured EventSources.
    */
   public EventSource[] initEventSources(EventSourceContext<Microcks> context) {
      return new EventSource[]{
         secretDR.initEventSource(context),
         dbPersistentVolumeDR.initEventSource(context),
         dbDeploymentDR.initEventSource(context),
         dbServiceDR.initEventSource(context)
      };
   }
}
