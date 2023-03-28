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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.github.microcks.operator.api.Microcks;
import io.github.microcks.operator.api.MicrocksSpec;
import io.github.microcks.operator.api.model.MicrocksServiceSpec;

/**
 * @author laurent
 */
public class MicrocksService extends AbstractModel {

   private static final int DEFAULT_REPLICAS = 1;

   private static final boolean DEFAULT_INVOCATION_STATS_ENABLED = true;

   protected boolean mockInvocationStats;

   private MicrocksService(HasMetadata resource) {
      setReplicas(DEFAULT_REPLICAS);
      setOwnerReference(resource);
      this.mockInvocationStats = DEFAULT_INVOCATION_STATS_ENABLED;
   }

   public static MicrocksService fromCrd(Microcks microcks) {

      MicrocksSpec microcksSpec = microcks.getSpec();
      MicrocksServiceSpec serviceSpec = microcksSpec.getMicrocks();

      MicrocksService result = new MicrocksService(microcks);

      return result;
   }
}
