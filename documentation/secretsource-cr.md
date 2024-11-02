
## Overview

The `SecretSource` Custom Resource (CR) allows you to define secrets in Microcks. Those secrets can be directly specified
in the resource or synchronized from Kubernetes `Secrets` existing in the same namespace.
`SecretSource` Custom Resource Definition is currently defined using the `microcks.io/v1alpha1` API version. To access
the full schema definition you may want to check the 
[secretsources.microcks.io-v1.yml](../deploy/crd/secretsources.microcks.io-v1.yml) file.

The `SecretSource` is a [Microcks-dependent resource](./microcks-dependent-cr.md) as it needs to have a Microcks 
instance to be managed by the operator first.

At a higher level, a `SecretSource` resource is organized using the following structure:

```yaml
apiVersion: microcks.io/v1alpha1
kind: SecretSource
metadata:
  name: tests-secrets
  annotations:
    microcks.io/instance: microcks
spec:
  secrets:
    - <secret-specification-details>
    - <secret-specification-details>
```

`spec.secrets` contains one or more secret specification details.

Once created in your namespace, you can easily list the existing secret sources using:

```yaml
$ kubectl get secretsources -n m2
NAME            AGE
tests-secrets    1d
```

and get access to a specific instance details using `kubectl get secretsources/tests-secrets -n m2 -o yaml`.
The operator tracks the status of the reconciliation using the following structure in the `status` field
of the resource:

```yaml
apiVersion: microcks.io/v1alpha1
kind: SecretSource
metadata:
  annotations:
    microcks.io/instance: microcks
spec:
  [...]
status:
  status: READY
  observedGeneration: 0
  conditions:
    - lastTransitionTime: "2024-09-14T04:58:55Z"
      message: 66ca3b482a11675200f87792
      status: READY
      type: my-secret
    - lastTransitionTime: "2024-09-14T04:58:55Z"
      message: 66ca3b482a11675200f87793
      status: READY
      type: my-secret-2
```

Basically, one `condition` is created per secret specification to track the reconciliation result and the global status
is made available via the `status.status` field. The `type` field of the condition represents the secret name in Microcks
instance and the `message` field represents its unique identifier.

## Secret specification details

Direct secret declaration specification:

| Property      | Description                                                                                                                 |
|---------------|-----------------------------------------------------------------------------------------------------------------------------|
| `name`        | **Mandatory**. The name of the secret to create in the Microcks target instance.                                            |
| `description` | **Mandatory**. The description of the secret to create in the Microcks target instance.                                     |
| `username`    | **Optional**. A username for a secret holding basic authentication information.                                             |
| `password`    | **Optional**. A password for a secret holding basic authentication information.<br/> Must be provided with `username`       |
| `token`       | **Optional**. A token for a secret holding token-based authentication information.                                          |
| `tokenHeader` | **Optional**. A header that will be used to transport the token of a secret holding token-based authentication information. |
| `caCertPem`   | **Optional**. A certificate or certificate chain in PEM format for a secret holding TLS authentication information.         |


Kubernetes `Secret` resource synchronization specification:

| Property                    | Description                                                                                                    |
|-----------------------------|----------------------------------------------------------------------------------------------------------------|
| `name`                      | **Mandatory**. The name of the secret to create in the Microcks target instance.                               |
| `description`               | **Mandatory**. The description of the secret to create in the Microcks target instance.                        |
| `valuesFrom`                | **Mandatory**. The origin and specification of secret synchronisation                                          |
| `valuesFrom.secretRef`      | **Mandatory**. The name of the source Kubernetes `Secret` to synchronize in Microcks                           |
| `valuesFrom.usernameKey`    | **Optional**. The `Secret` key containing a username for a secret holding basic authentication information.    |
| `valuesFrom.passwordKey`    | **Optional**. The `Secret` key containing a password for a secret holding basic authentication information.    |
| `valuesFrom.tokenKey`       | **Optional**. The `Secret` key containing a token for a secret holding token-based authentication information. |
| `valuesFrom.tokenHeaderKey` | **Optional**. The `Secret` key containing a header that will be used to transport the token of a secret holding token-based authentication information. |
| `valuesFrom.caCertPemKey`   | **Optional**. The `Secret` key containing a certificate or certificate chain in PEM format for a secret holding TLS authentication information.         |

## Example

Here is a full example below:

```yaml
apiVersion: microcks.io/v1alpha1
kind: SecretSource
metadata:
  name: tests-secrets
  annotations:
    microcks.io/instance: microcks
spec:
  secrets:
    - name: my-secret
      description: My secret description
      username: my-username
      password: my-password
      token: my-token
      tokenHeader: my-token-header
      caCertPem: |
        ----BEGIN CERTIFICATE-----
        SGVsbG8gZXZlcnlvbmUgYW5kIHdlbGNvbWUgdG8gTWljcm9ja3Mh
        ----END CERTIFICATE-----
    - name: my-secret-2
      description: My secret description 2
      valuesFrom:
        secretRef: microcks-keycloak-admin
        usernameKey: username
        passwordKey: password
```