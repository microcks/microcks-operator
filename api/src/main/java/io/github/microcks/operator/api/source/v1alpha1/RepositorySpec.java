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

/**
 * Representation of an ArtifactSpec part of an operator-managed APISource definition.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "url", "disableSSLValidation", "secretRef" })
public class RepositorySpec {

   @JsonPropertyDescription("The URL to access this repository artifact definition")
   private String url;

   @JsonPropertyDescription("Whether to disable SSL validation when accessing the repository. Defaults to false")
   private boolean disableSSLValidation = false;

   @JsonPropertyDescription("Reference to a Secret to reference in the importer")
   private String secretRef;

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public boolean getDisableSSLValidation() {
      return disableSSLValidation;
   }

   public void setDisableSSLValidation(boolean disableSSLValidation) {
      this.disableSSLValidation = disableSSLValidation;
   }

   public String getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(String secretRef) {
      this.secretRef = secretRef;
   }
}
