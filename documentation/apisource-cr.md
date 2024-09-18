
## Overview

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

## Artifact specification details

| Property       | Description                      |
|----------------|----------------------------------|
| `url`          | **Mandatory**.                   |
| `mainArtifact` | **Optional**. Default is `true`. |
| `secretRef`    | **Optional**.                    |

## Importer specification details

| Property                          | Description                       |
|-----------------------------------|-----------------------------------|
| `mainArtifact`                    | **Optional**. Default is `true`.  |
| `active`                          | **Optional**. Default is `true`.  |
| `repository.url`                  | **Mandatory**.                    |
| `repository.disableSSLValidation` | **Optional**. Default is `false`. |
| `repository.secretRef`            | **Optional**.                     |
| `labels`                          | **Optional**.                     |

## Example

```yaml
apiVersion: microcks.io/v1alpha1
kind: APISource
metadata:
  name: tests-artifacts
  annotations:
    microcks.io/instance: microcks
spec:
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