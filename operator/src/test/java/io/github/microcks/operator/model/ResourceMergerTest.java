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
package io.github.microcks.operator.model;

import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.base.v1alpha1.MicrocksSpec;
import io.github.microcks.operator.api.base.v1alpha1.KeycloakSpec;

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
 * This is a unit test for the {@link ResourceMerger} util class.
 * @author Laurent
 */
public class ResourceMergerTest {

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
   public void testMergeEmptyMicrocksSpec() {
      Microcks simplestCR = new Microcks();

      MicrocksSpec result = null;
      try {
         result = new ResourceMerger().mergeResources(defaultCR.getSpec(), simplestCR.getSpec());
      } catch (Throwable t) {
         t.printStackTrace();
         fail("Merging Microcks CR should not fail");
      }

      assertNotNull(result);
      assertEquals("latest", result.getVersion());
      assertEquals(1, result.getMicrocks().getReplicas());
      assertEquals(1, result.getPostman().getReplicas());
      assertTrue(result.getKeycloak().isInstall());
      assertTrue(result.getMongoDB().isInstall());
      assertFalse(result.getFeatures().getAsync().isEnabled());
   }

   @Test
   public void testMergePartialMicrocksSpec() {
      Microcks partialCR = new Microcks();
      MicrocksSpec spec = new MicrocksSpec();
      KeycloakSpec keycloak = new KeycloakSpec();
      spec.setKeycloak(keycloak);
      partialCR.setSpec(spec);

      spec.setVersion("1.7.0");
      keycloak.setInstall(false);
      keycloak.setRealm("custom-realm");
      keycloak.setUrl("keycloak.acme.com");

      MicrocksSpec result = null;
      try {
         result = new ResourceMerger().mergeResources(defaultCR.getSpec(), partialCR.getSpec());
      } catch (Throwable t) {
         t.printStackTrace();
         fail("Merging Microcks CR should not fail");
      }

      assertNotNull(result);
      assertEquals("1.7.0", result.getVersion());
      assertEquals(1, result.getMicrocks().getReplicas());
      assertEquals(1, result.getPostman().getReplicas());
      assertFalse(result.getKeycloak().isInstall());
      assertEquals("custom-realm", result.getKeycloak().getRealm());
      assertEquals("keycloak.acme.com", result.getKeycloak().getUrl());
      assertTrue(result.getMongoDB().isInstall());
      assertFalse(result.getFeatures().getAsync().isEnabled());
   }
}
