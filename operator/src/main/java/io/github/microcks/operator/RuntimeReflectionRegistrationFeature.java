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

import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.api.model.IngressSpec;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.jboss.logging.Logger;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A GraalVM feature that allows auto-registration of classes for serialization. This to avoid having to maintain the
 * reflection-config.json with large classes list.
 * @author laurent
 */
public class RuntimeReflectionRegistrationFeature implements Feature {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @Override
   public String getDescription() {
      return "Register Custom Resource API classes for reflection";
   }

   @Override
   public void beforeAnalysis(BeforeAnalysisAccess access) {
      final ClassLoader loader = access.getApplicationClassLoader();

      // Gathering all the classes from API packages in same Jar locations.
      Set<Class> apiClasses = findAllClassesInSamePackageAndSameLocation(loader, IngressSpec.class);
      apiClasses.addAll(findAllClassesInSamePackageAndSameLocation(loader, Microcks.class));

      for (Class clazz : apiClasses) {
         registerClassForReflection(clazz);
      }
   }

   /**
    * Register all class elements (constructors, methods, fields) for reflection in GraalVM.
    * @param clazz The class to register.
    */
   public void registerClassForReflection(Class clazz) {
      logger.infof("Registering %s for reflection", clazz.getCanonicalName());
      RuntimeReflection.register(clazz);
      RuntimeReflection.register(clazz.getDeclaredConstructors());
      RuntimeReflection.register(clazz.getDeclaredMethods());
      RuntimeReflection.register(clazz.getDeclaredFields());
      RuntimeReflection.register(clazz.getConstructors());
      RuntimeReflection.register(clazz.getMethods());
      RuntimeReflection.register(clazz.getFields());
   }

   /**
    * Build a set of classes having the same packages and being in same jar/path location that the one provided as the
    * reference class.
    * @param loader         The classloader to use when instrospecting for classes
    * @param referenceClazz The reference class to use for finding base package and same location.
    * @return A Set of classes matching the package and location of reference.
    */
   public Set<Class> findAllClassesInSamePackageAndSameLocation(ClassLoader loader, Class referenceClazz) {
      // We used https://www.baeldung.com/java-find-all-classes-in-package as a starting point but
      // since requested package is dispatched in different Jars due to Quarkus class generation mechanisms,
      // we could not find back the package classes. We had to go the hard way through filesystem and path.
      final String packageName = referenceClazz.getPackageName();
      final String packagePath = referenceClazz.getPackageName().replace('.', '/');
      final String extension = ".class";

      URI clazzURI;
      try {
         clazzURI = loader.getResource(
               referenceClazz.getPackageName().replace('.', '/') + "/" + referenceClazz.getSimpleName() + ".class")
               .toURI();
      } catch (Exception e) {
         // Return empty set.
         logger.errorf("Exception while loading URI for clazz %s", referenceClazz.getCanonicalName());
         return new HashSet<>();
      }

      try {
         final String clazzPath = clazzURI.toString();
         URI pkg = URI.create(clazzPath.substring(0, clazzPath.lastIndexOf('/')));

         Path root;
         if (pkg.toString().startsWith("jar:")) {
            try {
               root = FileSystems.getFileSystem(pkg).getPath(packagePath);
            } catch (final FileSystemNotFoundException e) {
               root = FileSystems.newFileSystem(pkg, Collections.emptyMap()).getPath(packagePath);
            }
         } else {
            root = Paths.get(pkg);
         }

         try (final Stream<Path> allPaths = Files.walk(root)) {
            // Filter classes having the same package.
            return allPaths
                  .filter(file -> file.toString().endsWith(extension) && file.toString().startsWith(packagePath))
                  .map(file -> getClass(
                        file.toString().substring(packagePath.length() + 1, file.toString().lastIndexOf(extension)),
                        packageName))
                  .collect(Collectors.toSet());
         }
      } catch (Exception e) {
         logger.error("Got an exception while walking the package location", e);
      }
      return new HashSet<>();
   }

   private Class getClass(String className, String packageName) {
      try {
         return Class.forName(packageName + "." + className);
      } catch (ClassNotFoundException e) {
         logger.errorf("Got an exception while loading class %s in pkg %s", className, packageName, e);
      }
      return null;
   }
}
