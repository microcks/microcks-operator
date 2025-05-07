
## Overview

The `Microcks` Custom Resource (CR) allows you to deploy Microcks instance in the Kubernetes namespaces 
it has been created in. `Microcks` Custom Resource Definition is currently defined using the `microcks.io/v1alpha1`
API version. To access the full schema definition you may want to check the
[microckses.microcks.io-v1.yml](../deploy/crd/microckses.microcks.io-v1.yml) file. 

At a higher level, a `Microcks` resource is organized using the following structure:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  version: 1.10.0
  microcks:
    <microcks-specification-details>
  keycloak:
    <keycloak-specification-details>
  mongodb:
    <mongodb-specification-details>
  postman:
    <postman-specification-details>
  features:
    <features-specification-details>
```

Most of the information above are optional. `spec.version` is mandatory to specify the version you want to
deploy as an instance.

> [!WARNING]
> Even most of the properties are optional, some Kubernetes distribution may require some of them to be set.
> As an example, Amazon EKS default EBS storage required the MongoDB `securityContext` to be set to prevent permissions issues.

Once created in your namespace, you can easily list the existing instance using:

```yaml
$ kubectl get microckses -n m2
NAME       AGE
microcks   1d
```

and get access to a specific instance details using `kubectl get microckses/microcks -n m2 -o yaml`.
The operator tracks the status of the reconciliation using the following structure in the `status` field 
of the resource:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
  namespace: m2
spec:
  version: 1.10.0
  #[...]
status:
  status: READY
  observedGeneration: 0
  microcksUrl: microcks.m2.minikube.local
  keycloakUrl: keycloak.m2.minikube.local
  condtions:
  - lastTransitionTime: "2024-09-13T07:09:02Z"
    status: DEPLOYING
    type: MicrocksDeploying
  - lastTransitionTime: "2024-09-13T07:10:43Z"
    status: READY
    type: MicrocksReady
  [...]
```

Basically, one or more `conditions` are created per component that is deployed (based on the specification details)
to track the progress and the global status is made available via the `status.status` field.

The `status.microcksUrl` and `status.keycloakUrl` are made available to retrieve the exposed endpoints for those
two components.

Beside the `labels` and `annotations` used for internal purposes, you can specify additional ones
for your own needs using the `commonLabels` and `commonAnnotations` like illustrated below. Those labels
and annotations will be added to all the resources managed by the Operator.

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  commonLabels:
    acme.com/team: Team A
    acme.com/billing-id: 123-456-789
  commonAnnotations:
    acme.com/custom-annotation: my-custom-value
    acme.com/other-custom-annotation: my-other-custom-value
```

Kubernetes scheduling can be customized for the different dependent `Deployments` creates by the Operator.
For that, you can use the `commonAffinities` and `commonTolerations` properties like illustrated below:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  commonAffinities:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
          - matchExpressions:
            - key: topology.kubernetes.io/zone
              operator: In
              values:
                - antarctica-east1
                - antarctica-west1
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 1
          preference:
            matchExpressions:
              - key: another-node-label-key
                operator: In
                values:
                  - another-node-label-value
  commonTolerations:
    - key: "key1"
      operator: "Equal"
      value: "value1"
      effect: "NoSchedule"
    - key: "key1"
      operator: "Equal"
      value: "value1"
      effect: "NoExecute"
```


## Microcks specification details

This part of the Custom Resource allows you to configure the Microcks web app component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property      | Description                                                                                                                                                      |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `url`         | **Mandatory on Kube, Optional on OpenShift**. The URL to use for exposing `Ingress`. <br/>If missing on OpenShift, default URL schema handled by Router is used. |
| `image`       | **Optional**. The Microcks container image to use. Default is `quay.io/microcks/microcks:<version>`. See note on `ImageSpec` below.                              |
| `replicas`    | **Optional**. The number of replicas for the Microcks main pod. Default is `1`.                                                                                  |

#### Image specification

Microcks container image can be specified using the `image` property. This property is of type `ImageSpec` and is defined as follows:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  microcks:
    image:
      registry: quay.io
      repository: microcks/microcks
      tag: 1.10.1
      #digest: sha256:3ce0494688e973ef45c77faa1812f58eb74aaced849c5f57067af81712e5df72
```

User can override any of `registry`, `repository`, `tag` and `digest` properties. If `digest` is provided, it will take precedence over `tag`.

### Ingresses configuration

Microcks web app component is by default exposed to the outer world using Kubernetes `Ingresses`.
For this component, the operator creates 2 ingresses:
* One `ingress` for HTTP traffic that allows access to UI, APIs and HTTP-based mocks,
* One `ingress` for gRPC traffic that allows access to gRPC mocks.

Each ingress can be configured via a specific property: `ingress` or `grpcIngress`.

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  microcks:
    ingress: <ingress-specification-details>
    grpcIngress: <ingress-specification-details>
```

Both properties follow the same `ingress-specification-details` structure that is described below:

| Property             | Description                                                                                                                                                                                                                                                                                                           |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ingressSecretRef`   | **Optional on Kube, not used on OpenShift**. The name of a TLS Secret for securing `Ingress`. <br/>If missing on Kubernetes, a self-signed certificate is generated.                                                                                                                                                  | 
| `ingressAnnotations` | **Optional**. Some custom annotations to add on `Ingress` or OpenShift `Route`. <br/>If these annotations are triggering a Certificate generation (for example through https://cert-manager.io/ or https://github.com/redhat-cop/cert-utils-operator), the `generateCert` property should be set to `false` for Kube. |
| `generateCert`       | **Optional on Kube, not used on OpenShift**. Whether to generate self-signed certificate or not if no valid `ingressSecretRef` provided. Default is `true`                                                                                                                                                            |

### Resources configuration

Resources assigned to Microcks web app pods can be configured using regular Kubernetes resources definition. 
Here is below an example with the default values that are used by the operator:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  microcks:
    resources:
      requests:
        cpu: 200m
        memory: 512Mi
      limits:
        memory: 512Mi
```

## Keycloak specification details

This part of the Custom Resource allows you to configure the Keycloak web app component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property     | Description                                                                                                                                                                                                                                                                                                                      |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`    | **Optional**. Flag for Keycloak enablement. Default is `true`. Set to `false` if you want to run Microcks without authentication.                                                                                                                                                                                                |
| `install`    | **Optional**. Flag for Keycloak installation. Default is `true`. Set to `false` if you want to reuse an existing Keycloak instance.                                                                                                                                                                                              |
| `image`      | **Optional**. The Keycloak container image to use. Default depends on Microcks version. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section.                                                                                                                          |
| `url`        | **Mandatory on Kube if keycloak.install==false, Optional otherwise**. The URL of Keycloak instance - just the hostname + port part (https is assumed). If missing on OpenShift, default URL schema handled by Router is used.                                                                                                    |
| `privateUrl` | **Optional but recommended**. A private URL - a full URL here - used by the Microcks component to internally join Keycloak. This is also known as `backend url` in [Keycloak doc](https://www.keycloak.org/server/hostname-deprecated#_backend). When specified, the `keycloak.url` is used as `frontend url` in Keycloak terms. |
| `realm`      | **Optional**. Name of Keycloak realm to use. Should be setup only if `install` is `false` and you want to reuse an existing realm. Default is `microcks`.                                                                                                                                                                        |

### Ingresses configuration

When installed by this Operator, Keycloak component is by default exposed to the outer world using Kubernetes `Ingress`, configured via a specific property: `ingress`.

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  keycloak:
    ingress: <ingress-specification-details>
```

This property follow the same `ingress-specification-details` structure that is described below:

| Property             | Description                                                                                                                                                                                                                                                                                                           |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ingressSecretRef`   | **Optional on Kube, not used on OpenShift**. The name of a TLS Secret for securing `Ingress`. <br/>If missing on Kubernetes, a self-signed certificate is generated.                                                                                                                                                  | 
| `ingressAnnotations` | **Optional**. Some custom annotations to add on `Ingress` or OpenShift `Route`. <br/>If these annotations are triggering a Certificate generation (for example through https://cert-manager.io/ or https://github.com/redhat-cop/cert-utils-operator), the `generateCert` property should be set to `false` for Kube. |
| `generateCert`       | **Optional on Kube, not used on OpenShift**. Whether to generate self-signed certificate or not if no valid `ingressSecretRef` provided. Default is `true`                                                                                                                                                            |

### Resources configuration

When installed by this Operator, resources assigned to Keycloak pod can be configured using regular Kubernetes resources definition.
Here is below an example with the default values that are used by the operator:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  keycloak:
    resources:
      requests:
        cpu: 400m
        memory: 512Mi
      limits:
        memory: 512Mi
```

### Persistence configuration

When installed by this Operator, Keycloak component is using a PostgreSQL database for persistence. The persitence related configuration properties are
the following ones:

| Property           | Description                                                                                                                                                                                               |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `persistent`       | **Optional**. Flag for Keycloak persistence. Default is `true`. Set to `false` if you want an ephemeral Keycloak installation.                                                                            |
| `postgresImage`    | **Optional**. The PostgreSQL container image to use. Default depends on Microcks version. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section. |
| `volumeSize`       | **Optional**. Size of persistent volume claim for Keycloak. Default is `1Gi`. Not used if not persistent install asked.                                                                                   |
| `storageClassName` | **Optional**. The cluster storage class to use for persistent volume claim. If not specified, we rely on cluster default storage class.                                                                   |
| `pvcAnnotations`   | **Optional**. A map of annotations that will be added to the `pvc` for the Keycloak PostgreSQL persistence.                                                                                               |

## MongoDB specification details

This part of the Custom Resource allows you to configure the MongoDB component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property        | Description                                                                                                                                                                                             |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `install`       | **Optional**. Flag for MongoDN installation. Default is `true`. Set to `false` if you want to reuse an existing MongoDB instance.                                                                       |
| `image`         | **Optional**. The Keycloak container image to use. Default depends on Microcks version. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section. |
| `uri`           | **Optional**. MongoDB URI in case you're reusing existing MongoDB instance. Mandatory if `install` is `false`.                                                                                          |
| `uriParameters` | **Optional**. Allows you to add parameters to the mongodb uri connection string.                                                                                                                        |
| `database`      | **Optional**. MongoDB database name in case you're reusing existing MongoDB instance. Used if `install` is `false`. Default to `appName`.                                                               |
| `secretRef`     | **Optional**. Reference of a Secret containing credentials for connecting a provided MongoDB instance. Mandatory if `install` is `false`.                                                               |

### Resources configuration

When installed by this Operator, `resources` assigned to MongoDB pod can be configured using regular Kubernetes resources definition.
Here is below an example with the default values that are used by the operator:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  mongodb:
    resources:
      requests:
        cpu: 250m
        memory: 512Mi
      limits:
        #cpu: 500m
        memory: 512Mi
```

### Persistence configuration

When installed by this Operator, MongoDB component is using a persistent volume. The persitence related configuration properties are
the following ones:

| Property           | Description                                                                                                                             |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `persistent`       | **Optional**. Flag for MongoDB persistence. Default is `true`. Set to `false` if you want an ephemeral MongoDB installation.            |
| `volumeSize`       | **Optional**. Size of persistent volume claim for MongoDB. Default is `2Gi`. Not used if not persistent install asked.                  |
| `storageClassName` | **Optional**. The cluster storage class to use for persistent volume claim. If not specified, we rely on cluster default storage class. |
| `pvcAnnotations`   | **Optional**. A map of annotations that will be added to the `pvc` for the MongoDB persistence.                                         |

### SecurityContext configuration

When installed by this Operator, `securityContext` assigned to MongoDB pod can be configured using regular Kubernetes `PodSecurityContext` definition.

> **Note:** Setting this securityContext is typically required on certain Kubernetes distributions like EKS with default EBS storage to
> prevent permissions issues while accessing the underlying persistent volume.

Here is below an example with the default values that matches the current default MongoDB image:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  mongodb:
    securityContext:
      runAsUser: 999
      runAsGroup: 999
      fsGroup: 999
```

## Postman specification details

This part of the Custom Resource allows you to configure the Postman runtime component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property    | Description                                                                                                                                                                                                    |
|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `image`     | **Optional**. The Postman runtime container image to use. Default depends on Microcks version. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section. |
| `replicas`  | **Optional**. The number of replicas for the Microcks Postman pod. Default is `1`.                                                                                                                             |

### Resources configuration

When installed by this Operator, resources assigned to Postman runtime pod can be configured using regular Kubernetes resources definition.
Here is below an example with the default values that are used by the operator:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  postman:
    resources:
      requests:
        memory: 256Mi
      limits:
        memory: 256Mi
```

## Features specification details

This part of the Custom Resource allows you to enable and configure the optional features of Microcks. Here's below the 
structure of the `features` property:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  #[...]
  features:
    async:
      #[...]
    repositoryFilter:
      #[...]
    repositoryTenancy:
      #[...]
    microcksHub:
      #[...]
    aiCopilot:
      #[...]
```

### AsyncAPI support

`features.async` allows you to enable and configure the support of Async related features as described into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

| Property         | Description                                                                                                                                                                                                                                                                      |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `async.enabled`  | **Optional**. Feature allowing to mock an tests asynchronous APIs through Events. Enabling it requires an active message broker. Default is `false`.                                                                                                                             |
| `async.image`    | **Optional**. The Async Minion component container image to use. Default is `quay.io/microcks/microcks-async-minion:<version>`. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section.                                  |
| `async.env`      | **Optional**. Some environment variables to add on `async-minion` container. This should be expressed using [Kubernetes syntax](https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/#define-an-environment-variable-for-a-container). |

#### Kafka feature details

Here are below the configuration properties of the Kafka support feature:

| Section                      | Property                        | Description                                                                                                                                                                                                            |
|------------------------------|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `async.kafka`                | `install`                       | **Optional**. Flag for Kafka installation. Default is `true` and require Strimzi Operator to be setup. Set to `false` if you want to reuse an existing Kafka instance.                                                 |
| `async.kafka`                | `url`                           | **Optional**. The URL of Kafka broker if it already exists or the one used for exposing Kafka `Ingress` when we install it. In this later case, it should only be the subdomain part (eg: `apps.example.com`).         |
| `async.kafka`                | `ingressClassName`              | **Optional**. The ingress class to use for exposing broker to the outer world when installing it. Default is `nginx`.                                                                                                  |
| `async.kafka`                | `persistent`                    | **Optional**. Flag for Kafka persistence. Default is `false`. Set to `true` if you want a persistent Kafka installation.                                                                                               |
| `async.kafka`                | `volumeSize`                    | **Optional**. Size of persistent volume claim for Kafka. Default is `2Gi`. Not used if not persistent install asked.                                                                                                   |
| `async.kafka.schemaRegistry` | `url`                           | **Optional**. The API URL of a Kafka Schema Registry. Used for Avro based serialization                                                                                                                                |
| `async.kafka.schemaRegistry` | `confluent`                     | **Optional**. Flag for indicating that registry is a Confluent one, or using a Confluent compatibility mode. Default to `true`                                                                                         |
| `async.kafka.schemaRegistry` | `username`                      | **Optional**. Username for connecting to the specified Schema registry. Default to ``                                                                                                                                  |
| `async.kafka.schemaRegistry` | `credentialsSource`             | **Optional**. Source of the credentials for connecting to the specified Schema registry. Default to `USER_INFO`                                                                                                        |
| `async.kafka.authentication` | `type`                          | **Optional**. The type of authentication for connecting to a pre-existing Kafka broker. Supports `SSL` or `SASL_SSL`. Default to `none`                                                                                |
| `async.kafka.authentication` | `truststoreType`                | **Optional**. For TLS transport, you'll always need a truststore to hold your cluster certificate. Default to `PKCS12`                                                                                                 |
| `async.kafka.authentication` | `truststoreSecretRef`           | **Optional**. For TLS transport, the reference of a Secret holding truststore and its password. Set `secret`, `storeKey` and `passwordKey` properties                                                                  |
| `async.kafka.authentication` | `keystoreType`                  | **Optional**. In case of `SSL` type, you'll also need a keystore to hold your user private key for mutual TLS authentication. Default to `PKCS12`                                                                      |
| `async.kafka.authentication` | `keystoreSecretRef`             | **Optional**. For mutual TLS authentication, the reference of a Secret holding keystore and its password. Set `secret`, `storeKey` and `passwordKey` properties                                                        |
| `async.kafka.authentication` | `saslMechanism`                 | **Optional**. For SASL authentication, you'll have to specify an additional authentication mechanism such as `SCRAM-SHA-512` or `OAUTHBEARER`                                                                          |
| `async.kafka.authentication` | `saslJaasConfig`                | **Optional**. For SASL authentication, you'll have to specify a JAAS configuration line with login module, username and password.                                                                                      |
| `async.kafka.authentication` | `saslLoginCallbackHandlerClass` | **Optional**. For SASL authentication, you may want to provide a Login Callback Handler implementations. This implementation may be provided by extending the main and `async-minion` images and adding your own libs. |

#### MQTT feature details

Here are below the configuration properties of the MQTT support feature:

| Section      | Property   | Description                                                                                                                              |
|--------------|------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `async.mqtt` | `url`      | **Optional**. The URL of MQTT broker (eg: `my-mqtt-broker.example.com:1883`). Default is undefined which means that feature is disabled. |
| `async.mqtt` | `username` | **Optional**. The username to use for connecting to secured MQTT broker. Default to `microcks`.                                          |
| `async.mqtt` | `password` | **Optional**. The password to use for connecting to secured MQTT broker. Default to `microcks`.                                          |

#### WebSocket feature details

Here are below the configuration properties of the WebSocket support feature:

| Section    | Property             | Description                                                                                                                                                                                                                                                                                 |
|------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `async.ws` | `ingressSecretRef`   | **Optional**. The name of a TLS Secret for securing WebSocket `Ingress`. If missing, self-signed certificate is generated.                                                                                                                                                                  |
| `async.ws` | `ingressAnnotations` | **Optional**. A map of annotations that will be added to the `Ingress` for Microcks WebSocket mocks. If these annotations are triggering a Certificate generation (for example through [cert-mamanger.io](https://cert-manager.io/)). The `generateCert` property should be set to `false`. |
| `async.ws` | `generateCert`       | **Optional**. Whether to generate self-signed certificate or not if no valid `ingressSecretRef` provided. Default is `true`                                                                                                                                                                 |

#### AMQP feature details

Here are below the configuration properties of the AMQP support feature:

| Section      | Property   | Description                                                                                                                              |
|--------------|------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `async.amqp` | `url`      | **Optional**. The URL of AMQP broker (eg: `my-amqp-broker.example.com:5672`). Default is undefined which means that feature is disabled. |
| `async.amqp` | `username` | **Optional**. The username to use for connecting to secured AMQP broker. Default to `microcks`.                                          |
| `async.amqp` | `password` | **Optional**. The password to use for connecting to secured AMQP broker. Default to `microcks`.                                          |

#### NATS feature details

Here are below the configuration properties of the NATS support feature:

| Section      | Property   | Description                                                                                                                              |
|--------------|------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `async.nats` | `url`      | **Optional**. The URL of NATS broker (eg: `my-nats-broker.example.com:4222`). Default is undefined which means that feature is disabled. |
| `async.nats` | `username` | **Optional**. The username to use for connecting to secured NATS broker. Default to `microcks`.                                          |
| `async.nats` | `password` | **Optional**. The password to use for connecting to secured NATS broker. Default to `microcks`.                                          |

#### Google PubSub feature details

Here are below the configuration properties of the Google PubSub support feature:

| Section              | Property                  | Description                                                                                                                          |
|----------------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `async.googlepubsub` | `project`                 | **Optional**. The GCP project id of PubSub (eg: `my-gcp-project-347219`). Default is undefined which means that feature is disabled. |
| `async.googlepubsub` | `serviceAccountSecretRef` | **Optional**. The name of a Generic Secret holding Service Account JSON credentiels. Set `secret` and `fileKey` properties.          |

#### Amazon SQS feature details

Here are below the configuration properties of the Amazon SQS support feature:

| Section     | Property               | Description                                                                                                                                                                                                                                                                     |
|-------------|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `async.sqs` | `region`               | **Optional**. The AWS region for connecting SQS service (eg: `eu-west-3`). Default is undefined which means that feature is disabled.                                                                                                                                           |
| `async.sqs` | `credentialsType`      | **Optional**. The type of credentials we use for authentication. 2 options here `env-variable` or `profile`. Default to `env-variable`.                                                                                                                                         |
| `async.sqs` | `credentialsProfile`   | **Optional**. When using `profile` authent, name of profile to use for authenticating to SQS. This profile should be present into a credentials file mounted from a Secret (see below). Default to `microcks-sqs-admin`.                                                        |
| `async.sqs` | `credentialsSecretRef` | **Optional**. The name of a Generic Secret holding either environment variables (set `secret` and `accessKeyIdKey`, `secretAccessKeyKey` and optional `sessionTokenKey` properties) or an AWS credentials file with referenced profile (set `secret` and `fileKey` properties). |
| `async.sqs` | `endpointOverride`     | **Optional**. The AWS endpoint URI used for API calls. Handy for using SQS via [LocalStack](https://localstack.cloud).                                                                                                                                                          |

#### Amazon SNS feature details

Here are below the configuration properties of the Amazon SNS support feature:

| Section     | Property               | Description                                                                                                                                                                                                                                                                     |
|-------------|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `async.sns` | `region`               | **Optional**. The AWS region for connecting SNS service (eg: `eu-west-3`). Default is undefined which means that feature is disabled.                                                                                                                                           |
| `async.sns` | `credentialsType`      | **Optional**. The type of credentials we use for authentication. 2 options here `env-variable` or `profile`. Default to `env-variable`.                                                                                                                                         |
| `async.sns` | `credentialsProfile`   | **Optional**. When using `profile` authent, name of profile to use for authenticating to SQS. This profile should be present into a credentials file mounted from a Secret (see below). Default to `microcks-sns-admin`.                                                        |
| `async.sns` | `credentialsSecretRef` | **Optional**. The name of a Generic Secret holding either environment variables (set `secret` and `accessKeyIdKey`, `secretAccessKeyKey` and optional `sessionTokenKey` properties) or an AWS credentials file with referenced profile (set `secret` and `fileKey` properties). |
| `async.sns` | `endpointOverride`     | **Optional**. The AWS endpoint URI used for API calls. Handy for using SNS via [LocalStack](https://localstack.cloud).                                                                                                                                                          |

> **Note:** Enabling both SQS and SNS features and using `env-variable` credentials type for both, may lead to collision as both clients rely on the
> same environment variables. So you have to specify `credentialsSecretRef` on only one of those two services and be sure that the access key and secret
> access key mounted refers to a IAM account having write access to both services.

### Repository Filtering

`features.repositoryFilter` allows you to enable and configure the organization features as described into [Filtering repository content](https://microcks.io/documentation/guides/administration/organizing-repository/#2-filtering-repository-content).

| Property                      | Description                                                                                        |
|-------------------------------|----------------------------------------------------------------------------------------------------|
| `repositoryFilter.enabled`    | **Optional**. Feature allowing to filter repository content via a main label. Default is `false`.  |
| `repositoryFilter.labelKey`   | **Optional**. The key of main label used for filtering the repository. Default is `domain`         |
| `repositoryFilter.labelLabel` | **Optional**. The display label of the main label used for filtering. Default is `Domain`          |
| `repositoryFilter.labelList`  | **Optional**. The list of label keys to display on the API list screen. Default is `domain,status` |

### Repository Tenancy

`features.repositoryTenancy` allows you to enable and configure the organization features as described into [Segmenting management responsibilities](https://microcks.io/documentation/guides/administration/organizing-repository/#3-segmenting-management-responsibilities).

| Property                                       | Description                                                                                                                       |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `repositoryTenancy.enabled`                    | **Optional**. Feature allowing to segment tenancy of repository content. Default is `false`.                                      |
| `repositoryTenancy.artifactImportAllowedRoles` | **Optional**. The list of roles that are allowed to import new artifact in the repository. Default is `admin,manager,manager-any` |

### Hub Integration

`features.microcksHub` allows you to enable and configure the [Microcks Hub](https://hub.microcks.io) as a standard marketplace within your Microcks instance.

| Property                   | Description                                                                                                |
|----------------------------|------------------------------------------------------------------------------------------------------------|
| `microcksHub.enabled`      | **Optional**. Feature allowing to enable Microcks Hub integration. Default is `true`.                      |
| `microcksHub.allowedRoles` | **Optional**. The list of roles that are allowed to access the Hub. Default is `admin,manager,manager-any` |

### AI Copilot support

`features.aiCopilot` allows you to enable and configure the support of AI Copilot related features as described into the [Enabling the AI Copilot](https://microcks.io/documentation/guides/integration/ai-copilot/).

| Property                   | Description                                                                                                                    |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `aiCopilot.enabled`        | **Optional**. Feature allowing to use AI Copilot assictance. Default is `false`.                                               |
| `aiCopilot.implementation` | **Optional**. The name of the LLM implementation the Copilot is using. Possible implementation is only `openai` at the moment. |

#### OpenAI implementation details

Here are below the configuration properties of the Amazon SNS support feature:

| Section            | Property    | Description                                                                                                                        |
|--------------------|-------------|------------------------------------------------------------------------------------------------------------------------------------|
| `aiCopilot.openai` | `apiKey`    | **Mandatory**. The OpenAI API key to use for authenticating to OpenAI endpoint.                                                    |
| `aiCopilot.openai` | `apiUrl`    | **Optional**. The OpenAI API url. Default to public OpenAI endpoints.                                                              |
| `aiCopilot.openai` | `timeout`   | **Optional**. The timeout in seconds during LLM exchanges. Default depends on Microcks version. Typically `30` at time of writing. |
| `aiCopilot.openai` | `model`     | **Optional**. The model to use on the OpenAI LLM. Default depends on Microcks version. Typically `gpt-3.5` at time of writing.     |
| `aiCopilot.openai` | `maxTokens` | **Optional**. The maximum number of tokens to use during exchanges. Typically `3000` at time of writing.                           |

