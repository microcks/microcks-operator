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
package io.github.microcks.operator.base.resources;

import io.github.microcks.operator.api.base.v1alpha1.AsyncFeatureSpec;
import io.github.microcks.operator.api.base.v1alpha1.KafkaAuthenticationSpec;
import io.github.microcks.operator.api.base.v1alpha1.KafkaAuthenticationType;
import io.github.microcks.operator.api.base.v1alpha1.KafkaSpec;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksStatus;
import io.github.microcks.operator.base.MicrocksReconciler;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is a unit test verifying the Kafka authentication section of the main web app
 * {@code application.properties} template: SASL properties must be rendered for an external Kafka cluster
 * ({@code install: false}), on both the pre-1.14 and the 1.14+ producer layouts, and must not be rendered
 * when the operator installs Kafka itself.
 * @author Abhayanth K
 */
@QuarkusTest
class MicrocksConfigMapDependentResourceTest {

   private static final String CLIENT_CALLBACK_HANDLER_CLASS = "software.amazon.msk.auth.iam.IAMClientCallbackHandler";
   private static final String LOGIN_CALLBACK_HANDLER_CLASS = "com.example.MyLoginHandler";

   /** Since Microcks 1.14, the web app has two dedicated Kafka producers, each configured separately. */
   private static final String[] PRODUCER_PREFIXES = {
         "kafka.producers.service-changes.properties.",
         "kafka.producers.asyncapi-triggers.properties." };

   /** Before Microcks 1.14, the web app relies on the single Spring Kafka producer. */
   private static final String SPRING_PRODUCER_PREFIX = "spring.kafka.producer.properties.";

   private static final String CLIENT_CALLBACK_HANDLER_KEY = "sasl.client.callback.handler.class=";
   private static final String LOGIN_CALLBACK_HANDLER_KEY = "sasl.login.callback.handler.class=";

   private Microcks buildMicrocks(String version) throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());
      MicrocksSpec spec = reconciler.loadDefaultMicrocksSpec(version);
      spec.setVersion(version);

      // The web app template reads the exposed URLs from the status, so it has to be present.
      MicrocksStatus status = new MicrocksStatus();
      status.setMicrocksUrl("microcks.example.com");
      status.setKeycloakUrl("keycloak.example.com");

      Microcks microcks = new Microcks();
      microcks.setMetadata(new ObjectMetaBuilder().withName("microcks").withNamespace("test").build());
      microcks.setSpec(spec);
      microcks.setStatus(status);
      return microcks;
   }

   private String renderApplicationProperties(String version, boolean installKafka,
         KafkaAuthenticationSpec authentication) throws Exception {
      Microcks microcks = buildMicrocks(version);

      KafkaSpec kafkaSpec = new KafkaSpec();
      kafkaSpec.setInstall(installKafka);
      kafkaSpec.setUrl("my-cluster:9098");
      kafkaSpec.setAuthentication(authentication);

      AsyncFeatureSpec asyncSpec = new AsyncFeatureSpec();
      asyncSpec.setEnabled(true);
      asyncSpec.setKafka(kafkaSpec);

      microcks.getSpec().getFeatures().setAsync(asyncSpec);

      ConfigMap configMap = new MicrocksConfigMapDependentResource().desired(microcks, null);
      return configMap.getData().get("application.properties");
   }

   private static KafkaAuthenticationSpec saslSslAuthentication(String saslMechanism) {
      KafkaAuthenticationSpec authentication = new KafkaAuthenticationSpec();
      authentication.setType(KafkaAuthenticationType.SASL_SSL);
      authentication.setSaslMechanism(saslMechanism);
      return authentication;
   }

   private static KafkaAuthenticationSpec saslSslAuthenticationWithCallbackHandlers() {
      KafkaAuthenticationSpec authentication = saslSslAuthentication("AWS_MSK_IAM");
      authentication.setSaslClientCallbackHandlerClass(CLIENT_CALLBACK_HANDLER_CLASS);
      authentication.setSaslLoginCallbackHandlerClass(LOGIN_CALLBACK_HANDLER_CLASS);
      return authentication;
   }

   private static void assertRendered(String applicationProperties, String property) {
      Assertions.assertTrue(applicationProperties.contains(property),
            property + " must be present in rendered application.properties");
   }

   private static void assertNotRendered(String applicationProperties, String property) {
      Assertions.assertFalse(applicationProperties.contains(property),
            property + " must be absent from rendered application.properties");
   }

   @Test
   void testSaslPropertiesRenderedForExternalKafka() throws Exception {
      String applicationProperties = renderApplicationProperties("1.14.0", false,
            saslSslAuthenticationWithCallbackHandlers());

      for (String producerPrefix : PRODUCER_PREFIXES) {
         assertRendered(applicationProperties, producerPrefix + "security.protocol=SASL_SSL");
         assertRendered(applicationProperties, producerPrefix + "sasl.mechanism=AWS_MSK_IAM");
         assertRendered(applicationProperties,
               producerPrefix + CLIENT_CALLBACK_HANDLER_KEY + CLIENT_CALLBACK_HANDLER_CLASS);
         assertRendered(applicationProperties,
               producerPrefix + LOGIN_CALLBACK_HANDLER_KEY + LOGIN_CALLBACK_HANDLER_CLASS);
      }
   }

   @Test
   void testSaslPropertiesRenderedForExternalKafkaBefore1_14() throws Exception {
      String applicationProperties = renderApplicationProperties("1.13.0", false,
            saslSslAuthenticationWithCallbackHandlers());

      assertRendered(applicationProperties, SPRING_PRODUCER_PREFIX + "security.protocol=SASL_SSL");
      assertRendered(applicationProperties, SPRING_PRODUCER_PREFIX + "sasl.mechanism=AWS_MSK_IAM");
      assertRendered(applicationProperties,
            SPRING_PRODUCER_PREFIX + CLIENT_CALLBACK_HANDLER_KEY + CLIENT_CALLBACK_HANDLER_CLASS);
      assertRendered(applicationProperties,
            SPRING_PRODUCER_PREFIX + LOGIN_CALLBACK_HANDLER_KEY + LOGIN_CALLBACK_HANDLER_CLASS);
   }

   @Test
   void testSaslCallbackHandlerPropertiesNotRenderedWhenNotConfigured() throws Exception {
      String applicationProperties = renderApplicationProperties("1.14.0", false,
            saslSslAuthentication("SCRAM-SHA-512"));

      for (String producerPrefix : PRODUCER_PREFIXES) {
         // Guard against a false pass: the SASL_SSL branch must have been rendered for the absence to mean anything.
         assertRendered(applicationProperties, producerPrefix + "sasl.mechanism=SCRAM-SHA-512");
         assertNotRendered(applicationProperties, producerPrefix + CLIENT_CALLBACK_HANDLER_KEY);
         assertNotRendered(applicationProperties, producerPrefix + LOGIN_CALLBACK_HANDLER_KEY);
      }
   }

   @Test
   void testKafkaAuthenticationNotRenderedForInstalledKafka() throws Exception {
      String applicationProperties = renderApplicationProperties("1.14.0", true,
            saslSslAuthenticationWithCallbackHandlers());

      // Guard against a false pass: the Kafka section itself must have been rendered.
      assertRendered(applicationProperties, "kafka.bootstrap-servers=");
      for (String producerPrefix : PRODUCER_PREFIXES) {
         assertNotRendered(applicationProperties, producerPrefix + "security.protocol=");
         assertNotRendered(applicationProperties, producerPrefix + CLIENT_CALLBACK_HANDLER_KEY);
      }
   }
}
