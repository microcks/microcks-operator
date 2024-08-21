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
package io.github.microcks.operator.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a status that can be detailed with multiple conditions.
 * @author laurent
 */
public abstract class MultiConditionsStatus {

   @JsonPropertyDescription("List of status conditions")
   private List<Condition> conditions;

   public List<Condition> getConditions() {
      return conditions;
   }

   public void setConditions(List<Condition> conditions) {
      this.conditions = conditions;
   }

   private List<Condition> prepareConditionsUpdate() {
      List<Condition> oldConditions = getConditions();
      return oldConditions != null ? new ArrayList<>(oldConditions) : new ArrayList<>(0);
   }

   public void addCondition(Condition condition) {
      List<Condition> newConditions = prepareConditionsUpdate();
      newConditions.add(condition);
      setConditions(Collections.unmodifiableList(newConditions));
   }

   public void addConditions(Collection<Condition> conditions) {
      List<Condition> newConditions = prepareConditionsUpdate();
      newConditions.addAll(conditions);
      setConditions(Collections.unmodifiableList(newConditions));
   }
}
