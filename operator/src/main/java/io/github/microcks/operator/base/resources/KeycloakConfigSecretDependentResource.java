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
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.logging.Logger;

/**
 * A Keycloak Kubernetes ConfigMap dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class KeycloakConfigSecretDependentResource extends CRUDKubernetesDependentResource<Secret, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-keycloak-config";

   /** Default empty constructor. */
   public KeycloakConfigSecretDependentResource() {
      super(Secret.class);
   }

   /**
    * Get the name of Secret given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of Secret
    */
   public static final String getSecretName(Microcks microcks) {
      return microcks.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getSecretName(primary);
   }

   @Override
   protected Secret desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Keycloak ConfigMap for '%s'", microcks.getMetadata().getName());

      // Compute realm-config with Qute template.
      String realmConfig = Templates.microcksRealm(microcks.getSpec()).render();

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      SecretBuilder builder = new SecretBuilder().withNewMetadata().withName(getSecondaryResourceName(microcks))
            .withNamespace(microcksMetadata.getNamespace()).addToLabels("app", microcksName)
            .addToLabels("container", "keycloak").addToLabels("group", "microcks").endMetadata()
            .addToStringData("microcks-realm.json", realmConfig);

      return builder.build();
   }

   /** A Qute templates accessor. */
   @CheckedTemplate
   public static class Templates {
      /** Qute template for microcks-realm.json. */
      public static native TemplateInstance microcksRealm(MicrocksSpec spec);
   }
}
