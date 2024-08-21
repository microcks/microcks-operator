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
package io.github.microcks.operator.api.source.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * Representation of an ImporterSpec part of an operator-managed APISource definition.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "name", "repository", "mainArtifact" })
public class ImporterSpec {

   @JsonPropertyDescription("Name of importer to create")
   private String name;

   @JsonPropertyDescription("Reference to a Repository to import from")
   private RepositorySpec repository;

   @JsonPropertyDescription("Define the imported artifact as main/primary one. Defaults to true")
   private boolean mainArtifact = true;

   @JsonPropertyDescription("Define if this importer is active or not. Defaults to true")
   private boolean active = true;

   @JsonPropertyDescription("Labels to put on created importer")
   private Map<String, String> labels;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public RepositorySpec getRepository() {
      return repository;
   }

   public void setRepository(RepositorySpec repository) {
      this.repository = repository;
   }

   public boolean getMainArtifact() {
      return mainArtifact;
   }

   public void setMainArtifact(boolean mainArtifact) {
      this.mainArtifact = mainArtifact;
   }

   public boolean getActive() {
      return active;
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   public Map<String, String> getLabels() {
      return labels;
   }

   public void setLabels(Map<String, String> labels) {
      this.labels = labels;
   }
}
