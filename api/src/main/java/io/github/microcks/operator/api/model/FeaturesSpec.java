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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the Features part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "async", "repositoryFilter", "repositoryTenancy", "microcksHub" })
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class FeaturesSpec {

   @JsonPropertyDescription("Configuration for asynchronous features")
   private AsyncFeatureSpec async;

   @JsonPropertyDescription("Configuration for repository filtering features")
   private RepositoryFilterSpec repositoryFilter;

   @JsonPropertyDescription("Configuration for repository tenancy features")
   private RepositoryTenancySpec repositoryTenancy;

   @JsonPropertyDescription("Configuration for Microcks Hub related features")
   private MicrocksHubSpec microcksHub;


   public AsyncFeatureSpec getAsync() {
      return async;
   }

   public void setAsync(AsyncFeatureSpec async) {
      this.async = async;
   }

   public RepositoryFilterSpec getRepositoryFilter() {
      return repositoryFilter;
   }

   public void setRepositoryFilter(RepositoryFilterSpec repositoryFilter) {
      this.repositoryFilter = repositoryFilter;
   }

   public RepositoryTenancySpec getRepositoryTenancy() {
      return repositoryTenancy;
   }

   public void setRepositoryTenancy(RepositoryTenancySpec repositoryTenancy) {
      this.repositoryTenancy = repositoryTenancy;
   }

   public MicrocksHubSpec getMicrocksHub() {
      return microcksHub;
   }

   public void setMicrocksHub(MicrocksHubSpec microcksHub) {
      this.microcksHub = microcksHub;
   }
}
