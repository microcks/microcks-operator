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

import io.github.microcks.operator.api.model.GatewayRouteSpec;
import io.github.microcks.operator.api.model.ImageSpec;
import io.github.microcks.operator.api.model.IngressSpec;
import io.github.microcks.operator.api.model.OpenShiftSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

import java.util.Map;

/**
 * Representation of the Keycloak part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "enabled", "install", "realm", "image", "url", "privateUrl", "ingress", "gatewayRoute", "resources",
      "persistent", "volumeSize", "storageClassName", "pvcAnnotations", "serviceAccount", "serviceAccountCredentials", "openshift" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class KeycloakSpec {

   @JsonPropertyDescription("Enable Keycloak for authentication. Default to true.")
   private boolean enabled = true;

   @JsonPropertyDescription("Install Keycloak or reuse an existing instance? Default to true.")
   private boolean install = true;

   @JsonPropertyDescription("Keycloak realm to use for authentication on Microcks")
   private String realm;

   @JsonPropertyDescription("The container image to use for Keycloak")
   private ImageSpec image;

   @JsonProperty("url")
   @JsonPropertyDescription("Keycloak root URL to use for access and Ingress if created")
   private String url;

   @JsonProperty("privateUrl")
   @JsonPropertyDescription("A private URL - a full URL here - used by the Microcks component to internally join Keycloak. This is also known as `backendUrl` in Keycloak. When specified, the `keycloak.url` is used as `frontendUrl` in Keycloak terms.")
   private String privateUrl;

   @JsonPropertyDescription("Configuration to apply to Ingress if created")
   private IngressSpec ingress;

   @JsonPropertyDescription("Configuration to apply to HTTPRoute if created")
   private GatewayRouteSpec gatewayRoute;

   @JsonPropertyDescription("Kubernetes resource requirements for Keycloak service")
   private ResourceRequirements resources;

   @JsonPropertyDescription("Use persistent storage or ephemeral one? Default to true.")
   private boolean persistent = true;

   @JsonPropertyDescription("The container image to use for PostgreSQL database")
   private ImageSpec postgresImage;

   @JsonPropertyDescription("Size of persistent storage volume if persistent")
   private String volumeSize;

   @JsonPropertyDescription("Name of storage class to use if not relying on default")
   private String storageClassName;

   @JsonPropertyDescription("Annotations to be added to managed Persistent Volume Claim")
   private Map<String, String> pvcAnnotations;

   @JsonPropertyDescription("Service Account for connecting external services to Microcks")
   private String serviceAccount;

   @JsonPropertyDescription("Service Account credentials for external services")
   private String serviceAccountCredentials;

   @JsonPropertyDescription("Flag to enable/disable the Service Account dedicated to the operator for connecting to Keycloak. Default to true.")
   private boolean operatorServiceAccountEnabled = true;

   @JsonPropertyDescription("Configuration of OpenShift specific settings")
   private OpenShiftSpec openshift;

   public KeycloakSpec() {
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
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

   public ImageSpec getImage() {
      return image;
   }

   public void setImage(ImageSpec image) {
      this.image = image;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public String getPrivateUrl() {
      return privateUrl;
   }

   public void setPrivateUrl(String privateUrl) {
      this.privateUrl = privateUrl;
   }

   public IngressSpec getIngress() {
      return ingress;
   }

   public void setIngress(IngressSpec ingress) {
      this.ingress = ingress;
   }

   public GatewayRouteSpec getGatewayRoute() {
      return gatewayRoute;
   }

   public void setGatewayRoute(GatewayRouteSpec gatewayRoute) {
      this.gatewayRoute = gatewayRoute;
   }

   public ResourceRequirements getResources() {
      return resources;
   }

   public void setResources(ResourceRequirements resources) {
      this.resources = resources;
   }

   public boolean isPersistent() {
      return persistent;
   }

   public void setPersistent(boolean persistent) {
      this.persistent = persistent;
   }

   public ImageSpec getPostgresImage() {
      return postgresImage;
   }

   public void setPostgresImage(ImageSpec postgresImage) {
      this.postgresImage = postgresImage;
   }

   public String getVolumeSize() {
      return volumeSize;
   }

   public void setVolumeSize(String volumeSize) {
      this.volumeSize = volumeSize;
   }

   public String getStorageClassName() {
      return storageClassName;
   }

   public void setStorageClassName(String storageClassName) {
      this.storageClassName = storageClassName;
   }

   public Map<String, String> getPvcAnnotations() {
      return pvcAnnotations;
   }

   public void setPvcAnnotations(Map<String, String> pvcAnnotations) {
      this.pvcAnnotations = pvcAnnotations;
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

   public boolean isOperatorServiceAccountEnabled() {
      return operatorServiceAccountEnabled;
   }

   public void setOperatorServiceAccountEnabled(boolean operatorServiceAccountEnabled) {
      this.operatorServiceAccountEnabled = operatorServiceAccountEnabled;
   }

   public OpenShiftSpec getOpenshift() {
      return openshift;
   }

   public void setOpenshift(OpenShiftSpec openshift) {
      this.openshift = openshift;
   }
}
