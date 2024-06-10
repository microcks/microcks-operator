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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of the Async features config of an operator-managed Microcks installation.
 * @author laurent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "enabled", "defaultBinding", "defaultFrequency", "defaultAvroEncoding", "kafka", "amqp", "mqtt",
      "nats", "googlepubsub" })
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class AsyncFeatureSpec {

   @JsonPropertyDescription("Enable/disable this feature. Default is false")
   private boolean enabled = false;

   @JsonPropertyDescription("Default protocol binding to use if no specified in AsyncAPI document")
   private String defaultBinding;

   @JsonPropertyDescription("Default frequency for publishing async mock messages")
   private int defaultFrequency;

   @JsonPropertyDescription("Default Avro encoding mode for publishing mock messages")
   private AvroEncoding defaultAvroEncoding;

   @JsonPropertyDescription("Configuration of Kafka broker access and/or install")
   private KafkaSpec kafka;

   @JsonPropertyDescription("Configuration of AMQP broker access")
   private GenericBrokerConnectionSpec amqp;

   @JsonPropertyDescription("Configuration of MQTT broker access")
   private GenericBrokerConnectionSpec mqtt;

   @JsonPropertyDescription("Configuration of NATS broker access")
   private GenericBrokerConnectionSpec nats;

   @JsonPropertyDescription("Configuration of Google PubSub access")
   private GooglePubSubConnectionSpec googlepubsub;

   @JsonPropertyDescription("Configuration of Amazon SQS access")
   private AmazonServiceConnectionSpec sqs;

   @JsonPropertyDescription("Configuration of Amazon SNS access")
   private AmazonServiceConnectionSpec sns;


   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public String getDefaultBinding() {
      return defaultBinding;
   }

   public void setDefaultBinding(String defaultBinding) {
      this.defaultBinding = defaultBinding;
   }

   public int getDefaultFrequency() {
      return defaultFrequency;
   }

   public void setDefaultFrequency(int defaultFrequency) {
      this.defaultFrequency = defaultFrequency;
   }

   public AvroEncoding getDefaultAvroEncoding() {
      return defaultAvroEncoding;
   }

   public void setDefaultAvroEncoding(AvroEncoding defaultAvroEncoding) {
      this.defaultAvroEncoding = defaultAvroEncoding;
   }

   public KafkaSpec getKafka() {
      return kafka;
   }

   public void setKafka(KafkaSpec kafka) {
      this.kafka = kafka;
   }

   public GenericBrokerConnectionSpec getAmqp() {
      return amqp;
   }

   public void setAmqp(GenericBrokerConnectionSpec amqp) {
      this.amqp = amqp;
   }

   public GenericBrokerConnectionSpec getMqtt() {
      return mqtt;
   }

   public void setMqtt(GenericBrokerConnectionSpec mqtt) {
      this.mqtt = mqtt;
   }

   public GenericBrokerConnectionSpec getNats() {
      return nats;
   }

   public void setNats(GenericBrokerConnectionSpec nats) {
      this.nats = nats;
   }

   public GooglePubSubConnectionSpec getGooglepubsub() {
      return googlepubsub;
   }

   public void setGooglepubsub(GooglePubSubConnectionSpec googlepubsub) {
      this.googlepubsub = googlepubsub;
   }

   public AmazonServiceConnectionSpec getSqs() {
      return sqs;
   }

   public void setSqs(AmazonServiceConnectionSpec sqs) {
      this.sqs = sqs;
   }

   public AmazonServiceConnectionSpec getSns() {
      return sns;
   }

   public void setSns(AmazonServiceConnectionSpec sns) {
      this.sns = sns;
   }
}
