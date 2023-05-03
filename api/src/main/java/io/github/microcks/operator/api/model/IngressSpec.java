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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

import java.util.Map;

/**
 * Representation of a Kubernetes Ingress configuration of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class IngressSpec {

   @JsonPropertyDescription("Whether operator should generate self-signed certificate for this ingress")
   private boolean generateCert = true;

   @JsonPropertyDescription("Existing Secret holding TLS key and certificate name")
   private String secretRef;

   @JsonPropertyDescription("Annotations to apply to Ingress if created")
   private Map<String, String> annotations;

   public IngressSpec() {
   }
   public boolean isGenerateCert() {
      return generateCert;
   }

   public void setGenerateCert(boolean generateCert) {
      this.generateCert = generateCert;
   }

   public String getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(String secretRef) {
      this.secretRef = secretRef;
   }

   public Map<String, String> getAnnotations() {
      return annotations;
   }

   public void setAnnotations(Map<String, String> annotations) {
      this.annotations = annotations;
   }
}
