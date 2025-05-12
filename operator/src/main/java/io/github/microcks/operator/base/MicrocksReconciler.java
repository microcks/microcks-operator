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
package io.github.microcks.operator.base;

import io.github.microcks.operator.WatcherKey;
import io.github.microcks.operator.WatcherManager;
import io.github.microcks.operator.api.base.v1alpha1.FeaturesSpecBuilder;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksServiceSpecBuilder;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksStatus;
import io.github.microcks.operator.api.model.Condition;
import io.github.microcks.operator.api.model.ExpositionType;
import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.api.model.Status;
import io.github.microcks.operator.base.resources.KeycloakIngressesPreparer;
import io.github.microcks.operator.base.resources.MicrocksIngressesPreparer;
import io.github.microcks.operator.base.resources.StrimziKafkaResource;
import io.github.microcks.operator.base.resources.StrimziKafkaTopicResource;
import io.github.microcks.operator.model.ConditionUtil;
import io.github.microcks.operator.model.IngressSpecUtil;
import io.github.microcks.operator.model.ResourceMerger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
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

import java.util.Arrays;
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

   final KubernetesClient client;

   private final ResourceMerger merger = new ResourceMerger();

   private static final String KEYCLOAK_MODULE = "Keycloak";
   private static final String MONGODB_MODULE = "Mongo";
   private static final String MICROCKS_MODULE = "Microcks";
   private static final String POSTMAN_MODULE = "Postman";
   private static final String ASYNC_MODULE = "Async";

   private final Workflow<Microcks> keycloakModuleWF;
   private final Workflow<Microcks> mongoDBModuleWF;
   private final Workflow<Microcks> microcksModuleWF;
   private final Workflow<Microcks> postmanRuntimeModuleWF;
   private final Workflow<Microcks> asyncFeatureModuleWF;

   private final KeycloakDependentResourcesManager keycloakReconciler;
   private final MongoDBDependentResourcesManager mongoDBReconciler;
   private final MicrocksDependentResourcesManager microcksReconciler;
   private final PostmanRuntimeDependentResourcesManager postmanRuntimeReconciler;
   private final AsyncFeatureDependentResourcesManager asyncFeatureReconciler;

   /**
    * Default constructor with injected Kubernetes client.
    * @param client A Kubernetes client for interacting with the cluster
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

      // Load default values for CR and build a complete representation.
      MicrocksSpec defaultSpec = loadDefaultMicrocksSpec(microcks.getSpec().getVersion());
      MicrocksSpec completeSpec = merger.mergeResources(defaultSpec, microcks.getSpec());

      Microcks completeCR = new Microcks();
      completeCR.setKind(microcks.getKind());
      completeCR.setMetadata(microcks.getMetadata());
      completeCR.setSpec(completeSpec);
      completeCR.setStatus(microcks.getStatus());

//      // Some diagnostic helpers during development.
//      ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
//      logger.info("defaultSpec: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(defaultSpec));
//      logger.info("CompleteCR: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(completeCR));

      boolean isOpenShift = client.adapt(OpenShiftClient.class).isSupported();
      final List<OwnerReference> refs = List.of(getOwnerReference(completeCR));

      String microcksUrl = null;
      if (!ExpositionType.NONE.equals(completeSpec.getCommonExpositions().getType())) {
         if (isOpenShift && completeCR.getSpec().getMicrocks().getOpenshift().getRoute().isEnabled()) {
            // We can create an OpenShift Route here to get the Url.
            microcksUrl = manageRouteAndGetURL(MicrocksIngressesPreparer.prepareRoute(completeCR, context), ns, refs);
            logger.infof("Retrieved Microcks URL from Route: %s", microcksUrl);
         } else if (completeCR.getSpec().getMicrocks() != null && completeCR.getSpec().getMicrocks().getUrl() != null) {
            // Manage either an Ingress or an HTTPRoute.
            if (ExpositionType.INGRESS.equals(completeCR.getSpec().getCommonExpositions().getType())) {
               // We can create an Ingress here to get the Url.
               microcksUrl = manageIngressAndGetURL(completeCR, completeCR.getSpec().getMicrocks().getIngress(),
                     MicrocksIngressesPreparer.getIngressSecretName(microcks), completeCR.getSpec().getMicrocks().getUrl(),
                     MicrocksIngressesPreparer.prepareIngress(completeCR, context), ns, refs);
               logger.infof("Retrieved Microcks URL from Ingress: %s", microcksUrl);
            } else if (ExpositionType.GATEWAYROUTE.equals(completeCR.getSpec().getCommonExpositions().getType())) {
               // We can create an HTTPRoute here.
               microcksUrl = manageHTTPRouteAndGetURL(completeCR,
                     MicrocksIngressesPreparer.prepareHTTPRoute(completeCR, context), ns, refs);
               logger.infof("Retrieved Microcks URL from HTTPRoute: %s", microcksUrl);
            }
         } else {
            // Houston, we have a problem...
            // Either on OpenShift and you should enable route in the CR.
            // Either on vanilla Kubernetes and you should specify URL.
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

      if (spec.getKeycloak().isEnabled()) {
         String keycloakUrl = null;
         if (spec.getKeycloak().isInstall() && completeSpec.getKeycloak().getIngress().isExpose()) {
            if (isOpenShift && spec.getKeycloak().getOpenshift().getRoute().isEnabled()) {
               // We can create an OpenShift Route here to get the Url.
               keycloakUrl = manageRouteAndGetURL(KeycloakIngressesPreparer.prepareRoute(completeCR, context), ns, refs);
               logger.infof("Retrieved Keycloak URL from Route: %s", keycloakUrl);
            } else if (completeCR.getSpec().getKeycloak().isInstall() && completeCR.getSpec().getKeycloak().getUrl() != null) {
               // Manage either an Ingress or an HTTPRoute.
               if (ExpositionType.INGRESS.equals(completeCR.getSpec().getCommonExpositions().getType())) {
                  // We can create an Ingress here to get the Url.
                  keycloakUrl = manageIngressAndGetURL(completeCR, completeCR.getSpec().getKeycloak().getIngress(),
                        KeycloakIngressesPreparer.getIngressSecretName(microcks), completeCR.getSpec().getKeycloak().getUrl(),
                        KeycloakIngressesPreparer.prepareIngress(completeCR, context), ns, refs);

                  logger.infof("Retrieved Keycloak URL from Ingress: %s", keycloakUrl);
               } else if (ExpositionType.GATEWAYROUTE.equals(completeCR.getSpec().getCommonExpositions().getType())) {
                  // We can create an HTTPRoute here.
                  keycloakUrl = manageHTTPRouteAndGetURL(completeCR,
                        KeycloakIngressesPreparer.prepareHTTPRoute(completeCR, context), ns, refs);
                  logger.infof("Retrieved Keycloak URL from HTTPRoute: %s", keycloakUrl);
               }
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
      }

      // Reconcile all our different workflows and handle the results.
      WorkflowReconcileResult keycloakResult = keycloakModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(keycloakResult, microcks.getStatus(), KEYCLOAK_MODULE) || updateStatus;
      logger.infof("Keycloak reconciliation triggered an update? %s", updateStatus);

      WorkflowReconcileResult mongoDBResult = mongoDBModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(mongoDBResult, microcks.getStatus(), MONGODB_MODULE) || updateStatus;
      logger.infof("Mongo reconciliation triggered an update?: %s", updateStatus);

      WorkflowReconcileResult microcksResult = microcksModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(microcksResult, microcks.getStatus(), MICROCKS_MODULE) || updateStatus;
      logger.infof("Microcks reconciliation triggered an update?: %s", updateStatus);

      WorkflowReconcileResult postmanResult = postmanRuntimeModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(postmanResult, microcks.getStatus(), POSTMAN_MODULE) || updateStatus;
      logger.infof("Postman reconciliation triggered an update?: %s", updateStatus);

      WorkflowReconcileResult asyncFeatureResult = asyncFeatureModuleWF.reconcile(completeCR, context);
      updateStatus = handleWorkflowReconcileResult(asyncFeatureResult, microcks.getStatus(), ASYNC_MODULE) || updateStatus;
      logger.infof("Async reconciliation triggered an update?: %s", updateStatus);

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

      if (updateStatus) {
         logger.info("Returning an updateStatus control. ========================");
         logger.info("Global status before check is: " + microcks.getStatus().getStatus());
         checkIfGloballyReady(completeCR, microcks.getStatus());
         logger.info("Global status after check is: " + microcks.getStatus().getStatus());
         return UpdateControl.updateStatus(microcks);
      }

      logger.info("Returning a noUpdate control. =============================");
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl cleanup(Microcks microcks, Context<Microcks> context) {
      final MicrocksSpec spec = microcks.getSpec();
      logger.infof("Starting cleanup operation for '%s'", microcks.getMetadata().getName());

      try {
         // Do our best to recreate complete CR in order to remove Strimzi watchers.
         /*
         Microcks defaultCR = loadDefaultMicrocksCR();
         MicrocksSpec completeSpec = merger.mergeResources(defaultCR.getSpec(), microcks.getSpec());
         */

         MicrocksSpec completeSpec = loadDefaultMicrocksSpec(microcks.getSpec().getVersion());

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

   /**
    * Load a MicrocksSpec with default values for given version
    * @param version The version of Microcks to get a full default spec for
    * @return A MicrocksSpec with default values
    * @throws Exception If default values cannot be loaded
    */
   public MicrocksSpec loadDefaultMicrocksSpec(String version) throws Exception {
      Microcks versionCR = null;
      MicrocksSpec versionSpec = null;

      // Look at specific version defaults first.
      try {
         versionCR = ReconcilerUtils.loadYaml(Microcks.class, getClass(), String.format("/k8s/defaults/microcks-%s-default.yml", version));
         versionSpec = versionCR.getSpec();
      } catch (Exception e) {
         // Default for this version does not exist, fallback to logical defaults.
         versionSpec = new MicrocksSpec();
         versionSpec.setVersion(version);
         versionSpec.setMicrocks(new MicrocksServiceSpecBuilder()
               .withNewImage()
                  .withRegistry("quay.io")
                  .withRepository("microcks/microcks")
                  .withTag(version)
               .endImage().build());
         versionSpec.setFeatures(new FeaturesSpecBuilder().withNewAsync()
               .withNewImage()
                  .withRegistry("quay.io")
                  .withRepository("microcks/microcks-async-minion")
                  .withTag(version)
               .endImage().endAsync().build());
      }

      // Look at minor version defaults if any.
      if (version.contains(".")) {
         String[] versionDigits = version.split("\\.");
         try {
            Microcks minorCR = ReconcilerUtils.loadYaml(Microcks.class, getClass(),
                  String.format("/k8s/defaults/microcks-%s.%s-default.yml", versionDigits[0], versionDigits[1]));

            versionSpec = merger.mergeResources(minorCR.getSpec(), versionSpec);
         } catch (Exception e) {
            // Default for this minor does not exist. Nothing special to do here.
         }
      }

      // Finally, merge with global defaults.
      Microcks defaultCR = ReconcilerUtils.loadYaml(Microcks.class, getClass(), "/k8s/microcks-default.yml");
      return merger.mergeResources(defaultCR.getSpec(), versionSpec);
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
    * @param host        The host chosen for exposing the ingress
    * @param ingress     The Ingress resource to create or replace
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
    * Manage an HTTPRoute creation and retrieval of host name.
    * @param microcks The primary resource this HTTPRoute comes from
    * @param route    The HTTPRoute resource to create or replace
    * @param ns       The namespace where to create it
    * @param refs     The controller owner references
    * @return The host to which the HTTPRoute is attached (read from the created or updated route).
    */
   protected String manageHTTPRouteAndGetURL(Microcks microcks, HTTPRoute route, String ns, List<OwnerReference> refs) {
      route.getMetadata().setOwnerReferences(refs);
      route = client.resource(route).inNamespace(ns).serverSideApply();

      return route.getSpec().getHostnames().get(0);
   }

   /**
    * Manage creation of Ingress Secret if required.
    * @param microcks   The primary resource this Ingress comes from
    * @param spec       The specification of Ingress to create a secret for
    * @param secretName The name of secret where TLS properties are stored
    * @param host       The host chosen for exposing the ingress
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
   protected boolean handleWorkflowReconcileResult(WorkflowReconcileResult result, MicrocksStatus status, String module) {
      logger.debugf("Reconciled %s dependents: %s", module, result.getReconciledDependents());
      boolean updateStatus = false;

      if (result.getReconciledDependents() != null && !result.getReconciledDependents().isEmpty()) {
         logger.debugf("We've reconciled %d dependent resources for module '%s'",
               result.getReconciledDependents().size(), module);

         if (!result.getNotReadyDependents().isEmpty()) {
            logger.debugf("  Got not ready dependents: " + result.getNotReadyDependents().size());
            for (DependentResource dependentResource : result.getNotReadyDependents()) {
               logger.debugf("    dependentResource: %s", dependentResource);
            }
            Condition condition = ConditionUtil.getOrCreateCondition(status, module + "Deploying");
            condition.setStatus(Status.DEPLOYING);
            ConditionUtil.touchConditionTime(condition);
            updateStatus = true;
         } else if (result.allDependentResourcesReady()) {
            logger.debugf("  All dependents are ready!");
            Condition condition = ConditionUtil.getOrCreateCondition(status, module + "Ready");
            // It may already have been set to ready by previous reconciliation.
            if (Status.READY != condition.getStatus()) {
               condition.setStatus(Status.READY);
               ConditionUtil.touchConditionTime(condition);
               updateStatus = true;
            }
         }

         if (result.erroredDependentsExist()) {
            logger.error("  Some dependents are in error...");
            for (Map.Entry<DependentResource, ReconcileResult> entry : result.getReconcileResults().entrySet()) {
               logger.errorf(" - errored: '%s'", entry.getValue().toString());
            }
         }
      } else {
         logger.debugf("  No dependents to reconcile. Mark module as ready!");
         Condition condition = ConditionUtil.getOrCreateCondition(status, module + "Ready");
         condition.setStatus(Status.READY);
         ConditionUtil.touchConditionTime(condition);
         updateStatus = true;
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
   protected void checkIfGloballyReady(Microcks completeCR, MicrocksStatus status) {
      MicrocksSpec spec = completeCR.getSpec();

      Condition microcksCondition = ConditionUtil.getCondition(status, MICROCKS_MODULE + "Ready");
      if (microcksCondition == null || microcksCondition.getStatus() != Status.READY) {
         return;
      }
      Condition postmanCondition = ConditionUtil.getCondition(status, POSTMAN_MODULE + "Ready");
      if (postmanCondition == null || postmanCondition.getStatus() != Status.READY) {
         return;
      }

      // Evaluate MongoDB condition if you must install it.
      if (spec.getMongoDB().isInstall()) {
         Condition mongoCondition = ConditionUtil.getCondition(status, MONGODB_MODULE + "Ready");
         if (mongoCondition == null || mongoCondition.getStatus() != Status.READY) {
            return;
         }
      }
      // Evaluate Keycloak condition if you must install it.
      if (spec.getKeycloak().isInstall()) {
         Condition keycloakCondition = ConditionUtil.getCondition(status, KEYCLOAK_MODULE + "Ready");
         if (keycloakCondition == null || keycloakCondition.getStatus() != Status.READY) {
            return;
         }
      }
      // Evaluate Async condition if you must install it.
      if (spec.getFeatures().getAsync().isEnabled()) {
         Condition asyncCondition = ConditionUtil.getCondition(status, ASYNC_MODULE + "Ready");
         if (asyncCondition == null || asyncCondition.getStatus() != Status.READY) {
            return;
         }
      }
      status.setStatus(Status.READY);
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
