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

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

/**
 * This is the API definition for Microcks custom resource.
 * @author laurent
 */
@Group("microcks.io")
@Version("v1alpha1")
@ShortNames("microcks")
@Plural("Microckses")
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", refs = {
      @BuildableReference(ObjectMeta.class) })
public class Microcks extends CustomResource<MicrocksSpec, MicrocksStatus> implements Namespaced {

   /*
    * private ObjectMeta metadata; // Add it for the generator / builder private MicrocksSpec spec; // Add it for the
    * generator / builder
    * 
    * @Override public ObjectMeta getMetadata() { return metadata; }
    * 
    * @Override public void setMetadata(ObjectMeta metadata) { this.metadata = metadata; }
    * 
    * @Override public MicrocksSpec getSpec() { return spec; }
    * 
    * @Override public void setSpec(MicrocksSpec spec) { this.spec = spec; }
    */
}
