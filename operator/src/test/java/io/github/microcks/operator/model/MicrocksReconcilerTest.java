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
package io.github.microcks.operator.model;

import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.base.MicrocksReconciler;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is a unit test for the {@link MicrocksReconciler} class.
 * @author laurent
 */
class MicrocksReconcilerTest {

   @Test
   void testLoadDefaultMicrocksSpec() throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());

      MicrocksSpec spec = reconciler.loadDefaultMicrocksSpec("1.10.1");

      Assertions.assertEquals("1.10.1", spec.getVersion());
      Assertions.assertEquals("quay.io/microcks/microcks:1.10.1", spec.getMicrocks().getImage().getCoordinates());
      Assertions.assertEquals("quay.io/microcks/microcks-postman-runtime:0.6.0", spec.getPostman().getImage().getCoordinates());
      Assertions.assertEquals(1, spec.getMicrocks().getReplicas());
      Assertions.assertEquals(1, spec.getPostman().getReplicas());
   }

   @Test
   void testLoadDefaultMicrocksSpecForTestly() throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());

      MicrocksSpec spec = reconciler.loadDefaultMicrocksSpec("testly");

      Assertions.assertEquals("testly", spec.getVersion());
      Assertions.assertEquals("quay.io/microcks/microcks:nightly", spec.getMicrocks().getImage().getCoordinates());
      Assertions.assertEquals("quay.io/microcks/microcks-postman-runtime:nightly", spec.getPostman().getImage().getCoordinates());
      Assertions.assertEquals(2, spec.getPostman().getReplicas());
   }

   @Test
   void testLoadDefaultMicrocksSpecForUnknownVersion() throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());

      MicrocksSpec spec = reconciler.loadDefaultMicrocksSpec("1.10.2");

      Assertions.assertEquals("1.10.2", spec.getVersion());
      Assertions.assertEquals("quay.io/microcks/microcks:1.10.2", spec.getMicrocks().getImage().getCoordinates());
      Assertions.assertEquals("quay.io/microcks/microcks-postman-runtime:0.6.0", spec.getPostman().getImage().getCoordinates());
      Assertions.assertEquals(1, spec.getMicrocks().getReplicas());
   }
}
