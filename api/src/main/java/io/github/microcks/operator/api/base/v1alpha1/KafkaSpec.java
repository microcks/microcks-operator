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
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "install", "url", "ingressClassName", "persistent", "volumeSize", "schemaRegistry",
      "authentication", "resources", "zkResources" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
/**
 * Representation of the Kafka async features config of an operator-managed Microcks installation.
 * @author laurent
 */
public class KafkaSpec {

   @JsonPropertyDescription("Install Kafka broker or reuse an existing instance? Default to true")
   private boolean install = true;

   @JsonPropertyDescription("The URL to use for either exposing embedded Kafka broker or connecting external broker")
   private String url;

   @JsonPropertyDescription("When exposed via ingress, sets the ingressClassName property in the Ingress resources")
   private String ingressClassName;

   @JsonPropertyDescription("Use persistent storage or ephemeral one? Default to false")
   private boolean persistent = false;

   @JsonPropertyDescription("Size of persistent storage volume if persistent")
   private String volumeSize;

   @JsonPropertyDescription("Specification of Schema registry connection")
   private SchemaRegistrySpec schemaRegistry;

   @JsonPropertyDescription("Specification of authentication method when using external Kafka")
   private KafkaAuthenticationSpec authentication;

   @JsonPropertyDescription("Kubernetes resource requirements for Kafka broker")
   private ResourceRequirements resources;

   @JsonPropertyDescription("Kubernetes resource requirements for Zookeeper ensemble")
   private ResourceRequirements zkResources;

   public boolean isInstall() {
      return install;
   }

   public void setInstall(boolean install) {
      this.install = install;
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

   public String getIngressClassName() {
      return ingressClassName;
   }

   public void setIngressClassName(String ingressClassName) {
      this.ingressClassName = ingressClassName;
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

   public SchemaRegistrySpec getSchemaRegistry() {
      return schemaRegistry;
   }

   public void setSchemaRegistry(SchemaRegistrySpec schemaRegistry) {
      this.schemaRegistry = schemaRegistry;
   }

   public KafkaAuthenticationSpec getAuthentication() {
      return authentication;
   }

   public void setAuthentication(KafkaAuthenticationSpec authentication) {
      this.authentication = authentication;
   }

   public ResourceRequirements getResources() {
      return resources;
   }

   public void setResources(ResourceRequirements resources) {
      this.resources = resources;
   }

   public ResourceRequirements getZkResources() {
      return zkResources;
   }

   public void setZkResources(ResourceRequirements zkResources) {
      this.zkResources = zkResources;
   }
}
