## Microcks Operator Helm Chart

Install and manage the [Microcks](http://microcks.io) Operator and its Custom Resource Definitions (CRDs) using [Helm](https://helm.sh) package manager.

### Requirements
- Kubernetes >= 1.22
- Helm 3.x

### What this Chart deploys
- A `Deployment` running the Microcks Operator container
- (Optionally) a `ServiceAccount`, `Role`, and `RoleBinding`
- CRDs placed under `crds/` are handled by Helm (see CRDs section)

## Installing the Chart

> You can retrieve the Helm chart from the official Microcks Helm repo: `helm repo add microcks https://microcks.io/helm`

Basic install into a dedicated namespace:

```bash
helm install microcks-operator ./deploy/helm/microcks-operator \
  --namespace microcks --create-namespace
```

Install while skipping CRDs (for clusters where CRDs are pre-installed/managed out-of-band):

```bash
helm install microcks-operator ./deploy/helm/microcks-operator \
  --namespace microcks --create-namespace \
  --skip-crds
```

Install with a custom values file:

```bash
helm install microcks-operator ./deploy/helm/microcks-operator \
  --namespace microcks --create-namespace \
  -f my-values.yaml
```

Quick override example (pin the image tag and set resources):

```bash
helm install microcks-operator ./deploy/helm/microcks-operator \
  -n microcks --create-namespace \
  --set image.tag="0.0.6" \
  --set resources.limits.cpu="200m" \
  --set resources.limits.memory="512Mi" \
  --set resources.requests.cpu="100m" \
  --set resources.requests.memory="256Mi"
```

## Upgrading the chart

```bash
helm upgrade microcks-operator ./deploy/helm/microcks-operator -n microcks
```

If you manage CRDs separately, include `--skip-crds` during upgrades to avoid changes to CRDs by Helm:

```bash
helm upgrade microcks-operator ./deploy/helm/microcks-operator -n microcks --skip-crds
```

## Uninstalling the chart

```bash
helm uninstall microcks-operator -n microcks
```

Note: Uninstalling the chart does not remove CRDs. Delete CRDs only if you are certain no custom resources remain.


## Example values.yaml

```yaml
nameOverride: ""
fullnameOverride: ""

image:
  repository: quay.io/microcks/microcks-operator
  tag: "v1.0.0"
  pullPolicy: IfNotPresent

imagePullSecrets: []

replicaCount: 1

serviceAccount:
  create: true
  name: ""
  annotations: {}

rbac:
  create: true

resources:
  limits:
    cpu: 200m
    memory: 256Mi
  requests:
    cpu: 100m
    memory: 128Mi

nodeSelector: {}
tolerations: []
affinity: {}
```

## Notes
- To skip CRD installation on `helm install`, pass `--skip-crds`.
- If `serviceAccount.create=false`, set `serviceAccount.name` to an existing SA and ensure the necessary RBAC exists (or set `rbac.create=false` and bind your own roles).

