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

import io.github.microcks.operator.api.model.SecretReferenceSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of an Amazon Service connection config of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "region", "credentialsType", "credentialsProfile", "credentialsSecretRef" })
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class AmazonServiceConnectionSpec {

   @JsonPropertyDescription("The Amazon region to use for connecting to this service")
   private String region;

   @JsonPropertyDescription("The type of credentials provider to use for connecting. Either env-variable or profile.")
   private AmazonCredentialsProviderType credentialsType;

   @JsonPropertyDescription("The name of profile to use for connecting when using profile credentials type")
   private String credentialsProfile;

   @JsonPropertyDescription("A secret reference holding the credentials information for the chosen type")
   private SecretReferenceSpec credentialsSecretRef;

   public String getRegion() {
      return region;
   }

   public void setRegion(String region) {
      this.region = region;
   }

   public AmazonCredentialsProviderType getCredentialsType() {
      return credentialsType;
   }

   public void setCredentialsType(AmazonCredentialsProviderType credentialsType) {
      this.credentialsType = credentialsType;
   }

   public String getCredentialsProfile() {
      return credentialsProfile;
   }

   public void setCredentialsProfile(String credentialsProfile) {
      this.credentialsProfile = credentialsProfile;
   }

   public SecretReferenceSpec getCredentialsSecretRef() {
      return credentialsSecretRef;
   }

   public void setCredentialsSecretRef(SecretReferenceSpec credentialsSecretRef) {
      this.credentialsSecretRef = credentialsSecretRef;
   }

   /** Different types of Amazon credentials provider. */
   public enum AmazonCredentialsProviderType {
      PROFILE("profile"),
      ENV_VARIABLE("env-variable");

      private String value;

      AmazonCredentialsProviderType(final String value) {
         this.value = value;
      }

      @JsonValue
      public String getValue() {
         return value;
      }

      @Override
      public String toString() {
         return this.getValue();
      }
   }
}
