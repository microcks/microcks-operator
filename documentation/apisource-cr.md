
## Overview

The `APISource` Custom Resource (CR) allows you to define mocks and importers in Microcks. This resources allows
you to define the artifacts to be imported or some importers to be used to import them and keep them synchronized.
`APISource` Custom Resource Definition is currently defined using the `microcks.io/v1alpha1` API version. To access
the full schema definition you may want to check the
[apisources.microcks.io-v1.yml](../deploy/crd/apisources.microcks.io-v1.yml) file.

The `APISource` is a [Microcks-dependent resource](./microcks-dependent-cr.md) as it needs to have a Microcks
instance to be managed by the operator first.

At a higher level, a `APISource` resource is organized using the following structure:

```yaml
apiVersion: microcks.io/v1alpha1
kind: APISource
metadata:
  name: tests-artifacts
  annotations:
    microcks.io/instance: microcks
spec:
  artifacts:
    - <artifact-specification-details>
    - <artifact-specification-details>
  importers:
    - <importer-specification-details>
    - <importer-specification-details>
```

* `spec.artifacts` contains one or more artifact specification details.
* `spec.importers` contains one or more importer specification details.

Once created in your namespace, you can easily list the existing secret sources using:

```yaml
$ kubectl get apisources -n m2
NAME             AGE
tests-artifacts   1d
```

and get access to a specific instance details using `kubectl get apisources/tests-artifacts -n m2 -o yaml`.
The operator tracks the status of the reconciliation using the following structure in the `status` field
of the resource:

```yaml
kind: APISource
metadata:
  annotations:
    microcks.io/instance: microcks
spec:
  [...]
status:
  conditions:
  - lastTransitionTime: "2024-10-31T22:11:14Z"
    message: API Pastry - 2.0:2.0.0
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/APIPastry-openapi.yaml
  - lastTransitionTime: "2024-10-31T22:11:15Z"
    message: io.github.microcks.grpc.hello.v1.HelloService:v1
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/hello-v1.proto
  - lastTransitionTime: "2024-10-31T22:11:15Z"
    message: io.github.microcks.grpc.hello.v1.HelloService:v1
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/HelloService.metadata.yml
  - lastTransitionTime: "2024-10-31T22:11:16Z"
    message: io.github.microcks.grpc.hello.v1.HelloService:v1
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/HelloService.postman.json
  - lastTransitionTime: "2024-10-31T22:11:16Z"
    message: Movie Graph API:1.0
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/films.graphql
  - lastTransitionTime: "2024-10-31T22:11:16Z"
    message: Movie Graph API:1.0
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/films-metadata.yml
  - lastTransitionTime: "2024-10-31T22:11:16Z"
    message: Movie Graph API:1.0
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/films-postman.json
  - lastTransitionTime: "2024-10-31T22:11:17Z"
    message: 67240085de8ad417f5cb4318
    status: READY
    type: https://raw.githubusercontent.com/microcks/microcks/master/samples/HelloService-soapui-project.xml
  observedGeneration: 0
  status: READY
```

Basically, one `condition` is created per `artifact` and `importer` specification to track the reconciliation result and
the global status  is made available via the `status.status` field. The `type` field of the condition represents the
artifact url in Microcks instance and the `message` field represents the API or importer unique identifier.

`importers` and Mock API discovered from `aritfacts` imported in Microcks are -by default- deleted when the custom resource 
is deleted. This behavior can be changed by setting the `keepAPIOnDelete` property to `true` in the `spec` section.

## Artifact specification details

Direct artifact declaration specification:

| Property       | Description                                                                                                                                                                                                                    |
|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `url`          | **Mandatory**. The URL of artifact to import into Microcks instance.                                                                                                                                                           |
| `mainArtifact` | **Optional**. Whether this artifact should be considered as a main/primary one oar as a secondary one. See [Multi-artifacts](https://microcks.io/documentation/explanations/multi-artifacts/) explanations. Default is `true`. |
| `secretRef`    | **Optional**. An optional Secret that can be used to fetch the artifact URL. See [`SecretSource`](./secretsource-cr.md) custom resource and [Microcks Secrets](https://microcks.io/documentation/guides/administration/secrets/) explanations.                                                 |

## Importer specification details

Importer declaration specification:

| Property                          | Description                                                                                                                                                                                                                                    |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mainArtifact`                    | **Optional**. Whether this artifact should be considered as a main/primary one oar as a secondary one. See [Multi-artifacts](https://microcks.io/documentation/explanations/multi-artifacts/) explanations. Default is `true`.                 |
| `active`                          | **Optional**. Whether this importer should be immediately activated. Default is `true`.                                                                                                                                                        |
| `repository.url`                  | **Mandatory**. The URL of artifact to import into Microcks instance.                                                                                                                                                                           |
| `repository.disableSSLValidation` | **Optional**. Default is `false`.                                                                                                                                                                                                              |
| `repository.secretRef`            | **Optional**. An optional Secret that can be used to fetch the artifact URL. See [`SecretSource`](./secretsource-cr.md) custom resource and [Microcks Secrets](https://microcks.io/documentation/guides/administration/secrets/) explanations. |
| `labels`                          | **Optional**. A map of labels to set on created importer.                                                                                                                                                                                      |

## Example

Here is a full example below:

```yaml
apiVersion: microcks.io/v1alpha1
kind: APISource
metadata:
  name: tests-artifacts
  annotations:
    microcks.io/instance: microcks
spec:
  keepAPIOnDelete: false
  artifacts:
    - url: https://raw.githubusercontent.com/microcks/microcks/master/samples/APIPastry-openapi.yaml
      mainArtifact: true
    - url: https://raw.githubusercontent.com/microcks/microcks/master/samples/hello-v1.proto
      mainArtifact: true
    - url: https://raw.githubusercontent.com/microcks/microcks/master/samples/HelloService.metadata.yml
      mainArtifact: false
    - url: https://raw.githubusercontent.com/microcks/microcks/master/samples/HelloService.postman.json
      mainArtifact: false
  importers:
    - name: Hello Soap Service
      mainArtifact: true
      active: false
      repository:
        url: https://raw.githubusercontent.com/microcks/microcks/master/samples/HelloService-soapui-project.xml
      labels:
        domain: authentication
        status: GA
        team: Team A
```