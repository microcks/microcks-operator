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
 * A Strimzi Kafka NodePool dependent resource.
 * @author laurent
 */
public class StrimiziKafkaNodePoolResource {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-kafka-nodes";
   private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

   private final KubernetesClient client;

   /**
    * Create a new StrimiziKafkaNodePoolResource with a client.
    * @param client Kuebrnetes client to use for connecting to cluster.
    */
   public StrimiziKafkaNodePoolResource(KubernetesClient client) {
      this.client = client;
   }

   /**
    * Compute a desired Strimzi Kafka NodePool resource using a GenericKubernetesResource representation.
    * @param microcks The primary microcks resource
    * @param context  The reconciliation context
    * @return A GenericKubernetesResource holding a KafkaNodePool resource from Strimzi
    */
   public GenericKubernetesResource desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Strimzi KafkaNodePool for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      // Compute strimzi-kafka-nodepool with Qute template.
      String strimziKafkaNodePool = StrimiziKafkaNodePoolResource.Templates
            .kafkaNodePool(microcksName, microcks.getSpec()).render();

      Map kafkaNodePoolMap = null;
      try {
         kafkaNodePoolMap = mapper.readValue(strimziKafkaNodePool, Map.class);
      } catch (Exception e) {
         logger.error("Exception while deserializing the Strimzi Kafka YAML", e);
         return null;
      }

      // Build the generic Kubernetes resource from map content.
      GenericKubernetesResource genericKafkaNodePool = new GenericKubernetesResourceBuilder()
            .withApiVersion(kafkaNodePoolMap.get("apiVersion").toString())
            .withKind(kafkaNodePoolMap.get("kind").toString())
            .withNewMetadata()
               .withName(microcksName + RESOURCE_SUFFIX)
               .withLabels(Map.of("strimzi.io/cluster", StrimziKafkaResource.getKafkaName(microcks)))
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .addToAdditionalProperties("spec", kafkaNodePoolMap.get("spec")).build();

      return genericKafkaNodePool;
   }

   /** A Qute templates accessor. */
   @CheckedTemplate
   public static class Templates {
      /** Qute template for Kafka resource. */
      public static native TemplateInstance kafkaNodePool(String name, MicrocksSpec spec);
   }
}
