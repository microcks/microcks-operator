package io.github.microcks.operator.model;

import io.github.microcks.operator.api.Microcks;
import io.github.microcks.operator.api.MicrocksSpec;
import io.github.microcks.operator.api.model.KeycloakSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This is a unit test for the {@link Merger} util class.
 * @author Laurent
 */
public class MergerTest {

   private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

   private Microcks defaultCR;

   @BeforeEach
   public void initDefaultMicrocks() {
      try {
         defaultCR = mapper.readValue(
               getClass().getResourceAsStream("/k8s/microcks-default.yml"), Microcks.class);
      } catch (Exception e) {
         e.printStackTrace();
         fail("Loading default Microcks CR should not fail");
      }
   }

   @Test
   public void testMergeEmptyMicrocks() {
      Microcks simplestCR = new Microcks();

      Microcks result = null;
      try {
         result = new Merger().merge(defaultCR, simplestCR);
      } catch (Throwable t) {
         fail("Merging Microcks CR should not fail");
      }

      assertNotNull(result.getSpec());
      assertEquals("latest", result.getSpec().getVersion());
      assertEquals(1, result.getSpec().getMicrocks().getReplicas());
      assertEquals(1, result.getSpec().getPostman().getReplicas());
      assertTrue(result.getSpec().getKeycloak().isInstall());
      assertTrue(result.getSpec().getMongoDB().isInstall());
      assertFalse(result.getSpec().getFeatures().getAsync().isEnabled());
   }

   @Test
   public void testMergePartialMicrocks() {
      Microcks partialCR = new Microcks();
      MicrocksSpec spec = new MicrocksSpec();
      KeycloakSpec keycloak = new KeycloakSpec();
      spec.setKeycloak(keycloak);
      partialCR.setSpec(spec);

      spec.setVersion("1.7.0");
      keycloak.setInstall(false);
      keycloak.setRealm("custom-realm");
      keycloak.setUrl("keycloak.acme.com");

      Microcks result = null;
      try {
         result = new Merger().merge(defaultCR, partialCR);
      } catch (Throwable t) {
         fail("Merging Microcks CR should not fail");
      }

      assertNotNull(result.getSpec());
      assertEquals("1.7.0", result.getSpec().getVersion());
      assertEquals(1, result.getSpec().getMicrocks().getReplicas());
      assertEquals(1, result.getSpec().getPostman().getReplicas());
      assertFalse(result.getSpec().getKeycloak().isInstall());
      assertEquals("custom-realm", result.getSpec().getKeycloak().getRealm());
      assertEquals("keycloak.acme.com", result.getSpec().getKeycloak().getUrl());
      assertTrue(result.getSpec().getMongoDB().isInstall());
      assertFalse(result.getSpec().getFeatures().getAsync().isEnabled());
   }
}
