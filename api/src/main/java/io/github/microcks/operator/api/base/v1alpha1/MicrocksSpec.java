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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * This the {@code specification} of a {@link Microcks} custom resource.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "version", "microcks", "postman", "keycloak", "mongodb", "features" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class MicrocksSpec {

   @JsonPropertyDescription("The Microcks version")
   private String version;

   @JsonPropertyDescription("Configuration of main Microcks service")
   private MicrocksServiceSpec microcks;

   @JsonPropertyDescription("Configuration of Postman runtime")
   private PostmanRuntimeSpec postman;

   @JsonPropertyDescription("Configuration of Keycloak access and/or install")
   private KeycloakSpec keycloak;

   @JsonProperty("mongodb")
   @JsonPropertyDescription("Configuration of MongoDB access and/or install")
   private MongoDBSpec mongodb;

   @JsonPropertyDescription("Configuration of optional features in Microcks")
   private FeaturesSpec features;

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public MicrocksServiceSpec getMicrocks() {
      return microcks;
   }

   public void setMicrocks(MicrocksServiceSpec microcks) {
      this.microcks = microcks;
   }

   public PostmanRuntimeSpec getPostman() {
      return postman;
   }

   public void setPostman(PostmanRuntimeSpec postman) {
      this.postman = postman;
   }

   public KeycloakSpec getKeycloak() {
      return keycloak;
   }

   public void setKeycloak(KeycloakSpec keycloak) {
      this.keycloak = keycloak;
   }

   public MongoDBSpec getMongoDB() {
      return mongodb;
   }

   public void setMongoDB(MongoDBSpec mongodb) {
      this.mongodb = mongodb;
   }

   public FeaturesSpec getFeatures() {
      return features;
   }

   public void setFeatures(FeaturesSpec features) {
      this.features = features;
   }
}
