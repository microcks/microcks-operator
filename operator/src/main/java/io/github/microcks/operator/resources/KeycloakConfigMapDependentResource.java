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
package io.github.microcks.operator.resources;

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.Microcks;
import io.github.microcks.operator.api.MicrocksSpec;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
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
public class KeycloakConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-keycloak-config";

   public KeycloakConfigMapDependentResource() {
      super(ConfigMap.class);
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return primary.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   protected ConfigMap desired(Microcks microcks, Context<Microcks> context) {
      logger.infof("Building desired Keycloak ConfigMap for '%s'", microcks.getMetadata().getName());

      // Compute realm-config with Qute template.
      String realmConfig = Templates.microcksRealm(microcks.getSpec()).render();

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      ConfigMapBuilder builder = new ConfigMapBuilder()
            .withNewMetadata()
               .withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("container", "keycloak")
               .addToLabels("group", "microcks")
            .endMetadata()
            .addToData("microcks-realm.json", realmConfig);

      return builder.build();
   }

   @CheckedTemplate
   public static class Templates {
      public static native TemplateInstance microcksRealm(MicrocksSpec spec);
   }
}
