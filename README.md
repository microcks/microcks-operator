# Microcks Operator

Kubernetes Operator for easy setup and management of Microcks installs and other entities (using Quarkus undercover 😉)

This Operator is meant to replace the existing [microcks-ansible-operator](https://github.com/microcks/microcks-ansible-operator)
that is kinda hard to maintain and to evolve.

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/microcks/microcks-operator/build-verify.yml?logo=github&style=for-the-badge)](https://github.com/microcks/microcks/actions)
[![Container](https://img.shields.io/badge/dynamic/json?color=blueviolet&logo=docker&style=for-the-badge&label=Quay.io&query=tags[0].name&url=https://quay.io/api/v1/repository/microcks/microcks-operator/tag/?limit=10&page=1&onlyActiveTags=true)](https://quay.io/repository/microcks/microcks-operator?tab=tags)
[![License](https://img.shields.io/github/license/microcks/microcks?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Project Chat](https://img.shields.io/badge/discord-microcks-pink.svg?color=7289da&style=for-the-badge&logo=discord)](https://microcks.io/discord-invite/)
[![CNCF Landscape](https://img.shields.io/badge/CNCF%20Landscape-5699C6?style=for-the-badge&logo=cncf)](https://landscape.cncf.io/?item=app-definition-and-development--application-definition-image-build--microcks)

## Build Status

The current development version is `0.0.1-SNAPSHOT`. 

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/microcks/microcks-operator/build-verify.yml?logo=github&style=for-the-badge)](https://github.com/microcks/microcks/actions)

#### Fossa license and security scans

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator.svg?type=shield&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator?ref=badge_shield&issueType=license)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator.svg?type=shield&issueType=security)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator?ref=badge_shield&issueType=security)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator.svg?type=small)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmicrocks%2Fmicrocks-operator?ref=badge_small)

## Installation

> To Do once finalized

## Usage

> To Do once finalized

## How to build it?

The operator is made of 2 modules:
* `api` contains the model for manipulating Custom Resources elements using Java,
* `deploy` contains the Kubernetes resources for deploying the operator,
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
From the `operator/` folder, launch:

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