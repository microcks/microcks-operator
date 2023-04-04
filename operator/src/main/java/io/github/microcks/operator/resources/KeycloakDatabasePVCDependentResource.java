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
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jboss.logging.Logger;

/**
 * A Keycloak Kubernetes Persistent Volume Claim dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class KeycloakDatabasePVCDependentResource extends CRUDKubernetesDependentResource<PersistentVolumeClaim, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   public KeycloakDatabasePVCDependentResource() {
      super(PersistentVolumeClaim.class);
   }

   public static final String getPVCName(Microcks microcks) {
      return KeycloakDatabaseDeploymentDependentResource.getDeploymentName(microcks) ;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getPVCName(primary);
   }

   @Override
   protected PersistentVolumeClaim desired(Microcks microcks, Context<Microcks> context) {
      logger.infof("Building desired Keycloak DB PersistentVolumeClaim for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      PersistentVolumeClaimBuilder builder = new PersistentVolumeClaimBuilder()
            .withNewMetadata()
               .withName(getPVCName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("container", "keycloak-postgresql")
               .addToLabels("group", "microcks")
            .endMetadata()
            .withNewSpec()
               .withAccessModes("ReadWriteOnce")
               .withNewResources()
                  .addToRequests("storage", new Quantity(microcks.getSpec().getKeycloak().getVolumeSize()))
               .endResources()
            .endSpec();

      // Add optional storage class name if any.
      if (microcks.getSpec().getKeycloak().getStorageClassName() != null) {
         builder.editSpec()
                  .withStorageClassName(microcks.getSpec().getKeycloak().getStorageClassName())
               .endSpec();
      }
      return builder.build();
   }
}
