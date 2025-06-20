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
package io.github.microcks.operator.api.test.v1alpha1;

import io.github.microcks.operator.api.model.SecretReferenceSpec;
import io.github.microcks.operator.api.model.ValueFromSecretSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "clientId", "clientSecret", "tokenUri", "scopes", "username", "password", "refreshToken" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class OAuth2ClientContext {

   @JsonPropertyDescription("The OAuth2 client ID")
   private String clientId;

   @JsonPropertyDescription("The OAuth2 client Secret")
   private String clientSecret;

   @JsonPropertyDescription("A reference to a namespace Kubernetes Secret to get the OAuth2 client ID value from")
   private ValueFromSecretSpec clientIdFrom;

   @JsonPropertyDescription("A reference to a namespace Kubernetes Secret to get the OAuth2 client Secret value from")
   private ValueFromSecretSpec clientSecretFrom;

   @JsonPropertyDescription("The IDP URI where to get the OAuth2 token")
   private String tokenUri;

   @JsonPropertyDescription("Optional. The OAuth2 scopes to use for negotiating the token")
   private String scopes;

   @JsonPropertyDescription("Optional. The username to use for the OAuth2 client")
   private String username;

   @JsonPropertyDescription("Optional. The password to use for the OAuth2 client")
   private String password;

   @JsonPropertyDescription("Optional. A reference to a a namespace Kubernetes Secret to get the username to use for the OAuth2 client")
   private ValueFromSecretSpec usernameFrom;

   @JsonPropertyDescription("Optional. A reference to a a namespace Kubernetes Secret to get the password to use for the OAuth2 client")
   private ValueFromSecretSpec passwordFrom;

   @JsonPropertyDescription("Optional. The refreshToken to use for obtaining a new OAuth2 token")
   private String refreshToken;

   @JsonPropertyDescription("Optional. A reference to a a namespace Kubernetes Secret to get the refreshToken to use for obtaining a new OAuth2 token")
   private ValueFromSecretSpec refreshTokenFrom;

   @JsonPropertyDescription("The OAuth Grant Type flow to apply for getting the token")
   private String grantType;

   public String getClientId() {
      return clientId;
   }

   public void setClientId(String clientId) {
      this.clientId = clientId;
   }

   public String getClientSecret() {
      return clientSecret;
   }

   public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
   }

   public ValueFromSecretSpec getClientIdFrom() {
      return clientIdFrom;
   }

   public void setClientIdFrom(ValueFromSecretSpec clientIdFrom) {
      this.clientIdFrom = clientIdFrom;
   }

   public ValueFromSecretSpec getClientSecretFrom() {
      return clientSecretFrom;
   }

   public void setClientSecretFrom(ValueFromSecretSpec clientSecretFrom) {
      this.clientSecretFrom = clientSecretFrom;
   }

   public String getTokenUri() {
      return tokenUri;
   }

   public void setTokenUri(String tokenUri) {
      this.tokenUri = tokenUri;
   }

   public String getScopes() {
      return scopes;
   }

   public void setScopes(String scopes) {
      this.scopes = scopes;
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

   public ValueFromSecretSpec getUsernameFrom() {
      return usernameFrom;
   }

   public void setUsernameFrom(ValueFromSecretSpec usernameFrom) {
      this.usernameFrom = usernameFrom;
   }

   public ValueFromSecretSpec getPasswordFrom() {
      return passwordFrom;
   }

   public void setPasswordFrom(ValueFromSecretSpec passwordFrom) {
      this.passwordFrom = passwordFrom;
   }

   public String getRefreshToken() {
      return refreshToken;
   }

   public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
   }

   public ValueFromSecretSpec getRefreshTokenFrom() {
      return refreshTokenFrom;
   }

   public void setRefreshTokenFrom(ValueFromSecretSpec refreshTokenFrom) {
      this.refreshTokenFrom = refreshTokenFrom;
   }

   public String getGrantType() {
      return grantType;
   }

   public void setGrantType(String grantType) {
      this.grantType = grantType;
   }
}
