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

import io.github.microcks.operator.api.base.v1alpha1.Microcks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * A Strimzi Kubernetes KafkaTopic dependent resource.
 * @author laurent
 */
public class StrimziKafkaTopicResource {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-kafka-services-updates";

   private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

   private KubernetesClient client;

   /**
    * Create a new StrimziKafkaTopicResource with a client.
    * @param client Kuebrnetes client to use for connecting to cluster.
    */
   public StrimziKafkaTopicResource(KubernetesClient client) {
      this.client = client;
   }

   /**
    * Compute a desired Strimzi Kafka Topic resource using a GenericKubernetesResource representation.
    * @param microcks The primary microcks resource
    * @param context  The reconciliation context
    * @return A GenericKubernetesResource holding a KafkaTopic resource from Strimzi
    */
   public GenericKubernetesResource desired(Microcks microcks, Context<Microcks> context) {
      logger.infof("Building desired Strimzi Kafka Topic for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      // Compute strimzi-topic with Qute template.
      String strimziTopic = Templates.kafkaTopic(microcksName, microcksMetadata.getNamespace()).render();

      Map topicMap = null;
      try {
         topicMap = mapper.readValue(strimziTopic, Map.class);
      } catch (Exception e) {
         logger.error("Exception while deserializing the Strimzi Kafka YAML", e);
         return null;
      }

      GenericKubernetesResource genericTopic = new GenericKubernetesResourceBuilder()
            .withApiVersion(topicMap.get("apiVersion").toString()).withKind(topicMap.get("kind").toString())
            .withNewMetadata().withName(microcksName + RESOURCE_SUFFIX)
            .addToLabels("strimzi.io/cluster", StrimziKafkaResource.getKafkaName(microcks)).endMetadata()
            .addToAdditionalProperties("spec", topicMap.get("spec")).build();

      return genericTopic;
   }

   /** A Qute templates accessor. */
   @CheckedTemplate
   public static class Templates {
      /** Qute template for KafkaTopic resource. */
      public static native TemplateInstance kafkaTopic(String name, String namespace);
   }
}
