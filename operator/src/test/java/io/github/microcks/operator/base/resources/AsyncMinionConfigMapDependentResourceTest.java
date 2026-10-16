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
import io.github.microcks.operator.base.MicrocksReconciler;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is a unit test verifying that the {@code {#if minorVersion >= 14}} condition of the async-minion
 * {@code application.properties} template is correctly handled, i.e. that the
 * {@code microcks-asyncapi-triggers} Kafka properties are rendered for Microcks 1.14.0 and 1.15.0.
 * It also verifies that the optional SASL callback handler classes are rendered on all Kafka clients
 * when configured, and omitted when they are not.
 * @author laurent
 */
@QuarkusTest
class AsyncMinionConfigMapDependentResourceTest {

   private static final String ASYNCAPI_TRIGGERS_BOOTSTRAP =
         "%kube.mp.messaging.incoming.microcks-asyncapi-triggers.bootstrap.servers=";

   private static final String CLIENT_CALLBACK_HANDLER_CLASS = "software.amazon.msk.auth.iam.IAMClientCallbackHandler";
   private static final String LOGIN_CALLBACK_HANDLER_CLASS = "com.example.MyLoginHandler";

   /** The native producers and both SmallRye channels each need their own copy of the client callback handler. */
   private static final String[] CLIENT_CALLBACK_HANDLER_KEYS = {
         "%kube.kafka.sasl.client.callback.handler.class=",
         "%kube.mp.messaging.incoming.microcks-services-updates.sasl.client.callback.handler.class=",
         "%kube.mp.messaging.incoming.microcks-asyncapi-triggers.sasl.client.callback.handler.class=" };

   private static final String[] LOGIN_CALLBACK_HANDLER_KEYS = {
         "%kube.kafka.sasl.login.callback.handler.class=",
         "%kube.mp.messaging.incoming.microcks-services-updates.sasl.login.callback.handler.class=",
         "%kube.mp.messaging.incoming.microcks-asyncapi-triggers.sasl.login.callback.handler.class=" };

   private Microcks buildMicrocks(String version) throws Exception {
      MicrocksReconciler reconciler = new MicrocksReconciler(new KubernetesClientBuilder().build());
      MicrocksSpec spec = reconciler.loadDefaultMicrocksSpec(version);
      spec.setVersion(version);

      Microcks microcks = new Microcks();
      microcks.setMetadata(new ObjectMetaBuilder().withName("microcks").withNamespace("test").build());
      microcks.setSpec(spec);
      return microcks;
   }

   private String render(Microcks microcks) throws Exception {
      ConfigMap configMap = new AsyncMinionConfigMapDependentResource().desired(microcks, null);
      return configMap.getData().get("application.properties");
   }

   private String renderApplicationProperties(String version) throws Exception {
      return render(buildMicrocks(version));
   }

   /** Renders the async-minion properties for an external Kafka cluster using the given authentication spec. */
   private String renderApplicationPropertiesWithKafkaAuthentication(String version,
         KafkaAuthenticationSpec authentication) throws Exception {
      Microcks microcks = buildMicrocks(version);

      KafkaSpec kafkaSpec = new KafkaSpec();
      kafkaSpec.setInstall(false);
      kafkaSpec.setUrl("my-cluster:9092");
      kafkaSpec.setAuthentication(authentication);

      AsyncFeatureSpec asyncSpec = new AsyncFeatureSpec();
      asyncSpec.setEnabled(true);
      asyncSpec.setKafka(kafkaSpec);

      microcks.getSpec().getFeatures().setAsync(asyncSpec);
      return render(microcks);
   }

   private static void assertPropertiesRendered(String applicationProperties, String[] propertyKeys, String value) {
      for (String propertyKey : propertyKeys) {
         Assertions.assertTrue(applicationProperties.contains(propertyKey + value),
               propertyKey + value + " must be present in rendered application.properties");
      }
   }

   private static void assertPropertiesNotRendered(String applicationProperties, String[] propertyKeys) {
      for (String propertyKey : propertyKeys) {
         Assertions.assertFalse(applicationProperties.contains(propertyKey),
               propertyKey + " must be absent when no callback handler class is configured");
      }
   }

   @Test
   void testAsyncApiTriggersPropertiesRenderedFor1_14_0() throws Exception {
      String applicationProperties = renderApplicationProperties("1.14.0");

      Assertions.assertTrue(applicationProperties.contains(ASYNCAPI_TRIGGERS_BOOTSTRAP),
            "microcks-asyncapi-triggers bootstrap.servers property must be present for Microcks 1.14.0");
   }

   @Test
   void testAsyncApiTriggersPropertiesRenderedFor1_15_0() throws Exception {
      String applicationProperties = renderApplicationProperties("1.15.0");

      Assertions.assertTrue(applicationProperties.contains(ASYNCAPI_TRIGGERS_BOOTSTRAP),
            "microcks-asyncapi-triggers bootstrap.servers property must be present for Microcks 1.15.0");
   }

   @Test
   void testSaslCallbackHandlerPropertiesRendered() throws Exception {
      KafkaAuthenticationSpec authenticationSpec = new KafkaAuthenticationSpec();
      authenticationSpec.setType(KafkaAuthenticationType.SASL_SSL);
      authenticationSpec.setSaslMechanism("AWS_MSK_IAM");
      authenticationSpec.setSaslClientCallbackHandlerClass(CLIENT_CALLBACK_HANDLER_CLASS);
      authenticationSpec.setSaslLoginCallbackHandlerClass(LOGIN_CALLBACK_HANDLER_CLASS);

      String applicationProperties = renderApplicationPropertiesWithKafkaAuthentication("1.14.0", authenticationSpec);

      assertPropertiesRendered(applicationProperties, CLIENT_CALLBACK_HANDLER_KEYS, CLIENT_CALLBACK_HANDLER_CLASS);
      assertPropertiesRendered(applicationProperties, LOGIN_CALLBACK_HANDLER_KEYS, LOGIN_CALLBACK_HANDLER_CLASS);
   }

   @Test
   void testSaslCallbackHandlerPropertiesNotRenderedWhenNotConfigured() throws Exception {
      KafkaAuthenticationSpec authenticationSpec = new KafkaAuthenticationSpec();
      authenticationSpec.setType(KafkaAuthenticationType.SASL_SSL);
      authenticationSpec.setSaslMechanism("SCRAM-SHA-512");

      String applicationProperties = renderApplicationPropertiesWithKafkaAuthentication("1.14.0", authenticationSpec);

      // Guard against a false pass: the SASL_SSL branch must have been rendered for the absence to mean anything.
      Assertions.assertTrue(applicationProperties.contains("%kube.kafka.sasl.mechanism="),
            "SASL_SSL branch must be rendered for this test to be meaningful");

      assertPropertiesNotRendered(applicationProperties, CLIENT_CALLBACK_HANDLER_KEYS);
      assertPropertiesNotRendered(applicationProperties, LOGIN_CALLBACK_HANDLER_KEYS);
   }
}
