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
import io.github.microcks.operator.api.model.MultiConditionsStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for working with conditions.
 * @author laurent
 */
public class ConditionUtil {

   private static final DateTimeFormatter TRANSITION_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

   private ConditionUtil() {
      // Private constructor that hides the default implicit one.
   }

   /**
    *
    * @param status
    * @param type
    * @return
    */
   public static Condition getCondition(MultiConditionsStatus status, String type) {
      if (status.getConditions() != null) {
         for (Condition condition : status.getConditions()) {
            if (condition.getType().equals(type)) {
               return condition;
            }
         }
      }
      return null;
   }

   /**
    *
    * @param status
    * @param type
    * @return
    */
   public static Condition getOrCreateCondition(MultiConditionsStatus status, String type) {
      Condition condition = getCondition(status, type);
      if (condition == null) {
         condition = new Condition();
         condition.setType(type);
         status.addCondition(condition);
      }
      return condition;
   }

   /**
    *
    * @param condition
    */
   public static void touchConditionTime(Condition condition) {
      condition.setLastTransitionTime(TRANSITION_FORMATTER.format(ZonedDateTime.now(ZoneId.of("UTC"))));
   }
}
