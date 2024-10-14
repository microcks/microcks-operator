
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

| Property        | Description                                                                                                                                                                                                                                                                                                                      |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `install`       | **Optional**. Flag for Keycloak installation. Default is `true`. Set to `false` if you want to reuse an existing Keycloak instance.                                                                                                                                                                                              |
| `image`         | **Optional**. The Keycloak container image to use. Default depends on Microcks version. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section.                                                                                                                          |
| `url`           | **Mandatory on Kube if keycloak.install==false, Optional otherwise**. The URL of Keycloak instance - just the hostname + port part. If missing on OpenShift, default URL schema handled by Router is used.                                                                                                                       |
| `privateUrl`    | **Optional but recommended**. A private URL - a full URL here - used by the Microcks component to internally join Keycloak. This is also known as `backend url` in [Keycloak doc](https://www.keycloak.org/server/hostname-deprecated#_backend). When specified, the `keycloak.url` is used as `frontend url` in Keycloak terms. |
| `realm`         | **Optional**. Name of Keycloak realm to use. Should be setup only if `install` is `false` and you want to reuse an existing realm. Default is `microcks`.                                                                                                                                                                        |

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

## MongoDB specification details

This part of the Custom Resource allows you to configure the MongoDB component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property        | Description                                                                                                                                                                                             
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `install`       | **Optional**. Flag for MongoDN installation. Default is `true`. Set to `false` if you want to reuse an existing MongoDB instance.                                                                       |
| `image`         | **Optional**. The Keycloak container image to use. Default depends on Microcks version. <br/> This property if of type `ImageSpec` as explained in [Image specification](#image-specification) section. |
| `uri`           | **Optional**. MongoDB URI in case you're reusing existing MongoDB instance. Mandatory if `install` is `false`.                                                                                          |
| `uriParameters` | **Optional**. Allows you to add parameters to the mongodb uri connection string.                                                                                                                        |
| `database`      | **Optional**. MongoDB database name in case you're reusing existing MongoDB instance. Used if `install` is `false`. Default to `appName`.                                                                                                                                                                             |
| `secretRef`     | **Optional**. Reference of a Secret containing credentials for connecting a provided MongoDB instance. Mandatory if `install` is `false`.                                                                                                                                                                             |

### Resources configuration

When installed by this Operator, resources assigned to MongoDB pod can be configured using regular Kubernetes resources definition.
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

| Property            | Description                                                                                                                             |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `persistent`        | **Optional**. Flag for MongoDB persistence. Default is `true`. Set to `false` if you want an ephemeral MongoDB installation.            |
| `volumeSize`        | **Optional**. Size of persistent volume claim for MongoDB. Default is `2Gi`. Not used if not persistent install asked.                  |
| `storageClassName`  | **Optional**. The cluster storage class to use for persistent volume claim. If not specified, we rely on cluster default storage class. |

## Postman specification details

This part of the Custom Resource allows you to configure the Postman runtime component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property    | Description                                                                                                                                                                                                    
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

### AsyncAPI support

### Repository Filtering

### Repository Tenancy

### Hub Integration
