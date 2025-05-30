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

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.base.v1alpha1.KeycloakSpec;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

/**
 * A Keycloak Kubernetes Deployment dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class KeycloakDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-keycloak";

   /** Default empty constructor. */
   public KeycloakDeploymentDependentResource() {
      super(Deployment.class);
   }

   /**
    * Get the name of Deployment given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of Deployment
    */
   public static final String getDeploymentName(Microcks microcks) {
      return microcks.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getDeploymentName(primary);
   }

   @Override
   protected Deployment desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Keycloak Deployment for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();
      final MicrocksSpec spec = microcks.getSpec();

      int keycloakMajorVersion = getKeycloakMajorVersion(spec);

      Deployment deployment = null;
      if (keycloakMajorVersion >= 26) {
         deployment = ReconcilerUtils.loadYaml(Deployment.class, getClass(), "/k8s/keycloak-26-deployment.yml");
      } else {
         deployment = ReconcilerUtils.loadYaml(Deployment.class, getClass(), "/k8s/keycloak-deployment.yml");
      }
      DeploymentBuilder builder = new DeploymentBuilder(deployment)
            .editMetadata()
               .withName(getDeploymentName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("app.kubernetes.io/name", getDeploymentName(microcks))
               .addToLabels("app.kubernetes.io/version", microcks.getSpec().getVersion())
               .addToLabels("app.kubernetes.io/part-of", microcksName)
               .addToLabels(spec.getCommonLabels())
               .addToAnnotations(spec.getCommonAnnotations())
            .endMetadata()
            .editSpec()
               .editSelector().addToMatchLabels("app", microcksName).endSelector()
               .editTemplate()
                  // make sure label selector matches label (which has to be matched by service selector too)
                  .editMetadata()
                     .addToLabels("app", microcksName)
                     .addToLabels(spec.getCommonLabels())
                     .addToAnnotations(spec.getCommonAnnotations())
                  .endMetadata()
                  .editSpec()
                     .editFirstContainer()
                        .withImage(spec.getKeycloak().getImage().getCoordinates())
                        .withResources(spec.getKeycloak().getResources())
                        .addNewEnv()
                           .withName("KEYCLOAK_ADMIN")
                           .withNewValueFrom()
                              .withNewSecretKeyRef()
                                 .withName(KeycloakSecretDependentResource.getSecretName(microcks))
                                 .withKey(KeycloakSecretDependentResource.KEYCLOAK_ADMIN_KEY)
                              .endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .addNewEnv()
                           .withName("KEYCLOAK_ADMIN_PASSWORD")
                           .withNewValueFrom()
                              .withNewSecretKeyRef()
                                 .withName(KeycloakSecretDependentResource.getSecretName(microcks))
                                 .withKey(KeycloakSecretDependentResource.KEYCLOAK_ADMIN_PASSWORD_KEY)
                              .endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                     .endContainer()
                     .addNewVolume()
                        .withName("keycloak-config")
                        .withNewSecret()
                           .withSecretName(KeycloakConfigSecretDependentResource.getSecretName(microcks))
                        .endSecret()
                     .endVolume()
                  .endSpec()
               .endTemplate()
            .endSpec();


      if (spec.getKeycloak().isInstall() || spec.getKeycloak().getPrivateUrl() != null) {
         if (keycloakMajorVersion >= 26) {
            // Add the Keycloak URL as hostname arg with https prefix.
            builder.editSpec().editTemplate().editSpec()
                  .editFirstContainer()
                  .addToArgs("--hostname=https://" + microcks.getStatus().getKeycloakUrl())
                  .endContainer()
                  .endSpec().endTemplate().endSpec();
            builder.editSpec().editTemplate().editSpec()
                  .editFirstContainer()
                     .addToArgs("--hostname-backchannel-dynamic=true")
                  .endContainer()
                  .endSpec().endTemplate().endSpec();
         } else {
            // Add the Keycloak URL as hostname arg.
            builder.editSpec().editTemplate().editSpec()
                  .editFirstContainer()
                  .addToArgs("--hostname=" + microcks.getStatus().getKeycloakUrl())
                  .endContainer()
                  .endSpec().endTemplate().endSpec();
            builder.editSpec().editTemplate().editSpec()
                  .editFirstContainer()
                     .addToArgs("--hostname-strict-backchannel=false")
                  .endContainer()
                  .endSpec().endTemplate().endSpec();
         }
      }

      // Complete configuration with optional stuffs.
      if (spec.getCommonAffinities() != null) {
         builder.editSpec().editTemplate().editSpec().withAffinity(spec.getCommonAffinities()).endSpec().endTemplate().endSpec();
      }
      if (spec.getCommonTolerations() != null) {
         builder.editSpec().editTemplate().editSpec().withTolerations(spec.getCommonTolerations()).endSpec().endTemplate().endSpec();
      }

      return builder.build();
   }

   private int getKeycloakMajorVersion(MicrocksSpec spec) {
      KeycloakSpec keycloakSpec = spec.getKeycloak();
      String[] parts = keycloakSpec.getImage().getTag().split("\\.");
      try {
         return Integer.parseInt(parts[0]);
      } catch (NumberFormatException nfe) {
         logger.warnf("Cannot parse Keycloak version from tag '%s'", keycloakSpec.getImage().getTag());
      }
      if ("nightly".equals(spec.getVersion())) {
         return 26;
      }
      return 24;
   }
}
