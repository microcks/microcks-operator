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
package io.github.microcks.operator;

import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksStatus;
import io.github.microcks.operator.api.model.Condition;
import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.base.AsyncFeatureDependentResourcesManager;
import io.github.microcks.operator.base.KeycloakDependentResourcesManager;
import io.github.microcks.operator.base.MicrocksDependentResourcesManager;
import io.github.microcks.operator.base.MongoDBDependentResourcesManager;
import io.github.microcks.operator.base.PostmanRuntimeDependentResourcesManager;
import io.github.microcks.operator.base.resources.KeycloakIngressesPreparer;
import io.github.microcks.operator.base.resources.MicrocksIngressesPreparer;
import io.github.microcks.operator.base.resources.StrimziKafkaResource;
import io.github.microcks.operator.base.resources.StrimziKafkaTopicResource;
import io.github.microcks.operator.model.IngressSpecUtil;
import io.github.microcks.operator.model.ResourceMerger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

/**
 * Reconciliation entry point for the {@code Microcks} Kubernetes custom resource.
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

   private SimpleDateFormat transitionFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

   private Workflow<Microcks> keycloakModuleWF;
   private Workflow<Microcks> mongoDBModuleWF;
   private Workflow<Microcks> microcksModuleWF;
   private Workflow<Microcks> postmanRuntimeModuleWF;
   private Workflow<Microcks> asyncFeatureModuleWF;

   private KeycloakDependentResourcesManager keycloakReconciler;
   private MongoDBDependentResourcesManager mongoDBReconciler;
   private MicrocksDependentResourcesManager microcksReconciler;
   private PostmanRuntimeDependentResourcesManager postmanRuntimeReconciler;
   private AsyncFeatureDependentResourcesManager asyncFeatureReconciler;

   /**
    *
    * @param client
    */
   public MicrocksReconciler(KubernetesClient client) {
      this.client = client;

      // Build resources manager and reconciliation workflow for Keycloak module.
      keycloakReconciler = new KeycloakDependentResourcesManager(client);
      keycloakModuleWF = keycloakReconciler.buildReconciliationWorkflow();

      // Build resources manager and reconciliation workflow for MongoDB module.
      mongoDBReconciler = new MongoDBDependentResourcesManager(client);
      mongoDBModuleWF = mongoDBReconciler.buildReconciliationWorkflow();

      // Build resources manager and reconciliation workflow for Microcks module.
      microcksReconciler = new MicrocksDependentResourcesManager(client);
      microcksModuleWF = microcksReconciler.buildReconcialiationWorkflow();

      // Build resources manager and reconciliation workflow for Postman module.
      postmanRuntimeReconciler = new PostmanRuntimeDependentResourcesManager(client);
      postmanRuntimeModuleWF = postmanRuntimeReconciler.buildReconciliationWorkflow();

      // Build resources manager and reconciliation workflow for Async minion module.
      asyncFeatureReconciler = new AsyncFeatureDependentResourcesManager(client);
      asyncFeatureModuleWF = asyncFeatureReconciler.buildReconciliationWorkflow();
   }

   @Override
   public Map<String, EventSource> prepareEventSources(EventSourceContext<Microcks> context) {
      // Build names event sources from event sources coming from all the modules reconcilers.
      return EventSourceInitializer.nameEventSources(Stream.concat(
            Stream.concat(
                  Stream.concat(Arrays.stream(keycloakReconciler.initEventSources(context)),
                        Arrays.stream(mongoDBReconciler.initEventSources(context))),
                  Stream.concat(Arrays.stream(microcksReconciler.initEventSources(context)),
                        Arrays.stream(postmanRuntimeReconciler.initEventSources(context)))),
            Arrays.stream(asyncFeatureReconciler.initEventSources(context))).toArray(EventSource[]::new));
   }

   @Override
   public UpdateControl<Microcks> reconcile(Microcks microcks, Context<Microcks> context) throws Exception {
      final String ns = microcks.getMetadata().getNamespace();
      final MicrocksSpec spec = microcks.getSpec();

      boolean updateStatus = false;
      // Set a minimal status if not present.
      if (microcks.getStatus() == null) {
         microcks.setStatus(new MicrocksStatus());
         updateStatus = true;
      }

      logger.infof("Starting reconcile operation for '%s'", microcks.getMetadata().getName());

      /*
       * // Some diagnostic helpers during development.
       * ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
       * logger.info("initSpec: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec));
       * logger.info("ExtraProperties before: " + spec.getMicrocks().getExtraProperties());
       * logger.info("mockInvocationStats before: " + spec.getMicrocks().isMockInvocationStats());
       */

      // Load default values for CR and build a complete representation.
      Microcks defaultCR = loadDefaultMicrocksCR();

      MicrocksSpec completeSpec = merger.mergeResources(defaultCR.getSpec(), microcks.getSpec());
      Microcks completeCR = new Microcks();
      completeCR.setKind(microcks.getKind());
      completeCR.setMetadata(microcks.getMetadata());
      completeCR.setSpec(completeSpec);
      completeCR.setStatus(microcks.getStatus());

      /*
       * // Some diagnostic helpers during development.
       * logger.info("defaultCR: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(defaultCR.getSpec()));
       * logger.info("CompleteCR: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(completeCR));
       * logger.info("ExtraProperties after: " + completeCR.getSpec().getMicrocks().getExtraProperties());
       * logger.info("mockInvocationStats after: " + completeCR.getSpec().getMicrocks().isMockInvocationStats());
       */

      boolean isOpenShift = client.adapt(OpenShiftClient.class).isSupported();
      final List<OwnerReference> refs = List.of(getOwnerReference(completeCR));

      String microcksUrl = null;
      if (completeSpec.getMicrocks().getIngress().isExpose()) {
         if (isOpenShift && completeCR.getSpec().getMicrocks().getOpenshift().getRoute().isEnabled()) {
            // We can create an OpenShift Route here to get the Url.
            microcksUrl = manageRouteAndGetURL(MicrocksIngressesPreparer.prepareRoute(completeCR, context), ns, refs);
            logger.infof("Retrieved Microcks URL from Route: %s", microcksUrl);
         } else if (completeCR.getSpec().getMicrocks() != null && completeCR.getSpec().getMicrocks().getUrl() != null) {
            // We can create an Ingress here to get the Url.
            microcksUrl = manageIngressAndGetURL(completeCR, completeCR.getSpec().getMicrocks().getIngress(),
                  MicrocksIngressesPreparer.getIngressSecretName(microcks), completeCR.getSpec().getMicrocks().getUrl(),
                  MicrocksIngressesPreparer.prepareIngress(completeCR, context), ns, refs);
            logger.infof("Retrieved Microcks URL from Ingress: %s", microcksUrl);
         } else {
            // Problem.
            // Either on OpenShift and you should enable route in the CR
            // Either on vanilla Kubernetes and you should specify URL
            logger.error(
                  "No Microcks URL specified and OpenShift Route disabled. You must either add spec.microcks.url "
                        + "or spec.microcks.openshift.route.enabled=true in the Microcks custom resource.");
            microcks.getStatus().setStatus(Status.ERROR);
            microcks.getStatus().setMessage(
                  "\"No Microcks URL specified and OpenShift Route disabled. You must either add spec.microcks.url "
                        + "or spec.microcks.openshift.route.enabled=true in the Microcks custom resource.");
            return UpdateControl.updateStatus(microcks);
         }
      } else {
         if (spec.getMicrocks() != null && spec.getMicrocks().getUrl() != null) {
            microcksUrl = spec.getMicrocks().getUrl();
         } else {
            logger.error("No Microcks URL specified and not exposing. You must add spec.microcks.url");
            microcks.getStatus().setStatus(Status.ERROR);
            microcks.getStatus()
                  .setMessage("Not exposing Microcks and no URL specified. You must add spec.microcks.url");
            return UpdateControl.updateStatus(microcks);
         }
      }
      microcks.getStatus().setMicrocksUrl(microcksUrl);

      String keycloakUrl = null;
      if (spec.getKeycloak().isInstall() && completeSpec.getKeycloak().getIngress().isExpose()) {
         if (isOpenShift && spec.getKeycloak().getOpenshift().getRoute().isEnabled()) {
            // We can create an OpenShift Route here to get the Url.
            keycloakUrl = manageRouteAndGetURL(KeycloakIngressesPreparer.prepareRoute(completeCR, context), ns, refs);
            logger.infof("Retrieved Keycloak URL from Route: %s", keycloakUrl);
         } else if (completeCR.getSpec().getKeycloak().isInstall()
               && completeCR.getSpec().getKeycloak().getUrl() != null) {
            // We can create an Ingress here to get the Url.
            keycloakUrl = manageIngressAndGetURL(completeCR, completeCR.getSpec().getKeycloak().getIngress(),
                  KeycloakIngressesPreparer.getIngressSecretName(microcks), completeCR.getSpec().getKeycloak().getUrl(),
                  KeycloakIngressesPreparer.prepareIngress(completeCR, context), ns, refs);

            logger.infof("Retrieved Keycloak URL from Ingress: %s", keycloakUrl);
         } else {
            logger.error(
                  "No Keycloak URL specified and OpenShift Route disabled. You must either add spec.keycloak.url "
                        + "or spec.keycloak.openshift.route.enabled=true in the Microcks custom resource.");
            microcks.getStatus().setStatus(Status.ERROR);
            microcks.getStatus().setMessage(
                  "No Keycloak URL specified and OpenShift Route disabled. You must either add spec.keycloak.url "
                        + "or spec.keycloak.openshift.route.enabled=true in the Microcks custom resource.");
            return UpdateControl.updateStatus(microcks);
         }

      } else {
         if (spec.getKeycloak() != null && spec.getKeycloak().getUrl() != null) {
            keycloakUrl = spec.getKeycloak().getUrl();
         } else {
            logger.error(
                  "Not installing Keycloak but no URL specified. You must either add spec.keycloak.url or spec.keycloak.install=true with OpenShift support.");
            microcks.getStatus().setStatus(Status.ERROR);
            microcks.getStatus()
                  .setMessage("Not installing Keycloak but no URL specified. You must either add spec.keycloak.url "
                        + "or spec.keycloak.install=true with OpenShift support.");
            return UpdateControl.updateStatus(microcks);
         }
      }
      microcks.getStatus().setKeycloakUrl(keycloakUrl);

      // Reconcile all our different workflows and handle the results.
      WorkflowReconcileResult keycloakResult = keycloakModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(keycloakResult, microcks.getStatus(), "Keycloak") || updateStatus;

      WorkflowReconcileResult mongoDBResult = mongoDBModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(mongoDBResult, microcks.getStatus(), "Mongo") || updateStatus;

      WorkflowReconcileResult microcksResult = microcksModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(microcksResult, microcks.getStatus(), "Microcks") || updateStatus;

      WorkflowReconcileResult postmanResult = postmanRuntimeModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(postmanResult, microcks.getStatus(), "Postman") || updateStatus;

      WorkflowReconcileResult asyncFeatureResult = asyncFeatureModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(asyncFeatureResult, microcks.getStatus(), "Async") || updateStatus;

      /*
       * // Some diagnostic helpers during development.
       * Optional<WorkflowReconcileResult> workflowReconcileResult = context.managedDependentResourceContext().getWorkflowReconcileResult();
       * logger.info("workflowReconcileResult: " + workflowReconcileResult);
       * 
       * for (Deployment deployment : secondaryDeployments) {
       *   logger.infof("Deployment %s, ready replicas: %d", deployment.getMetadata().getName(), deployment.getStatus().getReadyReplicas());
       * }
       */

      //
      if (installStrimziKafka(completeCR)) {
         manageStrimziKafkaInstall(completeCR, context);
      }

      logger.infof("Finishing reconcile operation for '%s'", microcks.getMetadata().getName());
      logger.infof("=====================================================");

      if (updateStatus) {
         return UpdateControl.updateStatus(microcks);
      }

      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(Microcks microcks, Context<Microcks> context) {
      final MicrocksSpec spec = microcks.getSpec();
      logger.infof("Starting cleanup operation for '%s'", microcks.getMetadata().getName());

      try {
         // Do our best to recreate complete CR in order to remove Strimzi watchers.
         Microcks defaultCR = loadDefaultMicrocksCR();
         MicrocksSpec completeSpec = merger.mergeResources(defaultCR.getSpec(), microcks.getSpec());

         Microcks completeCR = new Microcks();
         completeCR.setKind(microcks.getKind());
         completeCR.setMetadata(microcks.getMetadata());
         completeCR.setSpec(completeSpec);
         completeCR.setStatus(microcks.getStatus());

         if (installStrimziKafka(completeCR)) {
            unmanageStrimziKafkaInstall(completeCR, context);
         }
      } catch (Exception e) {
         logger.warnf("Failed re-building the complete CR during cleanup with %s", e.getMessage());
      }

      return DeleteControl.defaultDelete();
   }

   /** Load from YAML resource. */
   private Microcks loadDefaultMicrocksCR() throws Exception {
      return ReconcilerUtils.loadYaml(Microcks.class, getClass(), "/k8s/microcks-default.yml");
   }

   /** Build a new OwnerReference to assign to CR resources. */
   private OwnerReference getOwnerReference(Microcks primary) {
      return new OwnerReferenceBuilder().withController(true).withKind(primary.getKind())
            .withApiVersion(primary.getApiVersion()).withName(primary.getMetadata().getName())
            .withUid(primary.getMetadata().getUid()).build();
   }

   /**
    * Manage an OpenShift Route creation and retrieval of host name.
    * @param route The OpenShift Route to created or replace
    * @param ns    The namespace where to create it
    * @param refs  The controller owner references
    * @return The host to which the route is attached
    */
   protected String manageRouteAndGetURL(Route route, String ns, List<OwnerReference> refs) {
      route.getMetadata().setOwnerReferences(refs);
      route = client.adapt(OpenShiftClient.class).routes().inNamespace(ns).resource(route).createOrReplace();

      return route.getSpec().getHost();
   }

   /**
    * Manage an Ingress creation and retrieval of host name.
    * @param microcks    The primary resource this Ingress comes from
    * @param ingressSpec The specification of Ingress to create
    * @param secretName  The name of secret where TLS properties are stored
    * @param host        The host choosen for exposing the ingress
    * @param ingress     The INgress resource to create or replace
    * @param ns          The namespace where to create it
    * @param refs        The controller owner references
    * @return The host to which the ingress is attached (read from ingress spec).
    */
   protected String manageIngressAndGetURL(Microcks microcks, IngressSpec ingressSpec, String secretName, String host,
         Ingress ingress, String ns, List<OwnerReference> refs) {
      createIngressSecretIfNeeded(microcks, ingressSpec, secretName, host);
      ingress.getMetadata().setOwnerReferences(refs);
      ingress = client.network().v1().ingresses().inNamespace(ns).resource(ingress).createOrReplace();

      return ingress.getSpec().getRules().get(0).getHost();
   }

   /**
    * Manage creation of Ingress Secret if required.
    * @param microcks   The primary resource this Ingress comes from
    * @param spec       The specification of Ingress to create a secret for
    * @param secretName The name of secret where TLS properties are stored
    * @param host       The host choosen for exposing the ingress
    */
   protected void createIngressSecretIfNeeded(Microcks microcks, IngressSpec spec, String secretName, String host) {
      if (IngressSpecUtil.generateCertificateSecret(spec)) {
         final String ns = microcks.getMetadata().getNamespace();
         final List<OwnerReference> refs = List.of(getOwnerReference(microcks));

         Map<String, String> labels = Map.of("app", microcks.getMetadata().getName(), "group", "microcks");

         Secret secret = client.secrets().inNamespace(ns).withName(secretName).get();
         if (secret == null) {
            logger.infof("Creating a new Ingress Secret named '%s'", secretName);
            Secret certSecret = IngressSpecUtil.generateSelfSignedCertificateSecret(secretName, labels, host);
            certSecret.getMetadata().setOwnerReferences(refs);
            client.secrets().inNamespace(ns).create(certSecret);
         }
      }
   }

   /**
    * Analyses the result of a reconciliation workflow, update status if needed and tell if updated as a result.
    * @param result The workflow reconciliation result
    * @param status The status sub-resource of the Microcks primary resource
    * @param module THe module to handle the status for
    * @return Whether the status has been updated.
    */
   protected boolean handleWorkflowReconcileResult(WorkflowReconcileResult result, MicrocksStatus status,
         String module) {
      logger.debugf("Reconciled %s dependents: %s", module, result.getReconciledDependents());
      boolean updateStatus = false;

      if (result.getReconciledDependents() != null && !result.getReconciledDependents().isEmpty()) {
         logger.debugf("We've reconciled %d dependent resources for module '%s'",
               result.getReconciledDependents().size(), module);

         if (!result.getNotReadyDependents().isEmpty()) {
            logger.info("  Got not ready dependents: " + result.getNotReadyDependents().size());
            for (DependentResource dependentResource : result.getNotReadyDependents()) {
               logger.info("    dependentResource: " + dependentResource);
            }
            Condition condition = getOrCreateCondition(status, module + "Deploying");
            condition.setStatus(Status.DEPLOYING);
            condition.setLastTransitionTime(getCurrentTransitionTime());
            updateStatus = true;
         } else if (result.allDependentResourcesReady()) {
            logger.info("  All dependents are ready!");
            Condition condition = getOrCreateCondition(status, module + "Ready");
            condition.setStatus(Status.READY);
            condition.setLastTransitionTime(getCurrentTransitionTime());
            updateStatus = true;
         }

         if (result.erroredDependentsExist()) {
            logger.error("  Some dependents are in error...");
            for (Map.Entry<DependentResource, ReconcileResult> entry : result.getReconcileResults().entrySet()) {
               logger.errorf(" - errored: '%s'", entry.getValue().toString());
            }
         }

         /*
          * // Some diagnostic helpers during development. for (DependentResource depRes :
          * result.getReconciledDependents()) { ReconcileResult recRes = result.getReconcileResults().get(depRes);
          * logger.debugf("- reconciled: '%s' with op", depRes.getClass().getSimpleName());
          * logger.infof("- reconciled: '%s' with op %s", depRes.getClass().getSimpleName(),
          * recRes.getSingleOperation()); }
          */
      }
      return updateStatus;
   }

   /**
    * Tell if we should install the Strimzi Kafka resources.
    * @param microcks The microcks primary resource Strimzi will be attached to
    * @return True if installation should be done, false otherwise.
    */
   protected boolean installStrimziKafka(Microcks microcks) {
      return microcks.getSpec().getFeatures().getAsync().isEnabled()
            && microcks.getSpec().getFeatures().getAsync().getKafka().isInstall();
   }

   /**
    * Manage the installation of the Strimzi Kafka resources.
    * @param microcks The microcks primary resource Strimzi will be attached to
    * @param context  The reconciliation context
    */
   protected void manageStrimziKafkaInstall(Microcks microcks, Context<Microcks> context) {
      // Build desired Strimzi Kafka broker and topic.
      StrimziKafkaResource strimziKafka = new StrimziKafkaResource(client);
      StrimziKafkaTopicResource strimziTopic = new StrimziKafkaTopicResource(client);
      GenericKubernetesResource strimziKafkaRes = strimziKafka.desired(microcks, context);
      GenericKubernetesResource strimziTopicRes = strimziTopic.desired(microcks, context);

      // Force the owner reference before creating or replacing those resources.
      strimziKafkaRes.getMetadata().setOwnerReferences(List.of(getOwnerReference(microcks)));
      strimziTopicRes.getMetadata().setOwnerReferences(List.of(getOwnerReference(microcks)));
      createOrReplaceGenericResource(strimziKafkaRes, microcks.getMetadata().getNamespace());
      createOrReplaceGenericResource(strimziTopicRes, microcks.getMetadata().getNamespace());
   }

   /**
    * Remove and un-watch the Strimzi Kafka resources.
    * @param microcks The microcks primary resource Strimzi are attached to
    * @param context  The reconciliation context
    */
   protected void unmanageStrimziKafkaInstall(Microcks microcks, Context<Microcks> context) {
      // Build desired Strimzi Kafka broker and topic.
      StrimziKafkaResource strimziKafka = new StrimziKafkaResource(client);
      StrimziKafkaTopicResource strimziTopic = new StrimziKafkaTopicResource(client);
      GenericKubernetesResource strimziKafkaRes = strimziKafka.desired(microcks, context);
      GenericKubernetesResource strimziTopicRes = strimziTopic.desired(microcks, context);

      removeGenericResourceWatcher(strimziKafkaRes, microcks.getMetadata().getNamespace());
      removeGenericResourceWatcher(strimziTopicRes, microcks.getMetadata().getNamespace());
   }

   /** */
   protected Condition getOrCreateCondition(MicrocksStatus status, String type) {
      Condition result = null;
      if (status.getConditions() != null) {
         for (Condition condition : status.getConditions()) {
            if (condition.getType().equals(type)) {
               result = condition;
               break;
            }
         }
      }
      if (result == null) {
         result = new Condition();
         result.setType(type);
         status.addCondition(result);
      }
      return result;
   }

   /** */
   protected String getCurrentTransitionTime() {
      return transitionFormat.format(Calendar.getInstance().getTime());
   }

   /** */
   private void createOrReplaceGenericResource(GenericKubernetesResource genericResource, String namespace) {
      // Create the generic Kubernetes resource.
      client.genericKubernetesResources(genericResource.getApiVersion(), genericResource.getKind())
            .inNamespace(namespace).resource(genericResource).createOrReplace();

      // Now take care about registering a watcher if necessary.
      WatcherKey watcherKey = new WatcherKey(genericResource.getMetadata().getName(), genericResource.getKind(),
            genericResource.getApiVersion());

      WatcherManager watchers = WatcherManager.getInstance();
      if (!watchers.hasWatcher(watcherKey)) {
         logger.infof("Registering a new watcher with key %s", watcherKey);
         Watcher watcher = new Watcher<GenericKubernetesResource>() {
            @Override
            public void eventReceived(Action action, GenericKubernetesResource resource) {
               logger.infof("Received event, action %s for %s", action.name(), resource.getMetadata().getName());
               if (Action.DELETED.equals(action)) {
                  logger.infof("Been deleted, current resource is %s", resource);
               }
            }

            @Override
            public void onClose(WatcherException cause) {
               logger.infof("Watcher was closed due to %e", cause.getMessage());
            }
         };
         client.genericKubernetesResources(genericResource.getApiVersion(), genericResource.getKind())
               .inNamespace(namespace).withName(genericResource.getMetadata().getName()).watch(watcher);
         watchers.registerWatcher(watcherKey, watcher);
      }
   }

   /** */
   private void removeGenericResourceWatcher(GenericKubernetesResource genericResource, String namespace) {
      WatcherKey watcherKey = new WatcherKey(genericResource.getMetadata().getName(), genericResource.getKind(),
            genericResource.getApiVersion());

      WatcherManager watchers = WatcherManager.getInstance();
      watchers.unregisterWatcher(watcherKey);
   }
}
