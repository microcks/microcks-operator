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
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * A Strimzi Kafka dependent resource.
 * @author laurent
 */
public class StrimziKafkaResource {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-kafka";
   private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

   private final KubernetesClient client;

   /**
    * Create a new StrimziKafkaResource with a client.
    * @param client Kuebrnetes client to use for connecting to cluster.
    */
   public StrimziKafkaResource(KubernetesClient client) {
      this.client = client;
   }

   /**
    * Get the name of Kafka given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of Kafka
    */
   public static String getKafkaName(Microcks microcks) {
      return microcks.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   /**
    * Compute a desired Strimzi Kafka resource using a GenericKubernetesResource representation.
    * @param microcks The primary microcks resource
    * @param context  The reconciliation context
    * @return A GenericKubernetesResource holding a Kafka resource from Strimzi
    */
   public GenericKubernetesResource desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Strimzi Kafka for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      // Compute strimzi-kafka with Qute template.
      String strimziKafka = Templates
            .kafka(microcksName, microcks.getSpec(), client.adapt(OpenShiftClient.class).hasApiGroup("route.openshift.io", true))
            .render();

      Map kafkaMap = null;
      try {
         kafkaMap = mapper.readValue(strimziKafka, Map.class);
      } catch (Exception e) {
         logger.error("Exception while deserializing the Strimzi Kafka YAML", e);
         return null;
      }

      // Build the generic Kubernetes resource from map content.
      GenericKubernetesResourceBuilder genericKafka = new GenericKubernetesResourceBuilder()
            .withApiVersion(kafkaMap.get("apiVersion").toString())
            .withKind(kafkaMap.get("kind").toString())
            .withNewMetadata()
               .withName(microcksName + RESOURCE_SUFFIX)
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .addToAdditionalProperties("spec", kafkaMap.get("spec"));

      if (microcks.getSpec().getFeatures().getAsync().getKafka().isEnableKraft()) {
         genericKafka.editMetadata()
               .addToAnnotations(Map.of(
                     "strimzi.io/node-pools", "enabled",
                     "strimzi.io/kraft", "enabled"))
               .endMetadata();
      }

      return genericKafka.build();
   }

   /** A Qute templates accessor. */
   @CheckedTemplate
   public static class Templates {
      /** Qute template for Kafka resource. */
      public static native TemplateInstance kafka(String name, MicrocksSpec spec, boolean isOpenShift);
   }
}
