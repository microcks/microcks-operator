# Microcks Operator

Kubernetes Operator for easy setup and management of Microcks installs and other entities (using Quarkus undercover 😉)

This Operator is meant to replace the existing [microcks-ansible-operator](https://github.com/microcks/microcks-ansible-operator)
that is kinda hard to maintain and to evolve.

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/microcks/microcks-operator/build-verify.yml?logo=github&style=for-the-badge)](https://github.com/microcks/microcks/actions)
[![Container](https://img.shields.io/badge/dynamic/json?color=blueviolet&logo=docker&style=for-the-badge&label=Quay.io&query=tags[0].name&url=https://quay.io/api/v1/repository/microcks/microcks-operator/tag/?limit=10&page=1&onlyActiveTags=true)](https://quay.io/repository/microcks/microcks-operator?tab=tags)
[![License](https://img.shields.io/github/license/microcks/microcks?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Project Chat](https://img.shields.io/badge/discord-microcks-pink.svg?color=7289da&style=for-the-badge&logo=discord)](https://microcks.io/discord-invite/)
[![Artifact HUB](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/microcks-operator-image&style=for-the-badge)](https://artifacthub.io/packages/search?repo=microcks-operator-image)
[![CNCF Landscape](https://img.shields.io/badge/CNCF%20Landscape-5699C6?style=for-the-badge&logo=cncf)](https://landscape.cncf.io/?item=app-definition-and-development--application-definition-image-build--microcks)

## Build Status

Latest release version is `0.0.1`.

The current development version is `0.0.2-SNAPSHOT`. 

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/microcks/microcks-operator/build-verify.yml?logo=github&style=for-the-badge)](https://github.com/microcks/microcks/actions)

#### Fossa license and security scans

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator.svg?type=shield&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator?ref=badge_shield&issueType=license)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator.svg?type=shield&issueType=security)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator?ref=badge_shield&issueType=security)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator.svg?type=small)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator?ref=badge_small)

#### OpenSSF best practices on Microcks core

[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/7513/badge)](https://bestpractices.coreinfrastructure.org/projects/7513)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/microcks/microcks/badge)](https://securityscorecards.dev/viewer/?uri=github.com/microcks/microcks)

## Community

* [Documentation](https://microcks.io/documentation/tutorials/getting-started/)
* [Microcks Community](https://github.com/microcks/community) and community meeting
* Join us on [Discord](https://microcks.io/discord-invite/), on [GitHub Discussions](https://github.com/orgs/microcks/discussions) or [CNCF Slack #microcks channel](https://cloud-native.slack.com/archives/C05BYHW1TNJ)

To get involved with our community, please make sure you are familiar with the project's [Code of Conduct](./CODE_OF_CONDUCT.md).

## Installation

Assuming you're connected to a Kubernetes cluster as an administrator, you must start installing the CRD in your cluster:

```sh
kubectl apply -f deploy/crd/microckses.microcks.io-v1.yml
kubectl apply -f deploy/crd/apisources.microcks.io-v1.yml
kubectl apply -f deploy/crd/secretsources.microcks.io-v1.yml
```

Then you can install the operator itself in a dedicated namespace -let's say `microcks`- using: 

```sh
kubectl create namespace microcks
kubectl apply -f deploy/operator-jvm.yaml -n microcks
```

## Usage

Once operator is installed, you can create a new `Microcks` Custom Resource (CR) to get a working instance of Microcks.

In below example, we're creating a new `Microcks` CR named `microcks` that will install Microcks `1.10.0`.
You need to customize the two `url` fields to match your environment with DNS names that will be mapped to the Microcks and Keycloak ingresses. 

```sh
cat <<EOF | kubectl apply -f -
apiVersion: microcks.io/v1alpha1
kind: Microcks
metadata:
  name: microcks
spec:
  version: 1.10.0
  microcks:
    url: microcks.m.minikube.local
  keycloak:
    url: keycloak.m.minikube.local
EOF
```

> For comprehensive documentation and examples of `Microcks` CR, please refer to the [Microcks CR documentation](./documentation/microcks-cr.md).

Microcks Operator also provide the `APISource` and `SecretSource` CRs to manage the content of a Microcks instance. Thanks to those CR, 
you can easily define load pre-existing API definitions and connection secrets into an operator-managed Microcks instance.

For example, you can create a new `APISource` CR named `tests-artifacts` that will load 4 artifacts into the `microcks` instance
and create an addition `Hello Soep Service` importer:

```sh
cat <<EOF | kubectl apply -f -
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
EOF
```

> For comprehensive documentation and examples of `APISource` CR, please refer to the [APISource CR documentation](./documentation/apisource-cr.md).

A Microcks instance may also need some secrets to be able to connect or to authenticate to external services like repositories or messaging brokers.
The `SecretSource` CR is here to help you define those secrets and have them loaded into the Microcks instance.

For example, you can create a new `SecretSource` CR named `tests-secrets` that will load 2 secrets into the `microcks` instance.
The first one is a simple secret with username, password, token and CA certificate. The second one is a secret that will be loaded 
from a Kubernetes secret named `microcks-keycloak-admin` and will use the `username` and `password` keys from this secret:

```sh
cat <<EOF | kubectl apply -f -
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
EOF
```

> For comprehensive documentation and examples of `SecretSource` CR, please refer to the [SecretSource CR documentation](./documentation/secretsource-cr.md).

## How to build it?

The operator is made of 2 modules:
* `api` contains the model for manipulating Custom Resources elements using Java,
* `operator` contains the Kubernetes controller implementing the remediation logic. It is implemented in [Quarkus](https://www.quarkus.io).

### Api module

Simply execute:

```sh
mvn clean install
```

### Operator module

Produce a native container image with the name elements specified within the `pom.xml`:

```sh
mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true
```

## Local development

Be sure to be connected to a Kubernetes cluster first with a context set to a default namespace. 

### For Microcks CR

In this situation, you'll be able to use Quarkus iterative development loop. From the `operator/` folder, launch:

```sh
mvn quarkus:dev
```

The operator will generate and then install/update the latest version of the CRD and wait for reconciliation loop to be triggered.

From the `deploy/` folder, create a new sample CRD using:

```sh
kubectl apply -f samples/microcks-microcks.io-v1alpha1.yml
```

You shall see the operator starting the reconciliation with a log like:

```
2024-07-31 14:12:18,732 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-391) Starting reconcile operation for 'microcks'
[...]
2024-07-31 14:12:48,615 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Keycloak reconciliation triggered an update? false
2024-07-31 14:12:48,618 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Mongo reconciliation triggered an update?: false
2024-07-31 14:12:48,621 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Microcks reconciliation triggered an update?: false
2024-07-31 14:12:48,623 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Postman reconciliation triggered an update?: false
2024-07-31 14:12:48,627 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Async reconciliation triggered an update?: false
2024-07-31 14:12:48,627 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Finishing reconcile operation for 'microcks'
2024-07-31 14:12:48,628 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-716) Returning a noUpdate control. =============================
 
```

### For APISource & SecretSource CR

In this situation, you won't be able to run the operator in local Quarkus process as the controllers for these CRs
use internal Kubernetes network names to interact with Microcks instance.

The Operator must then be deployed in your local Kubernetes cluster. You can use the `deploy/operator-dev-jvm.yaml` file to do so.

Then you can create the needed sample CRs using:

```sh
kubectl apply -f samples/apisource-microcks.io-v1alpha1-tests.yml
kubectl apply -f samples/secretsource-microcks.io-v1alpha1-tests.yml
```

You can check the reconciliation status of those CRs using:

```sh
kc get apisources/tests-artifacts -o yaml
kc get secretsources/tests-secrets -o yaml
```

When iterating on the operator code, you can rebuild the operator container image and then apply the new version using:

```sh
kc scale --replicas=0 deployment/microcks-operator
mvn clean package && docker build -f src/main/docker/Dockerfile.jvm -t quay.io/lbroudoux/microcks-operator:jvm-latest . && docker push quay.io/lbroudoux/microcks-operator:jvm-latest
kc scale --replicas=1 deployment/microcks-operator
```

## Local tests

