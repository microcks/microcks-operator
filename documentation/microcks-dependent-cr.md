
## Overview

Microcks-dependent resources are Kubernetes Custom Resources that needs the Operator to interact with
a Microcks instance. This target instance must have been previously provisioned by the Operator in the same
Kubernetes namespace.

In order to specify this target instance, such custom resources use the specific `microcks.io/instance` to
provide the name of the instance it relates to. Here is below the example of a [`SecretSource` resource](./secretsource-cr.md)
that targets the instance named `microcks`.

```yaml
apiVersion: microcks.io/v1alpha1
kind: SecretSource
metadata:
  name: tests-secrets
  annotations:
    microcks.io/instance: microcks
spec:
  [...]
```

The next sections provide explanations on how the operator securely connects to the instance.

## Instance connection flow

Following diagram represents the connection flow to the target Microcks instance that happens during the reconciliation
of a Microcks-dependent resource:

```mermaid
sequenceDiagram
    
```

This flow goes as follow:
1) The operator checks that a `microcks.io/instance` annotation is actually defined on the resource,
> If not present, the reconciliation stops, is marked with the `ERROR` status and will not be rescheduled.
2) The operator requests the Kube API server to get the details of the specified instance,
> If it does not exist, the reconciliation stops, is marked with the `ERROR` status and will not be rescheduled.
3) The operator checks that the Microcks instance as the `READY` status.
> If not ready, the reconciliation stops, is marked with the `ERROR` status and will be rescheduled after 5 seconds.
4) The operator retrieves the Keycloak configuration calling the Microcks instance API
5) If Keycloak is enabled, the operator retrieves the **Service Account** and associated credentials, authenticates to
Keycloak and retrieves an **OAuth token**,
6) The operator starts resources reconciliation, creating/updating/deleting resources on the Microcks instance using its
API with authenticated/authorized service account token.

## Service Account and credentials retrieval

As the previous description of the connection flow to the Microcks instance may give a good overview, it does not
provide details on how Service Account and its credentials retrieval is done. There are 2 different configuration to support:
* When Keycloak has been installed by the Operator,
* When your reuse an external Keycloak instance.

### 1. Using Keycloak installed by the Operator

In this situation, when reconciling a `Microcks` resource, the operator creates a specific Keycloak Service Account
named `microcks-operator-serviceaccount` and dedicated to its own usage. This Service Account is granted the Microcks' 
**administrator role** so that the operator can later on manage all resources. As the Keycloak configuration is also
stored in a `Secret`, this secret is retrieved by the Operator to read the dynamically generated credentials.

If you don't want to use the operator for managing Microcks-dependent resources, you can disable this mechanism
by specifying `spec.keycloak.operatorServiceAccountEnabled: false` in the Microcks custom resource.

### 2. Using an external Keycloak instance

In this scenario, you will need to provision by yourself a Service Account that is granted the Microcks'
**administrator role** on your Keycloak instance. Then, you will have to put both informations - the service account 
name and its credentials - into a Kubernetes `Secret` into the same namespace.

This secrete will need to have -at least- two keys:
* `service-account-name` will hold the value of the Service Account name to use,
* `service-account-credentials` will hold the corresponding credentials to authenticate on your target Keycloak instance and realm.

The last thing is how to tell the Microcks operator the secret to be used for Service Account retrievel? Well this
is done using an additional annotation on your Microcks-dependent resource: the `microcks.io/service-account-secret`
annotation allows you to specify the name of secret to consider:

```yaml
apiVersion: microcks.io/v1alpha1
kind: SecretSource
metadata:
  name: tests-secrets
  annotations:
    microcks.io/instance: microcks
    microcks.io/service-account-secret: my-keyclok-sa-secret-for-operator
spec:
  [...]
```
