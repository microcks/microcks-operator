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

import io.github.microcks.operator.api.model.Condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * This is a unit test for the {@link ConditionUtil} util class.
 * @author laurent
 */
class ConditionUtilTest {

   @Test
   void testTouchConditionTime() throws InterruptedException {
      Condition condition = new Condition();
      assertNull(condition.getLastTransitionTime());

      // Touch a first time.
      ConditionUtil.touchConditionTime(condition);
      String firstTransitionTime = condition.getLastTransitionTime();

      // Wait a second and touch a second time.
      Object semaphore = new Object();
      synchronized (semaphore) {
         semaphore.wait(1000);
      }
      ConditionUtil.touchConditionTime(condition);
      String secondTransitionTime = condition.getLastTransitionTime();
      assertNotEquals(secondTransitionTime, firstTransitionTime);
   }
}
