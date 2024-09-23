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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * A Keycloak Kubernetes Secret dependent resource holding realm configuration.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class KeycloakConfigSecretDependentResource extends KubernetesDependentResource<Secret, Microcks>
      implements Creator<Secret, Microcks>, Deleter<Microcks>, NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   /** The key used in secret to store the realm configuration. */
   public static final String REALM_CONFIG_KEY = "microcks-realm.json";
   /** The name of operator specific service account if any. */
   public static final String OPERATOR_SERVICE_ACCOUNT = "microcks-operator-serviceaccount";

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
      String operatorServiceAccountCredentials = UUID.randomUUID().toString();
      String realmConfig = Templates.microcksRealm(microcks.getSpec(), operatorServiceAccountCredentials).render();

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      SecretBuilder builder = new SecretBuilder().withNewMetadata().withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace()).addToLabels("app", microcksName)
               .addToLabels("container", "keycloak")
               .addToLabels("group", "microcks")
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .addToStringData(REALM_CONFIG_KEY, realmConfig);

      return builder.build();
   }

   /** A Qute templates accessor. */
   @CheckedTemplate
   public static class Templates {
      /** Qute template for microcks-realm.json. */
      public static native TemplateInstance microcksRealm(MicrocksSpec spec, String operatorServiceAccountCredentials);
   }
}
