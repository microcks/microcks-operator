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

import io.github.microcks.operator.MicrocksOperatorConfig;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.model.NamedSecondaryResourceProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.logging.Logger;

/**
 * A Microcks Kubernetes ConfigMap dependent resource.
 * @author laurent
 */
@KubernetesDependent(informer = @Informer(labelSelector = MicrocksOperatorConfig.RESOURCE_LABEL_SELECTOR))
public class MicrocksConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, Microcks>
      implements NamedSecondaryResourceProvider<Microcks> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String RESOURCE_SUFFIX = "-config";

   private ObjectMapper EXTRA_PROPERTIES_WRITER = new ObjectMapper(
         new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

   /** Default empty constructor. */
   public MicrocksConfigMapDependentResource() {
      super(ConfigMap.class);
   }

   /**
    * Get the name of ConfigMap given the primary Microcks resource.
    * @param microcks The primary resource
    * @return The name of ConfigMap
    */
   public static final String getConfigMapName(Microcks microcks) {
      return microcks.getMetadata().getName() + RESOURCE_SUFFIX;
   }

   @Override
   public String getSecondaryResourceName(Microcks primary) {
      return getConfigMapName(primary);
   }

   @Override
   protected ConfigMap desired(Microcks microcks, Context<Microcks> context) {
      logger.debugf("Building desired Microcks ConfigMap for '%s'", microcks.getMetadata().getName());

      final ObjectMeta microcksMetadata = microcks.getMetadata();
      final String microcksName = microcksMetadata.getName();

      // Compute configuration files with Qute templates.
      String applicationProperties = Templates.application(microcksName, microcksMetadata.getNamespace(), microcks.getSpec()).render();
      String featuresProperties = Templates.features(microcksName, microcksMetadata.getNamespace(), microcks.getSpec(),
            AsyncMinionWSIngressDependentResource.getWSHost(microcks)).render();
      String logbackXml = Templates.logback(microcks.getSpec()).render();

      ConfigMapBuilder builder = new ConfigMapBuilder().withNewMetadata().withName(getSecondaryResourceName(microcks))
               .withNamespace(microcksMetadata.getNamespace())
               .addToLabels("app", microcksName)
               .addToLabels("container", "microcks")
               .addToLabels("group", "microcks")
               .addToLabels(microcks.getSpec().getCommonLabels())
               .addToAnnotations(microcks.getSpec().getCommonAnnotations())
            .endMetadata()
            .addToData("application.properties", applicationProperties)
            .addToData("features.properties", featuresProperties).addToData("logback.xml", logbackXml);

      if (microcks.getSpec().getMicrocks().getExtraProperties() != null
            && !microcks.getSpec().getMicrocks().getExtraProperties().isEmpty()) {
         try {
            builder.addToData("application-extra.yaml",
                  EXTRA_PROPERTIES_WRITER.writeValueAsString(microcks.getSpec().getMicrocks().getExtraProperties()));
         } catch (JsonProcessingException e) {
            logger.error("Unable to write extraProperties in Microcks config", e);
         }
      }

      return builder.build();
   }

   /** A Qute templates accessor. */
   @CheckedTemplate
   public static class Templates {
      /** Qute template for application.properties. */
      public static native TemplateInstance application(String name, String namespace, MicrocksSpec spec);

      /** Qute template for features.properties. */
      public static native TemplateInstance features(String name, String namespace, MicrocksSpec spec, String wsUrl);

      /** Qute template for logback.xml. */
      public static native TemplateInstance logback(MicrocksSpec spec);
   }
}
