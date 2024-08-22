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

import io.github.microcks.client.ApiClient;
import io.github.microcks.client.ApiException;
import io.github.microcks.client.KeycloakClient;
import io.github.microcks.client.api.ConfigApi;
import io.github.microcks.client.model.KeycloakConfig;
import io.github.microcks.operator.api.base.v1alpha1.Microcks;
import io.github.microcks.operator.base.resources.KeycloakConfigSecretDependentResource;
import io.github.microcks.operator.base.resources.KeycloakServiceDependentResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

/**
 * A helper class for retrieving a Keycloak Service Account information and then
 * authenticating to a Keycloak server.
 * @author laurent
 */
public class KeycloakHelper {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private static final String KUBE_SVC_DNS_SUFFIX = ".svc.cluster.local";
   private static final String OIDC_TOKEN_ENDPOINT_SUFFIX = "/protocol/openid-connect/token";

   final KubernetesClient client;
   final ObjectMapper mapper;

   /**
    * Default constructor with injected Kubernetes client.
    * @param client A Kubernetes client for interacting with the cluster
    */
   public KeycloakHelper(KubernetesClient client) {
      this.client = client;
      this.mapper = new ObjectMapper();
   }

   /**
    * Retrive the Keycloak configuration from a Microcks instance.
    * @param completeMicrocks The Microcks instance to get Keycloak configuration from
    * @return KeyloakConfig instance
    * @throws ApiException If an error occurs while connecting to Microcks API
    */
   public KeycloakConfig getKeycloakConfig(Microcks completeMicrocks) throws ApiException {
      ApiClient apiClient = new ApiClient();
      apiClient.updateBaseUri("http://" + completeMicrocks.getMetadata().getName() + "."
            + completeMicrocks.getMetadata().getNamespace() + KUBE_SVC_DNS_SUFFIX + ":8080/api");
      logger.infof("Connecting to Microcks on '%s'", apiClient.getBaseUri());

      ConfigApi configApi = new ConfigApi(apiClient);
      return configApi.getKeycloakConfig();
   }

   /**
    * Get an OAuth token for a given resource metadata and Microcks instance. Depending on Keycloak config
    * and the target Microcks instance this will either use a service account secret or a default service account.
    * It will then authenticate to the Keycloak server token endpoint and return the OAuth token.
    * @param resourceMetadata The metadata of the resource that relates to a Microcks instance
    * @param completeMicrocks The Microcks instance that relates to the resource
    * @return An OAuth token than can be used as a bearer when calling Microcks API secured endpoints
    * @throws UnsatisfiedRequirementException If a configuration requirement is not satisfied
    * @throws ApiException If an error occurs while connecting to Microcks API
    * @throws IOException If the underlying http connection fails
    */
   public String getOAuthToken(ObjectMeta resourceMetadata, Microcks completeMicrocks)
         throws UnsatisfiedRequirementException, ApiException, IOException {
      KeycloakConfig keycloakConfig = getKeycloakConfig(completeMicrocks);

      String oauthToken;
      if (Boolean.TRUE.equals(keycloakConfig.getEnabled())) {
         ServiceAccountAndCredentials saAndCredentials = getServiceAccountAndCredentials(resourceMetadata, completeMicrocks);

         String keycloakEndpoint = getKeycloakEndpoint(completeMicrocks, keycloakConfig);
         logger.infof("Using keycloakEndpoints: %s", keycloakEndpoint);
         oauthToken = KeycloakClient.connectAndGetOAuthToken(
               saAndCredentials.getServiceAccountName(),
               saAndCredentials.getServiceAccountCredentials(),
               keycloakEndpoint);
         logger.info("Authentication to Keycloak server succeed!");

      } else {
         oauthToken = "<anonymous-admin-token>";
         logger.info("Keycloak protection is not enabled, using a fake token");
      }

      return oauthToken;
   }

   /** */
   private ServiceAccountAndCredentials getServiceAccountAndCredentials(ObjectMeta resourceMetadata, Microcks completeMicrocks) throws UnsatisfiedRequirementException {
      Map<String, String> annotations = resourceMetadata.getAnnotations();
      String serviceAccountSecret = annotations.get(MicrocksOperatorConfig.SERVICE_ACCOUNT_SECRET_SELECTOR);

      String serviceAccountName = KeycloakConfigSecretDependentResource.OPERATOR_SERVICE_ACCOUNT;
      String serviceAccountCredentials = null;

      if (serviceAccountSecret == null) {
         if (!completeMicrocks.getSpec().getKeycloak().isInstall()) {
            logger.errorf("No service account secret provided for accessing Keycloak from '%s'", resourceMetadata.getName());
            throw new UnsatisfiedRequirementException("No service account secret provided for accessing Keycloak. You must specify a "
                  + MicrocksOperatorConfig.SERVICE_ACCOUNT_SECRET_SELECTOR + " annotation.");
         }

         // We must read the service account secret from the Keycloak realm configuration.
         logger.infof("Using default service account secret to access Keycloak for '%s'", resourceMetadata.getName());
         logger.infof("Looking for '%s' secret in '%s' namespace", KeycloakConfigSecretDependentResource.getSecretName(completeMicrocks),
               resourceMetadata.getNamespace());
         Secret saSecret = client.secrets().inNamespace(resourceMetadata.getNamespace())
               .withName(KeycloakConfigSecretDependentResource.getSecretName(completeMicrocks)).get();

         if (saSecret == null) {
            logger.errorf("No '%s' secret found in '%s' namespace for getting Keycloak info",
                  KeycloakConfigSecretDependentResource.getSecretName(completeMicrocks),
                  resourceMetadata.getName());
            throw new UnsatisfiedRequirementException("No '"
                  + KeycloakConfigSecretDependentResource.getSecretName(completeMicrocks) + "' secret found in '"
                  + resourceMetadata.getName() + "' namespace for getting Keycloak info");
         }

         // Get base64 encoded realm config that needs to be decoded.
         String realmConfig = saSecret.getData().get(KeycloakConfigSecretDependentResource.REALM_CONFIG_KEY);
         realmConfig = new String(Base64.getDecoder().decode(realmConfig));

         serviceAccountCredentials = extractServiceAccountCredentialsFromRealmConfig(completeMicrocks, realmConfig);
      } else {
         // We must read the service account name and credentials from the provided secret.
         logger.infof("Using '%s' service account secret to access Keycloak '%s'", serviceAccountSecret, resourceMetadata.getName());
         Secret saSecret = client.secrets().inNamespace(resourceMetadata.getNamespace())
               .withName(serviceAccountSecret).get();

         serviceAccountName = saSecret.getStringData().get("service-account-name");
         serviceAccountCredentials = saSecret.getStringData().get("service-account-credentials");
      }

      return new ServiceAccountAndCredentials(serviceAccountName, serviceAccountCredentials);
   }

   /** */
   private String extractServiceAccountCredentialsFromRealmConfig(Microcks completeMicrocks, String realmConfig) throws UnsatisfiedRequirementException {
      String serviceAccountCredentials = null;

      JsonNode jsonRealmConfig;
      try {
         jsonRealmConfig = mapper.readTree(realmConfig);
      } catch (JsonProcessingException jpe) {
         logger.errorf("Error while parsing realm configuration: %s", jpe.getMessage());
         throw new UnsatisfiedRequirementException("Error while parsing '"
               + KeycloakConfigSecretDependentResource.REALM_CONFIG_KEY + "' in "
               + KeycloakConfigSecretDependentResource.getSecretName(completeMicrocks));
      }
      if (jsonRealmConfig.has("applications")) {
         JsonNode applicationsNode = jsonRealmConfig.get("applications");
         Iterator<JsonNode> applications = applicationsNode.elements();
         while (applications.hasNext()) {
            JsonNode application = applications.next();
            if (KeycloakConfigSecretDependentResource.OPERATOR_SERVICE_ACCOUNT.equals(application.path("name").asText())) {
               serviceAccountCredentials = application.get("secret").asText();
               break;
            }
         }
      }

      // Throw an exception if we didn't find the service account credentials.
      if (serviceAccountCredentials == null) {
         logger.errorf("%s does not contains secret for service account '%s'",
               KeycloakConfigSecretDependentResource.REALM_CONFIG_KEY,
               KeycloakConfigSecretDependentResource.OPERATOR_SERVICE_ACCOUNT);
         throw new UnsatisfiedRequirementException("No service account secret found in '"
               + KeycloakConfigSecretDependentResource.REALM_CONFIG_KEY + "' in "
               + KeycloakConfigSecretDependentResource.getSecretName(completeMicrocks));
      }
      return serviceAccountCredentials;
   }

   /** */
   private String getKeycloakEndpoint(Microcks completeCR, KeycloakConfig config) {
      if (completeCR.getSpec().getKeycloak().isInstall()) {
         return "http://" + KeycloakServiceDependentResource.getServiceName(completeCR) + "." + completeCR.getMetadata().getNamespace()
               + KUBE_SVC_DNS_SUFFIX + ":8080/realms/" + config.getRealm() + OIDC_TOKEN_ENDPOINT_SUFFIX;
      } else if (completeCR.getSpec().getKeycloak().getPrivateUrl() != null) {
         return completeCR.getSpec().getKeycloak().getPrivateUrl() + "/realms/"
               + config.getRealm() + OIDC_TOKEN_ENDPOINT_SUFFIX;
      }
      return config.getAuthServerUrl() + "/realms/" + config.getRealm() + OIDC_TOKEN_ENDPOINT_SUFFIX;
   }

   /** Thin wrapper class to hold service account name and credentials. */
   private static class ServiceAccountAndCredentials {
      final String serviceAccountName;
      final String serviceAccountCredentials;

      public ServiceAccountAndCredentials(String serviceAccountName, String serviceAccountCredentials) {
         this.serviceAccountName = serviceAccountName;
         this.serviceAccountCredentials = serviceAccountCredentials;
      }

      public String getServiceAccountName() {
         return serviceAccountName;
      }
      public String getServiceAccountCredentials() {
         return serviceAccountCredentials;
      }
   }
}
