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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a generic status condition as usually described in https://maelvls.dev/kubernetes-conditions/.
 * @author laurent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "status", "lastTransitionTime", "reason", "message" })
public class Condition implements AdditionalPropertyPreserving, Serializable {

   private static final long serialVersionUID = 1L;

   @JsonPropertyDescription("The status of the condition")
   private Status status;

   @JsonPropertyDescription("The reason for the condition's last transition (a single word in CamelCase)")
   private String reason;

   @JsonPropertyDescription("Human-readable message indicating details about the condition's last transition")
   private String message;

   @JsonPropertyDescription("The unique identifier of a condition, used to distinguish between other conditions in the resource")
   private String type;

   @JsonPropertyDescription("Last time the condition of a type changed from one status to another. "
         + "The required format is 'yyyy-MM-ddTHH:mm:ssZ', in the UTC time zone")
   private String lastTransitionTime;

   private Map<String, Object> additionalProperties;

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public String getReason() {
      return reason;
   }

   public void setReason(String reason) {
      this.reason = reason;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getLastTransitionTime() {
      return lastTransitionTime;
   }

   public void setLastTransitionTime(String lastTransitionTime) {
      this.lastTransitionTime = lastTransitionTime;
   }

   public void setAdditionalProperties(Map<String, Object> additionalProperties) {
      this.additionalProperties = additionalProperties;
   }

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
