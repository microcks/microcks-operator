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

/**
 * The different supported Avro encoding modes. <br/>
 * <ul>
 * <li>{@code RAW} should be used for encoding/deconding without a schema registry</li>
 * <li>{@code REGISTRY} should be used for encoding/deconding with a schema registry that may be configured</li>
 * </ul>
 * @author laurent
 */
public enum AvroEncoding {
   RAW,
   REGISTRY
}
