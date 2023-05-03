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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the Kafka authentication config of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "truststoreType", "truststoreSecretRef", "keystoreType",
      "keystoreSecretRef", "saslMechanism", "saslJaasConfig" })
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class KafkaAuthenticationSpec {

   @JsonPropertyDescription("Type of authentication method for connecting to external Kafka broker")
   private KafkaAuthenticationType type;

   @JsonPropertyDescription("Type of truststore where to find cluster certificate for TLS transport. PKCS12 and JKS are supported")
   private String truststoreType;

   @JsonPropertyDescription("Reference to Secret holding cluster certificate for TLS transport")
   private SecretReferenceSpec truststoreSecretRef;

   @JsonPropertyDescription("Type of keystore where to find private key when using MTLS. PKCS12 and JKS are supported")
   private String keystoreType;

   @JsonPropertyDescription("Reference to Secret holding client private key when using MTLS")
   private SecretReferenceSpec keystoreSecretRef;

   @JsonPropertyDescription("SASL mechanism used when using SASL_TLS authentication type")
   private String saslMechanism;

   @JsonPropertyDescription("Additional JAAS config for SASL_TLS authentication type")
   private String saslJaasConfig;

   public KafkaAuthenticationType getType() {
      return type;
   }

   public void setType(KafkaAuthenticationType type) {
      this.type = type;
   }

   public String getTruststoreType() {
      return truststoreType;
   }

   public void setTruststoreType(String truststoreType) {
      this.truststoreType = truststoreType;
   }

   public SecretReferenceSpec getTruststoreSecretRef() {
      return truststoreSecretRef;
   }

   public void setTruststoreSecretRef(SecretReferenceSpec truststoreSecretRef) {
      this.truststoreSecretRef = truststoreSecretRef;
   }

   public String getKeystoreType() {
      return keystoreType;
   }

   public void setKeystoreType(String keystoreType) {
      this.keystoreType = keystoreType;
   }

   public SecretReferenceSpec getKeystoreSecretRef() {
      return keystoreSecretRef;
   }

   public void setKeystoreSecretRef(SecretReferenceSpec keystoreSecretRef) {
      this.keystoreSecretRef = keystoreSecretRef;
   }

   public String getSaslMechanism() {
      return saslMechanism;
   }

   public void setSaslMechanism(String saslMechanism) {
      this.saslMechanism = saslMechanism;
   }

   public String getSaslJaasConfig() {
      return saslJaasConfig;
   }

   public void setSaslJaasConfig(String saslJaasConfig) {
      this.saslJaasConfig = saslJaasConfig;
   }
}
