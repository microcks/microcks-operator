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
package io.github.microcks.operator.api;

import io.github.microcks.operator.api.model.AdditionalPropertyPreserving;
import io.github.microcks.operator.api.model.Condition;
import io.github.microcks.operator.api.model.Status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This the {@code status} of a {@link Microcks} custom resource.
 * @author laurent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "status", "message", "microcksUrl", "keycloakUrl",
      "observedGeneration", "conditions"})
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class MicrocksStatus implements AdditionalPropertyPreserving {

   @JsonPropertyDescription("Global status of the reconciliation")
   private Status status = Status.UNKNOWN;

   private String message;

   @JsonPropertyDescription("The URL to access Microcks once installation completed")
   private String microcksUrl;

   @JsonPropertyDescription("The URL to access Keycloak once installation completed")
   private String keycloakUrl;

   @JsonPropertyDescription("Reconcilied generation")
   private long observedGeneration;

   @JsonPropertyDescription("List of status conditions")
   private List<Condition> conditions;

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getMicrocksUrl() {
      return microcksUrl;
   }

   public void setMicrocksUrl(String microcksUrl) {
      this.microcksUrl = microcksUrl;
   }

   public String getKeycloakUrl() {
      return keycloakUrl;
   }

   public void setKeycloakUrl(String keycloakUrl) {
      this.keycloakUrl = keycloakUrl;
   }

   public long getObservedGeneration() {
      return observedGeneration;
   }

   public void setObservedGeneration(long observedGeneration) {
      this.observedGeneration = observedGeneration;
   }

   public List<Condition> getConditions() {
      return conditions;
   }

   public void setConditions(List<Condition> conditions) {
      this.conditions = conditions;
   }

   private List<Condition> prepareConditionsUpdate() {
      List<Condition> oldConditions = getConditions();
      List<Condition> newConditions = oldConditions != null ? new ArrayList<>(oldConditions) : new ArrayList<>(0);
      return newConditions;
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

   private Map<String, Object> additionalProperties;

   @Override
   public Map<String, Object> getAdditionalProperties() {
      return additionalProperties;
   }

   @Override
   public void setAdditionalProperty(String name, Object value) {
      if (this.additionalProperties == null) {
         this.additionalProperties = new HashMap<>(1);
      }
      this.additionalProperties.put(name, value);
   }
}
