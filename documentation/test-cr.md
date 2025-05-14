
## Overview

The `Test` Custom Resource (CR) allows you to launch tests in Microcks. This resource allows
you to define the specification of a conformance test to be run into a Microcks instance.
`Test` Custom Resource Definition is currently defined using the `microcks.io/v1alpha1` API version. To access
the full schema definition you may want to check the
[tests.microcks.io-v1.yml](../deploy/crd/tests.microcks.io-v1.yml) file.

The `Test` is a [Microcks-dependent resource](./microcks-dependent-cr.md) as it needs to have a Microcks
instance to be managed by the operator first.

At a higher level, a `Test` resource is organized using the following structure:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Test
metadata:
  name: tests-apipastries-01
  annotations:
    microcks.io/instance: microcks
spec:
  serviceId: "API Pastries:0.0.1"
  testEndpoint: http://apipastries-app-01:3001
  runnerType: OPEN_API_SCHEMA
  timeout: 5000
  retentionPolicy: Retain
```

`spec` just allows you to define the test specification details.

Once created in your namespace, you can easily list the existing secret sources using:

```yaml
$ kubectl get tests -n m2
NAME                   AGE
tests-apipastries-01   21h
tests-apipastries-02   21h
```

and get access to a specific instance details using `kubectl get tests/tests-apipastries-01 -n m2 -o yaml`.
The operator tracks the status of the reconciliation using the following structure in the `status` field
of the resource:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Test
metadata:
  name: tests-apipastries-01
  annotations:
    microcks.io/instance: microcks
spec:
  [...]
status:
  id: 682313fd882e7c7ae7ee8da2
  url: https://microcks.m2.minikube.local/#/tests/682313fd882e7c7ae7ee8da2
  observedGeneration: 0
  result: FAILURE
  status: READY
```

The `status.id` field contains the unique identifier of the test in the Microcks instance.
You can then later use this `id` with the [Microcks API](https://microcks.io/documentation/references/apis/open-api/) to fetch all the test details.
`status.url` offers direct access to the Microcks UI to display all the test result details.

The `status.status` field tracks the status of the Kubernetes reconciliation process. 
`READY` means that the operator has finished reconciliating the observed generation of the `Test`. 

The `status.result` field tracks the result of the test execution on the Microcks instance. It can be one of the following values: `IN_PROGRESS`, `SUCCESS`, or `FAILURE`.

## Test specification details

These are the test specification properties you can use to define your `Test` resource. If your familiar with Microcks tests,
you'll realize that these are the same concepts you'll find in [test parameters](https://microcks.io/documentation/references/test-endpoints/) documentation.

| Property             | Description                                                                                                                                                                                                                                                                 |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `serviceId`          | **Mandatory**. The unique identifier of the service to be tested. It is composed of `<service-name>:<service-version>`.                                                                                                                                                     |
| `testEndpoint`       | **Mandatory**. The endpoint of the service to be tested. It must follow the [endpoint syntax](https://microcks.io/documentation/references/test-endpoints/#endpoints-syntax).                                                                                               |
| `runnerType`         | **Mandatory**. The type of runner to use for executing the test. It must be one in [this list](https://microcks.io/documentation/references/test-endpoints/#test-runner).                                                                                                   |
| `timeout`            | **Optional**. The timeout in milliseconds for the test execution. The default value is `5000`.                                                                                                                                                                              |
| `secretRef`          | **Optional**. Reference to a Microcks Secret for accessing the test endpoint                                                                                                                                                                                                |
| `filteredOperations` | **Optional**. A list of the operation names to include in th test. It not specified, all the service operations are considered.                                                                                                                                             |
| `operationHeaders`   | **Optional**. A map of headers to add to or override existing one. Keys are the operation name or `global` if apply to all. Values are simple key:value pairs of headers.                                                                                                   |
| `oAuth2Context`      | **Optional**. If the secured Test Endpoint cannot be accessed using a static Authentication Secret, Microcks is able to handle an OAuth2 / OpenID Connect authentication flow as the Tests prerequisites in order to retrieve an ephemeral bearer token. See details below. |
| `retentionPolicy`    | **Optional**. The retention policy to use for the Test CR in Kubernetes. It can be one of `Retain`, `Delete`, or `DeleteOnSuccess`. The default value is `Retain`. The operator keep of deleted the `Test` resource accordingly after the end of the reconciliation.        |

The `retentionPolicy` property is used to define the behavior of the operator when the test is completed on the Microcks instance.
* The default value is `Retain`, which means that the operator will keep the test resource in Kubernetes even after it has been completed.
* The `Delete` value means that the operator will delete the test resource from Kubernetes after it has been completed, whatever the result.
* The `DeleteOnSuccess` value means that the operator will delete the test resource from Kubernetes only if the test has been completed successfully.

When coupled with an event-based workflow (like [Argo Events](https://argoproj.github.io/events/) for example), this allows you to automatically
trigger  the next step of your pipeline without keeping the `Test` resource history in Kubernetes.

## Example

Here is a full example below:

```yaml
apiVersion: microcks.io/v1alpha1
kind: Test
metadata:
  name: tests-apipastries-02
  annotations:
    microcks.io/instance: microcks
spec:
  serviceId: "API Pastries:0.0.1"
  testEndpoint: http://apipastries-app-02:3002
  runnerType: OPEN_API_SCHEMA
  timeout: 5000
  filteredOperations:
    - 'GET /pastries'
    - 'GET /pastries/{name}'
  operationHeaders:
    global:
      x-trace-id: 1234567890
    'GET /pastries':
      x-provenance: tests-apipastries-02
  oAuth2Context:
    clientId: test-client
    clientSecret: test-secret
    tokenUrl: http://oauth2-server/realm/my-realm/openid/token
  retentionPolicy: DeleteOnSuccess
```