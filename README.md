# sample-test-app

A DevOps platform demo and reference implementation showcasing production-grade CI/CD, GitOps, and infrastructure-as-code patterns on Oracle Cloud (OKE).

**Repo:** `git@github.com:tsprasath/sample-test-app.git`
**DevOps/Infra repo:** `git@github.com:tsprasath/ai-devops.git`

---

## What This Demonstrates

- **Split-repo GitOps** — app code and devops/infra config live in separate repos
- **JCasC** — Jenkins Configuration as Code with env var interpolation
- **K8s bootstrap with Kustomize** — namespaces, OCIR secrets, Reloader, resource quotas, network policies per environment
- **Helm charts** with per-environment values files (dev / staging / prod)
- **ArgoCD ApplicationSet** for automatic service discovery and deployment
- **Jenkins shared library** for reusable CI/CD pipeline steps
- **Terraform modules** for OCI infrastructure (OKE, VCN, OCIR, Vault, WAF, API Gateway)
- **Security scanning** — Trivy (container), Gitleaks (secrets), OPA (policy)
- **Monitoring** — Prometheus, Grafana (DORA metrics, security overview), Loki

---

## Architecture Flow

```
 Developer pushes code
        |
        v
 +-------------+     webhook     +----------------+
 |  App Repo   | --------------> |    Jenkins      |
 | (this repo) |                 | (JCasC + shared |
 +-------------+                 |    library)     |
                                 +-------+--------+
                                         |
                          build, test, scan (Trivy/Gitleaks/OPA)
                                         |
                                         v
                                 +----------------+
                                 |  OCIR (Oracle  |
                                 |  Container     |
                                 |  Registry)     |
                                 +-------+--------+
                                         |
                              gitops-update (image tag)
                                         |
                                         v
                                 +----------------+
                                 |  DevOps Repo   |  <-- Helm values updated
                                 |  (ai-devops)   |
                                 +-------+--------+
                                         |
                                    auto-sync
                                         |
                                         v
                                 +----------------+
                                 |    ArgoCD      |
                                 | (ApplicationSet|
                                 |  per env)      |
                                 +-------+--------+
                                         |
                                         v
                                 +----------------+
                                 |  OKE Cluster   |
                                 |  dev / staging |
                                 |  / prod        |
                                 +----------------+
```

---

## Directory Structure

```
.
├── services/
│   └── auth-service/          # Node.js sample service
│       ├── src/
│       │   ├── index.js
│       │   └── middleware/auth.js
│       ├── tests/auth.test.js
│       └── package.json
├── ci/
│   ├── Jenkinsfile            # Main pipeline
│   ├── templates/
│   │   └── Jenkinsfile.app-repo   # Template for onboarding new app repos
│   ├── config/
│   │   ├── jenkins.yml        # JCasC configuration
│   │   └── env.example        # Required env vars
│   ├── shared-lib/
│   │   ├── vars/
│   │   │   ├── gitopsUpdate.groovy
│   │   │   └── promoteToProd.groovy
│   │   └── src/org/dev/Constants.groovy
│   ├── setup-jenkins.sh
│   └── setup-jenkins-ubuntu24.sh
├── infra/
│   ├── terraform/
│   │   ├── modules/           # OKE, VCN, OCIR, Vault, WAF, API Gateway
│   │   └── environments/      # dev, staging, prod tfvars
│   ├── helm-charts/
│   │   └── auth-service/      # Chart + per-env values files
│   ├── argocd-apps/
│   │   ├── appset-services.yaml   # ApplicationSet auto-discovery
│   │   └── {dev,staging,prod}/    # Per-env ArgoCD app manifests
│   ├── bootstrap/
│   │   ├── base/              # Namespaces, quotas, secrets, network policies
│   │   ├── overlays/{dev,staging,prod}/
│   │   └── bootstrap.sh
│   └── monitoring/
│       ├── prometheus/        # Prometheus + alerting rules
│       ├── grafana/           # DORA metrics, security, service health dashboards
│       └── loki/              # Log aggregation
└── webhook-setup.sh
```

---

## Quick Start

### Prerequisites

- OCI account with OKE cluster provisioned
- Jenkins instance (use `ci/setup-jenkins.sh` or `ci/setup-jenkins-ubuntu24.sh`)
- ArgoCD installed on the cluster
- `kubectl`, `helm`, `terraform`, `kustomize` installed locally

### 1. Provision Infrastructure

```bash
cd infra/terraform/environments/dev
cp terraform.tfvars.example terraform.tfvars   # fill in OCI values
terraform init && terraform apply
```

### 2. Bootstrap the Cluster

```bash
cd infra/bootstrap
bash bootstrap.sh dev    # creates namespaces, quotas, secrets, reloader
```

### 3. Configure Jenkins

```bash
# Copy env vars and fill in
cp ci/config/env.example ci/config/.env

# Run setup
bash ci/setup-jenkins.sh
```

Jenkins loads `ci/config/jenkins.yml` via JCasC with env var interpolation.

### 4. Deploy ArgoCD Apps

```bash
kubectl apply -f infra/argocd-apps/appset-services.yaml
```

ArgoCD auto-discovers services under `infra/helm-charts/` and deploys per environment.

### 5. Set Up Webhooks

```bash
bash webhook-setup.sh
```

---

## Onboarding a New Service

1. **Create the service** under `services/<service-name>/` with a Dockerfile
2. **Add a Helm chart** at `infra/helm-charts/<service-name>/` with `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`
3. **Copy the Jenkinsfile template** from `ci/templates/Jenkinsfile.app-repo` into the service or repo
4. **ArgoCD auto-discovers** — the ApplicationSet in `infra/argocd-apps/appset-services.yaml` picks up new charts automatically
5. **(Optional)** Add per-env ArgoCD manifests under `infra/argocd-apps/{dev,staging,prod}/`

---

## Environment Promotion Flow

```
dev  ──(auto-deploy on merge)──>  staging  ──(manual approval)──>  prod
```

1. **Dev**: Push to main triggers Jenkins → build → scan → push image → `gitopsUpdate` updates dev values → ArgoCD syncs
2. **Staging**: `gitopsUpdate` bumps staging values file → ArgoCD syncs to staging namespace
3. **Prod**: `promoteToProd` shared-lib step requires manual approval → updates prod values → ArgoCD syncs to prod namespace

Security gates (Trivy, Gitleaks, OPA) run at build time. Failed scans block promotion.

---

## Security

- **Trivy** — container image vulnerability scanning
- **Gitleaks** — secret detection in source code
- **OPA** — policy-as-code enforcement
- **Network Policies** — per-namespace traffic isolation
- **WAF** — Oracle WAF module via Terraform
- **OCIR secrets** — managed via Kustomize bootstrap, not checked into git

---

## Monitoring

Pre-configured dashboards and alerting:

- **DORA Metrics** — deployment frequency, lead time, MTTR, change failure rate
- **Service Health** — request rates, latency, error rates
- **Security Overview** — scan results, policy violations

See `infra/monitoring/README.md` for setup details.

---

## License

MIT
