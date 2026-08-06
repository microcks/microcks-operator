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

import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.base.MicrocksReconciler;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A Unit test for {@link MicrocksSpecHelper}.
 * @author laurent
 */
class MicrocksSpecHelperTest {

   @Test
   void testMinorVersionTriggersAsyncApiConditionFor1_14_And_1_15() throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());

      MicrocksSpec spec14 = reconciler.loadDefaultMicrocksSpec("1.14.0");
      spec14.setVersion("1.14.0");
      Assertions.assertTrue(MicrocksSpecHelper.getMicrocksMinorVersion(spec14) >= 14);

      MicrocksSpec spec15 = reconciler.loadDefaultMicrocksSpec("1.15.0");
      spec15.setVersion("1.15.0");
      Assertions.assertTrue(MicrocksSpecHelper.getMicrocksMinorVersion(spec15) >= 14);
   }
}
