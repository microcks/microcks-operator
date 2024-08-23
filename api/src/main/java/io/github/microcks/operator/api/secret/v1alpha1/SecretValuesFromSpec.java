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

/**
 * Representation of a SecretValuesFromSpec part of an operator-managed SecretSpec definition.
 * This has to be used instead of singular values for username, password, token, tokenHeader and caCertPem in order
 * to sync a secret content from Kubernetes to Microcks.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "secretRef", "usernameKey", "passwordKey", "tokenKey", "tokenHeaderKey", "caCertPermKey" })
public class SecretValuesFromSpec {

   @JsonPropertyDescription("The name of a namespace Kubernetes Secret to import values from")
   private String secretRef;

   @JsonPropertyDescription("The key in referenced Secret to import username value from")
   private String usernameKey;

   @JsonPropertyDescription("The key in referenced Secret to import password value from")
   private String passwordKey;

   @JsonPropertyDescription("The key in referenced Secret to import token value from")
   private String tokenKey;

   @JsonPropertyDescription("The key in referenced Secret to import token header value from")
   private String tokenHeaderKey;

   @JsonPropertyDescription("The key in referenced Secret to import CA certificate in PEM format value from")
   private String caCertPermKey;


   public String getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(String secretRef) {
      this.secretRef = secretRef;
   }

   public String getUsernameKey() {
      return usernameKey;
   }

   public void setUsernameKey(String usernameKey) {
      this.usernameKey = usernameKey;
   }

   public String getPasswordKey() {
      return passwordKey;
   }

   public void setPasswordKey(String passwordKey) {
      this.passwordKey = passwordKey;
   }

   public String getTokenKey() {
      return tokenKey;
   }

   public void setTokenKey(String tokenKey) {
      this.tokenKey = tokenKey;
   }

   public String getTokenHeaderKey() {
      return tokenHeaderKey;
   }

   public void setTokenHeaderKey(String tokenHeaderKey) {
      this.tokenHeaderKey = tokenHeaderKey;
   }

   public String getCaCertPermKey() {
      return caCertPermKey;
   }

   public void setCaCertPermKey(String caCertPermKey) {
      this.caCertPermKey = caCertPermKey;
   }
}
