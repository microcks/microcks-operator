
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
  namepsace: m2
spec:
  version: 1.10.0
  [...]
statuc:
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
  [...]
  commonLabels:
    acme.com/team: Team A
    acme.com/billing-id: 123-456-789
  commonAnnotations:
    acme.com/custom-annotation: my-custom-value
    acme.com/other-custom-annotation: my-other-custom-value
```

## Microcks specification details

This part of the Custom Resource allows you to configure the Microcks web app component deployment as described
into the [Architecture & deployment options](https://microcks.io/documentation/explanations/deployment-options/).

### Basic configuration

| Property      | Description                          |
|---------------|--------------------------------------|
| `url`         | **Mandatory on Kube, Optional on OpenShift**. The URL to use for exposing `Ingress`. <br/>If missing on OpenShift, default URL schema handled by Router is used. |  
| `replicas`    | **Optional**. The number of replicas for the Microcks main pod. Default is `1`. |

### Ingresses configuration

Microcks web app component is by default exposed to the outer workd using Kubernetes `Ingresses`.
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
  [...]
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
  [...]
  microcks:
    resources:
      requests:
        cpu: 200m
        memory: 512Mi
      limits:
        memory: 512Mi
```

## Keycloak specification details

## MongoDB specification details

## Postman specification details

## Features specification details