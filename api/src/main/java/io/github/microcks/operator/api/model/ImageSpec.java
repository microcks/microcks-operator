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
package io.github.microcks.operator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of a container image coordinates of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true, value = { "coordinates" })
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "registry", "repository", "tag", "digest" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class ImageSpec {

   @JsonPropertyDescription("The registry where to pull this image (docker.io, quay.io, internal one...)")
   private String registry;

   @JsonPropertyDescription("The repository name of the image")
   private String repository;

   @JsonPropertyDescription("The tag of the image")
   private String tag;

   @JsonPropertyDescription("The digest of the image")
   private String digest;

   /** Default constructor. */
   public ImageSpec() {
   }

   /** Get image coordinates as a string. */
   public String getCoordinates() {
      String image = String.join("/", registry, repository);
      return image + ":" + (digest != null && !digest.isBlank() ? "@" + digest : tag);
   }

   public String getRegistry() {
      return registry;
   }

   public void setRegistry(String registry) {
      this.registry = registry;
   }

   public String getRepository() {
      return repository;
   }

   public void setRepository(String repository) {
      this.repository = repository;
   }

   public String getTag() {
      return tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

   public String getDigest() {
      return digest;
   }

   public void setDigest(String digest) {
      this.digest = digest;
   }

   @Override
   public String toString() {
      return getCoordinates();
   }
}
