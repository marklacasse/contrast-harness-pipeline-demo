# Kustomize Configuration

This directory contains Kustomize configurations for deploying the vulnerable application with Contrast IAST.

## Structure

```
k8s-kustomize/
├── base/                    # Base configuration (common to all environments)
│   ├── deployment.yaml      # Deployment manifest
│   ├── service.yaml         # Service manifest
│   └── kustomization.yaml   # Base kustomization
└── overlays/                # Environment-specific configurations
    ├── dev/                 # Development environment
    │   └── kustomization.yaml
    └── prod/                # Production environment
        └── kustomization.yaml
```

## Usage

### Local Testing

**Build and view manifests without applying:**
```bash
# Dev environment
kubectl kustomize k8s-kustomize/overlays/dev

# Prod environment
kubectl kustomize k8s-kustomize/overlays/prod
```

**Apply to cluster:**
```bash
# Dev environment
kubectl apply -k k8s-kustomize/overlays/dev

# Prod environment
kubectl apply -k k8s-kustomize/overlays/prod
```

### With Harness

In your Harness pipeline, configure the Service to use:
- **Manifest Type**: K8s Manifest
- **Store Type**: Git
- **File Path**: `k8s-kustomize/overlays/dev` (or `prod`)
- **Enable**: "Use Kustomize for Manifest rendering"

Harness will automatically:
1. Run `kubectl kustomize` on the overlay
2. Replace the image tag with the artifact from Build stage
3. Apply the rendered manifests to your cluster

## Contrast Environment Variables

Contrast-specific variables are managed in the `configMapGenerator` section of each overlay's `kustomization.yaml`.

**Key variables to configure:**

```yaml
CONTRAST__API__URL                    # Contrast API endpoint
CONTRAST__API__SERVICE_KEY            # Service key from Contrast
CONTRAST__API__USER_NAME              # Contrast username
CONTRAST__API__API_KEY                # Contrast API key
CONTRAST__APPLICATION__NAME           # Application name in Contrast
CONTRAST__APPLICATION__TAGS           # Tags for the application
```

**Update credentials in overlays:**
1. Edit `overlays/dev/kustomization.yaml` for dev credentials
2. Edit `overlays/prod/kustomization.yaml` for prod credentials

> **Security Note**: Consider using Kubernetes Secrets or external secret management (e.g., AWS Secrets Manager, HashiCorp Vault) for sensitive credentials in production.

## Customization Examples

### Change Image Tag
Edit the overlay's `kustomization.yaml`:
```yaml
images:
  - name: zencid/contrast-harness-pipeline-demo
    newTag: v1.2.3
```

### Adjust Replicas
```yaml
replicas:
  - name: vulnerable-app
    count: 3
```

### Add Environment Variable
```yaml
configMapGenerator:
  - name: contrast-config
    behavior: merge
    literals:
      - NEW_VAR=value
```

### Patch Resources
```yaml
patches:
  - patch: |-
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/memory
        value: "1Gi"
    target:
      kind: Deployment
      name: vulnerable-app
```

## Benefits of Kustomize

✅ **Native to kubectl** - No additional tools required
✅ **Environment overlays** - Easy dev/staging/prod management
✅ **ConfigMap generation** - Automatic hash suffixes for rolling updates
✅ **Image management** - Built-in image tag overrides
✅ **Patches** - Strategic merge or JSON patches for customization
✅ **DRY principle** - Base + overlays reduces duplication
