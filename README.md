# Dev Project

Production-ready microservices scaffold with full DevOps pipeline.

## Stack
- **Runtime**: Node.js 20 (Express)
- **Container**: Docker (multi-stage, distroless-like Alpine)
- **Orchestration**: Kubernetes (OKE on Oracle Cloud)
- **CI/CD**: Jenkins (declarative pipeline + shared library)
- **GitOps**: ArgoCD (app-of-apps pattern)
- **Routing**: OCI API Gateway + Load Balancer
- **Security**: Trivy, Gitleaks, OPA Gatekeeper, OCI WAF
- **Infra**: Terraform (OCI modules)

## Services
| Service | Tech | Port | Status |
|---------|------|------|--------|
| auth-service | Node.js/Express | 3000 | ✅ Ready |

## Quick Start

```bash
# Local development
cd services/auth-service
cp .env.example .env
npm install
npm run dev

# Docker build
docker build -t dev/auth-service:local services/auth-service

# Helm install (dev)
helm install auth-service infra/helm-charts/auth-service -n dev --create-namespace
```

## Pipeline Flow
```
Code Push → Jenkins → Lint → Build → Test → Security Scan → Push OCIR → GitOps Update → ArgoCD Sync
```

## Directory Structure
```
services/          # Microservices source code
infra/terraform/   # OCI infrastructure as code
infra/helm-charts/ # Kubernetes deployment charts
infra/argocd-apps/ # GitOps application manifests
ci/                # Jenkins pipeline + shared library
security/          # OPA policies + scan configs
docs/              # Architecture documentation
```
