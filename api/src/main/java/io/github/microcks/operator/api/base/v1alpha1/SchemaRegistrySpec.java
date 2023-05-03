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
package io.github.microcks.operator.api.base.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "url", "confluent", "username", "credentialsSource" })
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
/**
 * Representation of the Schema registry config of an operator-managed Microcks installation.
 * @author laurent
 */
public class SchemaRegistrySpec {

   @JsonProperty("url")
   @JsonPropertyDescription("The URL for accessing external Schema Registry")
   private String url;

   @JsonPropertyDescription("Whether you should use the Confluent compatibility API layer. Default to true.")
   private boolean confluentCompatibility = true;

   @JsonPropertyDescription("The username for authenticating the connection to Schema Registry")
   private String username;

   @JsonPropertyDescription("The source of credentials for authenticating the connection to Schema Registry. Defaults to USER_INFO.")
   private String credentialsSource;

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public boolean isConfluentCompatibility() {
      return confluentCompatibility;
   }

   public void setConfluentCompatibility(boolean confluentCompatibility) {
      this.confluentCompatibility = confluentCompatibility;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getCredentialsSource() {
      return credentialsSource;
   }

   public void setCredentialsSource(String credentialsSource) {
      this.credentialsSource = credentialsSource;
   }
}
