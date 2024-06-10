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
package io.github.microcks.operator.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Merger holds utility methods for merging resources together.
 * @author laurent
 */
public class ResourceMerger {

   /** List of Java primitive types typically found in Json/Yaml documents. */
   private final List<String> PRIMITIVE_JSON_TYPES = Arrays.asList("Long", "Long[]", "Integer", "Integer[]", "String",
         "String[]", "Boolean", "boolean[]", "ArrayList");

   /** List of Java types that must be merged as associative maps. */
   private final List<String> MAP_JSON_TYPES = Arrays.asList("HashMap", "LinkedHashMap");

   private final List<String> RESOURCES_EXCLUDED_TYPES = Arrays.asList("io.fabric8.kubernetes.api.model.ObjectMeta");

   /**
    * Merge 2 resources of class T together, producing a third one. The first argument is considered as the default
    * representation we should stick with if no customization is provided via the second argument.
    * @param local  Represent a default instance of the resource.
    * @param remote Represent a customized instance of the resource.
    * @return A new instance that is a merge of the two
    * @param <T> The resource class type
    * @throws IllegalAccessException If a reflection error occurs
    * @throws InstantiationException If an instantiation error occurs
    */
   @SuppressWarnings("unchecked")
   public <T> T mergeResources(T local, T remote) throws IllegalAccessException, InstantiationException {
      Class<?> clazz = local.getClass();
      Object merged = clazz.newInstance();

      for (Field field : clazz.getDeclaredFields()) {
         // Sanity check: don't try to set final or transient.
         if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
            field.setAccessible(true);

            // Manage safety cases first...
            if (remote == null && local != null) {
               field.set(merged, field.get(local));
            } else if (remote != null && local == null) {
               field.set(merged, field.get(remote));
            } else {
               // Safely access the values from here.
               Object localValue = field.get(local);
               Object remoteValue = field.get(remote);

               // Don't try to merge objects that must be kept as-is.
               if (!RESOURCES_EXCLUDED_TYPES.contains(field.getType().getName())) {
                  if (localValue != null) {
                     if (PRIMITIVE_JSON_TYPES.contains(localValue.getClass().getSimpleName())) {
                        // If remote value is a primitive, use it if not null.
                        field.set(merged, (remoteValue != null) ? remoteValue : localValue);
                     } else if (MAP_JSON_TYPES.contains(localValue.getClass().getSimpleName())) {
                        // If remote value if a map, use it as base and add extra values from local.
                        field.set(merged,
                              (remoteValue != null) ? mergeMaps((Map) remoteValue, (Map) localValue) : localValue);
                     } else if (remoteValue != null) {
                        // If a remote value provided, use this one.
                        field.set(merged, this.mergeResources(localValue, remoteValue));
                     } else {
                        // No remote value, use the local (default)
                        field.set(merged, localValue);
                     }
                  } else {
                     // No value provided as default.
                     if (remoteValue != null) {
                        // Use the one coming from remote resource.
                        field.set(merged, remoteValue);
                     }
                  }
               } else {
                  // Kept the object as-is.
                  field.set(merged, remoteValue);
               }
            }
         }
      }
      return (T) merged;
   }

   /** Merge additional entries from optional into base map. */
   private Map mergeMaps(Map base, Map optionalComplement) {
      optionalComplement.keySet().forEach(k -> base.putIfAbsent(k, optionalComplement.get(k)));
      return base;
   }
}
