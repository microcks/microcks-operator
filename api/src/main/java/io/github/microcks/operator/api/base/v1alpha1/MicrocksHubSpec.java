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
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the Microcks Hub config of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class MicrocksHubSpec {

   @JsonPropertyDescription("Enable/disable this feature. Default is false.")
   private boolean enabled = false;

   private String allowedRoles;

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public String getAllowedRoles() {
      return allowedRoles;
   }

   public void setAllowedRoles(String allowedRoles) {
      this.allowedRoles = allowedRoles;
   }
}
