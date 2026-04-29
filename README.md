# sample-test-app

Sample Node.js auth service — template for onboarding apps into the DIKSHA split-repo GitOps pipeline.

**App repo:** `git@github.com:tsprasath/sample-test-app.git`
**DevOps repo:** `git@github.com:tsprasath/ai-devops.git` (Helm charts, ArgoCD, infra)

---

## What's Here

```
.
├── src/
│   ├── index.js              # Express app (health, auth endpoints)
│   └── middleware/auth.js     # JWT auth middleware
├── tests/
│   └── auth.test.js           # Jest tests
├── Dockerfile                 # Multi-stage production build
├── .dockerignore
├── .eslintrc.json
├── .env.example               # Local dev env vars
├── Jenkinsfile                # Full self-contained CI pipeline
├── helm/auth-service/         # Simple Helm chart for K8s deployment
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│       ├── deployment.yaml
│       ├── service.yaml
│       └── ingress.yaml
└── package.json
```

## Local Development

```bash
npm install
cp .env.example .env           # fill in values
npm run dev                    # nodemon on :3000
npm test                       # jest with coverage
npm run lint
```

## CI Pipeline (Jenkinsfile)

The `Jenkinsfile` in this repo is a **self-contained CI pipeline** — no shared library dependency. It runs:

| Stage | What it does |
|-------|-------------|
| **Checkout** | Clones repo, generates image tag (`branch_sha_buildnum`) |
| **Install Dependencies** | `npm ci` |
| **Code Quality** | ESLint + npm audit (parallel) |
| **Unit Tests** | Jest with coverage |
| **Docker Build** | Multi-stage build, tagged for OCIR |
| **Security Scans** | Trivy image/filesystem + Gitleaks (parallel, skipped if not installed) |
| **Smoke Test** | Docker run → health check → register → login → cleanup |
| **Push to OCIR** | Only on `main` branch, uses `ocir-credentials` from Jenkins |

### Jenkins Setup

Create a Pipeline job pointing to this repo:
- **SCM:** Git → `https://github.com/tsprasath/sample-test-app.git`
- **Branch:** `*/main`
- **Script Path:** `Jenkinsfile`

Required Jenkins credentials (for OCIR push on main):
- `ocir-credentials` — Username/Password for OCI Registry

### Environment Variables

The pipeline uses these (all hardcoded in Jenkinsfile, no external config needed):

| Variable | Value |
|----------|-------|
| `OCIR_REGISTRY` | `bom.ocir.io` |
| `OCIR_NAMESPACE` | `bmzbbujw9kal` |
| `OCIR_REPO` | `dev-repo-test` |
| `SERVICE` | `auth-service` |
| `TEST_PORT` | `3099` |

## How CD Works

1. Push to `main` → Jenkins CI runs → image pushed to OCIR
2. Image tag updated in `helm/auth-service/values.yaml` (this repo)
3. ArgoCD detects the change and deploys to OKE

### Helm Chart

Simple chart at `helm/auth-service/` — Deployment, Service, optional Ingress.

```bash
# Validate locally
helm template auth-service helm/auth-service/

# Deploy
helm upgrade --install auth-service helm/auth-service/ \
  --set image.tag=main_abc1234_42 \
  --namespace diksha

# Override env vars
helm upgrade --install auth-service helm/auth-service/ \
  --set env.LOG_LEVEL=debug \
  --set env.NODE_ENV=staging
```

### Runtime Config

- **Non-secrets** (NODE_ENV, PORT, LOG_LEVEL) → set via `env:` in values.yaml
- **Secrets** (JWT_SECRET, DB_PASSWORD, REDIS_PASSWORD) → K8s Secrets or Vault (mount as env vars in deployment)

## Onboarding a New App

Copy this repo as a template. Update `SERVICE`, `OCIR_REPO` in the Jenkinsfile and you're done.

## License

MIT
