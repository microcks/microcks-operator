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
 * Representation of the Repository filtering config of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "enabled", "labelKey", "labelLabel", "labelList" })
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class RepositoryFilterSpec {

   @JsonPropertyDescription("Enable/disable this feature. Default is false")
   private boolean enabled = false;

   @JsonPropertyDescription("The label key to consider for repository filtering")
   private String labelKey;

   @JsonPropertyDescription("The label of label used for repository filtering")
   private String labelLabel;

   @JsonPropertyDescription("The list of labels (separated by ,) to display on services list")
   private String labelList;

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public String getLabelKey() {
      return labelKey;
   }

   public void setLabelKey(String labelKey) {
      this.labelKey = labelKey;
   }

   public String getLabelLabel() {
      return labelLabel;
   }

   public void setLabelLabel(String labelLabel) {
      this.labelLabel = labelLabel;
   }

   public String getLabelList() {
      return labelList;
   }

   public void setLabelList(String labelList) {
      this.labelList = labelList;
   }
}
