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
package io.github.microcks.operator;

/**
 * A simple record that serves as a key for the {@code WatcherManager}.
 * @param name       The name of watched resource
 * @param kind       The kind of watched resource
 * @param apiVersion The api version of watched resource
 */
public record WatcherKey(String name, String kind, String apiVersion) {
}
