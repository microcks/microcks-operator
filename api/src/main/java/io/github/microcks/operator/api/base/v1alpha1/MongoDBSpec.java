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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the MongoDB part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class MongoDBSpec {

   @JsonPropertyDescription("Install MongoDB or reuse an existing instance? Default to true.")
   private boolean install = true;

   @JsonProperty("uri")
   @JsonPropertyDescription("MongoDB root URI to use for access")
   private String uri;

   @JsonPropertyDescription("Parameters added to MongoDB URI connection string. Only if install if false.")
   private String uriParameters;

   @JsonPropertyDescription("MongoDB database name in case you're reusing existing one. Only if install if false.")
   private String database;

   @JsonPropertyDescription("Reference of a Secret containing username and password keys for connecting to existing MongoDB instance")
   private SecretReferenceSpec secretRef;

   @JsonPropertyDescription("Kubernetes resource requirements for MongoDB")
   private ResourceRequirements resources;

   @JsonPropertyDescription("Use persistent storage or ephemeral one? Default to true")
   private boolean persistent = true;

   @JsonPropertyDescription("Size of persistent storage volume if persistent")
   private String volumeSize;

   @JsonPropertyDescription("Name of storage class to use if not relying on default")
   private String storageClassName;

   public boolean isInstall() {
      return install;
   }

   public void setInstall(boolean install) {
      this.install = install;
   }

   public String getUri() {
      return uri;
   }

   public void setUri(String uri) {
      this.uri = uri;
   }

   public String getUriParameters() {
      return uriParameters;
   }

   public void setUriParameters(String uriParameters) {
      this.uriParameters = uriParameters;
   }

   public String getDatabase() {
      return database;
   }

   public void setDatabase(String database) {
      this.database = database;
   }

   public SecretReferenceSpec getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(SecretReferenceSpec secretRef) {
      this.secretRef = secretRef;
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
}
