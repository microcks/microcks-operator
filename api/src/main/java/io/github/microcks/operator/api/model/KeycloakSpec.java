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
package io.github.microcks.operator.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the Keycloak part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class KeycloakSpec {

   @JsonPropertyDescription("Install Keycloak or reuse an existing instance? Default to true.")
   private boolean install = true;

   @JsonPropertyDescription("Keycloak realm to use for authentication on Microcks")
   private String realm;

   @JsonPropertyDescription("Keycloak root URL to use for access and Ingress if created")
   private String url;

   @JsonPropertyDescription("Use persistent storage or ephemeral one? Default to true.")
   private boolean persistent = true;

   @JsonPropertyDescription("Size of persistent storage volume if persistent")
   private String volumeSize;

   @JsonPropertyDescription("Service Account for connecting external services to Microcks")
   private String serviceAccount;

   @JsonPropertyDescription("Service Account credentials for external services")
   private String serviceAccountCredentials;

   public KeycloakSpec() {
   }

   public boolean isInstall() {
      return install;
   }

   public void setInstall(boolean install) {
      this.install = install;
   }

   public String getRealm() {
      return realm;
   }

   public void setRealm(String realm) {
      this.realm = realm;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public boolean isPersistent() {
      return persistent;
   }

   public void setPersistent(boolean persistent) {
      this.persistent = persistent;
   }

   public String getVolumeSize() {
      return volumeSize;
   }

   public void setVolumeSize(String volumeSize) {
      this.volumeSize = volumeSize;
   }

   public String getServiceAccount() {
      return serviceAccount;
   }

   public void setServiceAccount(String serviceAccount) {
      this.serviceAccount = serviceAccount;
   }

   public String getServiceAccountCredentials() {
      return serviceAccountCredentials;
   }

   public void setServiceAccountCredentials(String serviceAccountCredentials) {
      this.serviceAccountCredentials = serviceAccountCredentials;
   }
}
