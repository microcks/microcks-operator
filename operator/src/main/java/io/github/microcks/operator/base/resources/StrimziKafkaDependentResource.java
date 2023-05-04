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
package io.github.microcks.operator.base.resources;

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.dependent.managed.KubernetesClientAware;
import io.javaoperatorsdk.operator.processing.dependent.AbstractEventSourceHolderDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

/**
 * A Strimzi Kafka dependent resource.
 * @author laurent
 */
@KubernetesDependent(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR)
public class StrimziKafkaDependentResource extends AbstractEventSourceHolderDependentResource<HasMetadata, Microcks, InformerEventSource<HasMetadata, Microcks>>
      implements Creator<HasMetadata, Microcks>, Updater<HasMetadata, Microcks>,  KubernetesClientAware {

   KubernetesClient client;

   protected StrimziKafkaDependentResource() {
      super(HasMetadata.class);
   }

   @Override
   public void setKubernetesClient(KubernetesClient kubernetesClient) {
      this.client = kubernetesClient;
      /*
      List<HasMetadata> result = client.load(new FileInputStream(args[0])).get();
      client.resourceList(result).inNamespace("").createOrReplace();
      return client.resource().inNamespace().createOrReplace()
      */
   }

   @Override
   public KubernetesClient getKubernetesClient() {
      return null;
   }

   @Override
   protected InformerEventSource<HasMetadata, Microcks> createEventSource(EventSourceContext<Microcks> context) {
      return null;
   }

   @Override
   public HasMetadata create(HasMetadata desired, Microcks primary, Context<Microcks> context) {
      return null;
   }

   @Override
   public HasMetadata update(HasMetadata actual, HasMetadata desired, Microcks primary, Context<Microcks> context) {
      return null;
   }
}
