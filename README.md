# microcks-operator

## Build

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
2023-03-24 15:43:34,969 INFO  [io.git.mic.ope.MicrocksReconciler] (ReconcilerExecutor-microcksreconciler-113) Starting reconcile operation for 'microcks' 
```