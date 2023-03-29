package io.github.microcks.operator.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
public class Merger {

   private final List<String> PRIMITIVE_JSON_TYPES = Arrays.asList("Long", "Long[]",
         "Integer", "Integer[]", "String", "String[]",
         "Boolean", "boolean[]", "ArrayList", "LinkedHashMap");

   @SuppressWarnings("unchecked")
   public <T> T merge(T local, T remote) throws IllegalAccessException, InstantiationException {
      Class<?> clazz = local.getClass();
      Object merged = clazz.newInstance();

      for (Field field : clazz.getDeclaredFields()) {

         field.setAccessible(true);
         Object localValue = field.get(local);
         Object remoteValue = field.get(remote);

         if (localValue != null) {
            if (PRIMITIVE_JSON_TYPES.contains(localValue.getClass().getSimpleName())) {
               field.set(merged, (remoteValue != null) ? remoteValue : localValue);
            } else if (remoteValue != null) {
               field.set(merged, this.merge(localValue, remoteValue));
            } else {
               field.set(merged, localValue);
            }
         } else {
            if (remoteValue != null) {
               field.set(merged, remoteValue);
            }
         }
      }
      return (T) merged;
   }
}
