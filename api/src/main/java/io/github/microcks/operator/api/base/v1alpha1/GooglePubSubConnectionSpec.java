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
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the Google PubSub connection config of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class GooglePubSubConnectionSpec {

   @JsonPropertyDescription("The Google Cloud Platform (GCP) project to use for connecting to PubSub broker")
   private String project;

   @JsonPropertyDescription("A Secret reference holding the GCP serviceAccount authentication token JSON file")
   private SecretReferenceSpec serviceAccountSecretRef;

   public String getProject() {
      return project;
   }

   public void setProject(String project) {
      this.project = project;
   }

   public SecretReferenceSpec getServiceAccountSecretRef() {
      return serviceAccountSecretRef;
   }

   public void setServiceAccountSecretRef(SecretReferenceSpec serviceAccountSecretRef) {
      this.serviceAccountSecretRef = serviceAccountSecretRef;
   }
}
