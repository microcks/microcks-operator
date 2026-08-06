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

import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.base.MicrocksReconciler;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is a unit test verifying that the {@code {#if minorVersion >= 14}} condition of the async-minion
 * {@code application.properties} template is correctly handled, i.e. that the
 * {@code microcks-asyncapi-triggers} Kafka properties are rendered for Microcks 1.14.0 and 1.15.0.
 * @author laurent
 */
@QuarkusTest
class AsyncMinionConfigMapDependentResourceTest {

   private static final String ASYNCAPI_TRIGGERS_BOOTSTRAP =
         "%kube.mp.messaging.incoming.microcks-asyncapi-triggers.bootstrap.servers=";

   private Microcks buildMicrocks(String version) throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());
      MicrocksSpec spec = reconciler.loadDefaultMicrocksSpec(version);
      spec.setVersion(version);

      Microcks microcks = new Microcks();
      microcks.setMetadata(new ObjectMetaBuilder().withName("microcks").withNamespace("test").build());
      microcks.setSpec(spec);
      return microcks;
   }

   private String renderApplicationProperties(String version) throws Exception {
      Microcks microcks = buildMicrocks(version);
      ConfigMap configMap = new AsyncMinionConfigMapDependentResource().desired(microcks, null);
      return configMap.getData().get("application.properties");
   }

   @Test
   void testAsyncApiTriggersPropertiesRenderedFor1_14_0() throws Exception {
      String applicationProperties = renderApplicationProperties("1.14.0");

      Assertions.assertTrue(applicationProperties.contains(ASYNCAPI_TRIGGERS_BOOTSTRAP),
            "microcks-asyncapi-triggers bootstrap.servers property must be present for Microcks 1.14.0");
   }

   @Test
   void testAsyncApiTriggersPropertiesRenderedFor1_15_0() throws Exception {
      String applicationProperties = renderApplicationProperties("1.15.0");

      Assertions.assertTrue(applicationProperties.contains(ASYNCAPI_TRIGGERS_BOOTSTRAP),
            "microcks-asyncapi-triggers bootstrap.servers property must be present for Microcks 1.15.0");
   }
}

