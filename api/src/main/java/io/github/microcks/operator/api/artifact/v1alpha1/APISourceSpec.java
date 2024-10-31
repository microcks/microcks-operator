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
package io.github.microcks.operator.api.artifact.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

import java.util.List;

/**
 * This the {@code specification} of a {@link APISource} custom resource.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "artifacts", "importers" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class APISourceSpec {

   @JsonPropertyDescription("Flag to keep API when deleting an artifact. Default is false")
   private boolean keepAPIOnDelete = false;

   @JsonPropertyDescription("A list of Artifacts to import into Microcks instance")
   private List<ArtifactSpec> artifacts;

   @JsonPropertyDescription("A list of Importers to create in Microcks instance")
   private List<ImporterSpec> importers;

   public boolean isKeepAPIOnDelete() {
      return keepAPIOnDelete;
   }

   public void setKeepAPIOnDelete(boolean keepAPIOnDelete) {
      this.keepAPIOnDelete = keepAPIOnDelete;
   }

   public List<ArtifactSpec> getArtifacts() {
      return artifacts;
   }

   public void setArtifacts(List<ArtifactSpec> artifacts) {
      this.artifacts = artifacts;
   }

   public List<ImporterSpec> getImporters() {
      return importers;
   }

   public void setImporters(List<ImporterSpec> importers) {
      this.importers = importers;
   }
}
