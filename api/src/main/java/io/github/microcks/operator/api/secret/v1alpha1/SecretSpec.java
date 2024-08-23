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
package io.github.microcks.operator.api.secret.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of a SecretSpec part of an operator-managed SecretSource definition.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "name", "description", "username", "password", "token", "tokenHeader", "caCertPerm", "valuesFrom" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class SecretSpec {

   @JsonPropertyDescription("The unique name of Secret to create in Microcks instance")
   private String name;

   @JsonPropertyDescription("The human-readable description of what this secret is for")
   private String description;

   @JsonPropertyDescription("The username information contained in this secret")
   private String username;

   @JsonPropertyDescription("The password information contained in this secret")
   private String password;

   @JsonPropertyDescription("The token information contained in this secret")
   private String token;

   @JsonPropertyDescription("The token header information contained in this secret")
   private String tokenHeader;

   @JsonPropertyDescription("The CA certificate in PEM format container in this secret")
   private String caCertPem;

   @JsonPropertyDescription("The specification of a Kubernetes secret to import values from")
   private SecretValuesFromSpec valuesFrom;


   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getToken() {
      return token;
   }

   public void setToken(String token) {
      this.token = token;
   }

   public String getTokenHeader() {
      return tokenHeader;
   }

   public void setTokenHeader(String tokenHeader) {
      this.tokenHeader = tokenHeader;
   }

   public String getCaCertPem() {
      return caCertPem;
   }

   public void setCaCertPem(String caCertPem) {
      this.caCertPem = caCertPem;
   }

   public SecretValuesFromSpec getValuesFrom() {
      return valuesFrom;
   }

   public void setValuesFrom(SecretValuesFromSpec valuesFrom) {
      this.valuesFrom = valuesFrom;
   }
}
