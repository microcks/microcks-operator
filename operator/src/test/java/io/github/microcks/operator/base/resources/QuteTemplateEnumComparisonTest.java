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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This is a guard test for the Qute templates: an {@code {#if}} condition must not compare an enum with a string
 * literal. Qute compares the objects themselves, so an enum never equals a String and a condition such as
 * {@code {#if spec.commonExpositions.type == "INGRESS"}} silently never matches. Compare the enum name instead:
 * {@code {#if spec.commonExpositions.type.name == "INGRESS"}}.
 * <p>
 * A template {@code templates/<Class>/<method>.<ext>} is checked against the parameter types of its
 * {@code @CheckedTemplate} method {@code <Class>.Templates.<method>(...)}. An expression that cannot be resolved
 * also fails the test, so that it is checked by hand rather than silently skipped.
 * @author Abhayanth K
 */
class QuteTemplateEnumComparisonTest {

   private static final Path TEMPLATES_DIR = Paths.get("src/main/resources/templates");

   /** The condition of an {@code {#if ...}} or {@code {#else if ...}} section. */
   private static final Pattern CONDITION = Pattern.compile("\\{#(?:else )?if\\s+([^}]*)}");

   /** A dotted expression compared with a string literal, using any of the Qute equality operators. */
   private static final Pattern STRING_COMPARISON = Pattern
         .compile("([A-Za-z_][\\w.]*)\\s*(?:==|!=|\\beq\\b|\\bne\\b|\\bis\\b)\\s*(['\"])[^'\"]*\\2");

   @Test
   void testNoEnumComparedWithStringLiteral() throws Exception {
      List<String> violations = new ArrayList<>();
      try (Stream<Path> templates = Files.walk(TEMPLATES_DIR)) {
         for (Path template : templates.filter(Files::isRegularFile).sorted().toList()) {
            violations.addAll(checkTemplate(template));
         }
      }
      Assertions.assertTrue(violations.isEmpty(),
            "Compare enums by name in Qute conditions:\n" + String.join("\n", violations));
   }

   private List<String> checkTemplate(Path template) throws Exception {
      Method templateMethod = findTemplateMethod(template);
      List<String> lines = Files.readAllLines(template);
      List<String> violations = new ArrayList<>();

      for (int i = 0; i < lines.size(); i++) {
         Matcher condition = CONDITION.matcher(lines.get(i));
         while (condition.find()) {
            Matcher comparison = STRING_COMPARISON.matcher(condition.group(1));
            while (comparison.find()) {
               Class<?> type = resolveType(templateMethod, comparison.group(1));
               String location = TEMPLATES_DIR.relativize(template) + ":" + (i + 1) + ": " + comparison.group();
               if (type == null) {
                  violations.add(location + " (cannot resolve " + comparison.group(1) + ", check it by hand)");
               } else if (type.isEnum()) {
                  violations.add(location + " (" + type.getSimpleName() + " is an enum)");
               }
            }
         }
      }
      return violations;
   }

   /** Finds the {@code <Class>.Templates.<method>} declaration bound to a {@code <Class>/<method>.<ext>} template. */
   private Method findTemplateMethod(Path template) throws ClassNotFoundException {
      String className = getClass().getPackageName() + "." + template.getParent().getFileName() + "$Templates";
      String fileName = template.getFileName().toString();
      String methodName = fileName.substring(0, fileName.indexOf('.'));

      Class<?> templates = Class.forName(className, false, getClass().getClassLoader());
      return Arrays.stream(templates.getDeclaredMethods()).filter(method -> method.getName().equals(methodName))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No " + className + "." + methodName + " for " + template));
   }

   /** Resolves the type of a dotted expression such as {@code spec.commonExpositions.type}, or null if unknown. */
   private static Class<?> resolveType(Method templateMethod, String expression) {
      String[] segments = expression.split("\\.");
      Class<?> type = null;
      for (Parameter parameter : templateMethod.getParameters()) {
         Assertions.assertTrue(parameter.isNamePresent(), "Compile with -parameters to resolve template parameters");
         if (parameter.getName().equals(segments[0])) {
            type = parameter.getType();
         }
      }
      for (int i = 1; i < segments.length && type != null; i++) {
         type = propertyType(type, segments[i]);
      }
      return type;
   }

   /** Mirrors the Qute reflection resolver: a no-arg method named after the property, its getter, or a field. */
   private static Class<?> propertyType(Class<?> type, String property) {
      String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);
      for (String methodName : List.of(property, "get" + capitalized, "is" + capitalized)) {
         try {
            return type.getMethod(methodName).getReturnType();
         } catch (NoSuchMethodException e) {
            // Try the next naming convention.
         }
      }
      try {
         return type.getField(property).getType();
      } catch (NoSuchFieldException e) {
         return null;
      }
   }
}
