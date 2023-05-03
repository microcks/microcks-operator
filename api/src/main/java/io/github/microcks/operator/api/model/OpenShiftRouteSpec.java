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

/**
 * Representation of the OpenShift route part of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class OpenShiftRouteSpec {

   @JsonPropertyDescription("Whether operator should use Route API if on OpenShift")
   private boolean enabled = true;

   @JsonPropertyDescription("Type of OpenShift route to create ('edge', 'passthrough', 'reencrypt'")
   private String tlsTermination;

   @JsonPropertyDescription("TLS private key for reencrypt termination")
   private String tlsKey;

   @JsonPropertyDescription("TLS certificate for reencrypt termination")
   private String tlsCertificate;

   @JsonPropertyDescription("TLS CA certificate for reencrypt termination")
   private String tlsCaCertificate;

   @JsonPropertyDescription("TLS destination CA certificate for reencrypt termination")
   private String tlsDestinationCaCertificate;

   @JsonPropertyDescription("TLS insecure edge termination policy")
   private String tlsInsecureEdgeTerminationPolicy;

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public String getTlsTermination() {
      return tlsTermination;
   }

   public void setTlsTermination(String tlsTermination) {
      this.tlsTermination = tlsTermination;
   }

   public String getTlsKey() {
      return tlsKey;
   }

   public void setTlsKey(String tlsKey) {
      this.tlsKey = tlsKey;
   }

   public String getTlsCertificate() {
      return tlsCertificate;
   }

   public void setTlsCertificate(String tlsCertificate) {
      this.tlsCertificate = tlsCertificate;
   }

   public String getTlsCaCertificate() {
      return tlsCaCertificate;
   }

   public void setTlsCaCertificate(String tlsCaCertificate) {
      this.tlsCaCertificate = tlsCaCertificate;
   }

   public String getTlsDestinationCaCertificate() {
      return tlsDestinationCaCertificate;
   }

   public void setTlsDestinationCaCertificate(String tlsDestinationCaCertificate) {
      this.tlsDestinationCaCertificate = tlsDestinationCaCertificate;
   }

   public String getTlsInsecureEdgeTerminationPolicy() {
      return tlsInsecureEdgeTerminationPolicy;
   }

   public void setTlsInsecureEdgeTerminationPolicy(String tlsInsecureEdgeTerminationPolicy) {
      this.tlsInsecureEdgeTerminationPolicy = tlsInsecureEdgeTerminationPolicy;
   }
}
