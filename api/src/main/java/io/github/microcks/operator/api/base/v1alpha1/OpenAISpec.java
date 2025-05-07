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
package io.github.microcks.operator.api.base.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the OpenAI access for AICopilot features config of an operator-managed
 * Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "apiKey", "timeout", "model", "maxTokens" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class OpenAISpec {

   @JsonPropertyDescription("The OpenAI API key to use")
   private String apiKey;

   @JsonPropertyDescription("The timeout for OpenAI API calls")
   private int timeout;

   @JsonPropertyDescription("The model to use for OpenAI API calls")
   private String model;

   @JsonPropertyDescription("The maximum number of tokens to generate/transfer")
   private int maxTokens;

   @JsonPropertyDescription("The OpenAI API URL to use")
   private String apiUrl; 

   public String getApiKey() {
      return apiKey;
   }

   public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
   }

   public int getTimeout() {
      return timeout;
   }

   public void setTimeout(int timeout) {
      this.timeout = timeout;
   }

   public String getModel() {
      return model;
   }

   public void setModel(String model) {
      this.model = model;
   }

   public int getMaxTokens() {
      return maxTokens;
   }

   public void setMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
   }

   public String getApiUrl(){
      return apiUrl;
   }

   public void setApiUrl(String apiUrl){
      this.apiUrl = apiUrl;
   }
}
