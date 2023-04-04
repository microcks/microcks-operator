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
import io.github.microcks.operator.api.MicrocksSpec;
import io.github.microcks.operator.api.MicrocksStatus;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.model.ResourceMerger;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowReconcileResult;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

/**
 * @author laurent
 */
@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE)
@SuppressWarnings("unused")
@ApplicationScoped
public class MicrocksReconciler implements Reconciler<Microcks>, Cleaner<Microcks>, EventSourceInitializer<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @Inject
   KubernetesClient client;

   private ResourceMerger merger = new ResourceMerger();

   private Workflow<Microcks> keycloakModuleWF;
   private KeycloakDependentResourcesManager keycloakReconciler;


   public MicrocksReconciler(KubernetesClient client) {
      this.client = client;
      keycloakReconciler = new KeycloakDependentResourcesManager(client);
      keycloakModuleWF = keycloakReconciler.buildReconciliationWorkflow();
   }

   @Override
   public Map<String, EventSource> prepareEventSources(EventSourceContext<Microcks> context) {
      return EventSourceInitializer.nameEventSources(
            keycloakReconciler.initEventSources(context)
      );
   }

   @Override
   public UpdateControl<Microcks> reconcile(Microcks microcks, Context<Microcks> context) throws Exception {
      final MicrocksSpec spec = microcks.getSpec();
      logger.infof("Starting reconcile operation for '%s'", microcks.getMetadata().getName());

      Microcks defaultCR = loadDefaultMicrocksCR();
      MicrocksSpec completeSpec = merger.mergeResources(defaultCR.getSpec(), microcks.getSpec());
      Microcks completeCR = new Microcks();
      completeCR.setKind(microcks.getKind());
      completeCR.setMetadata(microcks.getMetadata());
      completeCR.setSpec(completeSpec);
      completeCR.setStatus(microcks.getStatus());

      //logger.info("CompleteCR: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(completeCR));

      Set<Secret> secondarySecrets = context.getSecondaryResources(Secret.class);
      logger.infof("Secondary Secrets watched: %d", secondarySecrets.size());
      Set<Deployment> secondaryDeployments = context.getSecondaryResources(Deployment.class);
      logger.infof("Secondary Deployments watched: %d", secondaryDeployments.size());
      Set<Service> secondaryServices = context.getSecondaryResources(Service.class);
      logger.infof("Secondary Services watched: %d", secondaryServices.size());

      Optional<WorkflowReconcileResult> workflowReconcileResult = context.managedDependentResourceContext().getWorkflowReconcileResult();
      logger.info("workflowReconcileResult: " + workflowReconcileResult);

      WorkflowReconcileResult keycloakResult = keycloakModuleWF.reconcile(completeCR, context);
      logger.info("Reconciled Keycloak dependents: " + keycloakResult.getReconciledDependents());

      if (keycloakResult.getReconciledDependents() != null && keycloakResult.getReconciledDependents().size() > 0) {
         logger.infof("We've reconciled %d Keycloak dependents", keycloakResult.getReconciledDependents().size());

         if (keycloakResult.getNotReadyDependents().size() > 0) {
            logger.info("  Got not ready dependents: " + keycloakResult.getNotReadyDependents().size());
         }
         if (keycloakResult.allDependentResourcesReady()) {
            logger.info("  All dependents are ready!");
         }

         for (DependentResource depRes : keycloakResult.getReconciledDependents()) {
            logger.infof("- reconciled: '%s'", depRes.getClass().getName());
            ReconcileResult recRes = keycloakResult.getReconcileResults().get(depRes);
            System.err.println(recRes.getSingleOperation());
            //System.err.println(recRes.getResourceOperations());
         }

         MicrocksStatus status = Objects.requireNonNullElse(microcks.getStatus(), new MicrocksStatus());
      }

      MicrocksStatus status = new MicrocksStatus();
      status.setStatus(Status.DEPLOYING);

      microcks.setStatus(status);

      logger.infof("Finishing reconcile operation for '%s'", microcks.getMetadata().getName());
      //return UpdateControl.patchStatus(microcks);
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(Microcks microcks, Context<Microcks> context) {
      final MicrocksSpec spec = microcks.getSpec();
      logger.infof("Starting cleanup operation for '%s'", microcks.getMetadata().getName());

      return DeleteControl.defaultDelete();
   }

   private Microcks loadDefaultMicrocksCR() throws Exception {
      return ReconcilerUtils.loadYaml(Microcks.class, getClass(), "/k8s/microcks-default.yml");
   }
}
