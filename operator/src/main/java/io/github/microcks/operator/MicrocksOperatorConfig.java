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
 * Configuration properties and constants for the Microcks operator.
 * @author laurent
 */
public class MicrocksOperatorConfig {

   /** Name of the Microcks operator. */
   public static final String OPERATOR_NAME = "microcks-operator";

   /** Label value for selecting resources managed by operator. */
   public static final String RESOURCE_LABEL_SELECTOR = "app.kubernetes.io/managed-by=" + OPERATOR_NAME;

   /** Label value for the 'group' value. */
   public static final String GROUP_LABEL_VALUE = "microcks";

   /** Annotation for specifying a Microcks instance the current resource relates to. */
   public static final String INSTANCE_SELECTOR = "microcks.io/instance";
   /** Annotation for specifying a Service Account secret for accessing a relative Microcks instance. */
   public static final String SERVICE_ACCOUNT_SECRET_SELECTOR = "microcks.io/service-account-secret";
}
