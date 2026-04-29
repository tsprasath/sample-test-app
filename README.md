# sample-test-app

Sample Node.js auth service — template for onboarding apps into the DIKSHA split-repo GitOps pipeline.

**App repo:** `git@github.com:tsprasath/sample-test-app.git`
**DevOps repo:** `git@github.com:tsprasath/ai-devops.git` (CI/CD, Helm, ArgoCD, infra)

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
├── Jenkinsfile                # Thin trigger — calls shared pipeline in ai-devops
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

## How CI/CD Works

1. Push to `main` triggers the `Jenkinsfile` in this repo
2. Jenkins runs the shared pipeline from `ai-devops` — build, test, scan (Trivy/Gitleaks), push to OCIR
3. Jenkins updates the image tag in `ai-devops/infra/helm-charts/auth-service/values-dev.yaml`
4. ArgoCD detects the change and deploys to OKE

See [ai-devops](https://github.com/tsprasath/ai-devops/tree/jcasc-gitops-bootstrap) for full pipeline, Helm charts, and infra.

## Onboarding a New App

Copy this repo as a template. The only file that ties it to the pipeline is the `Jenkinsfile` — it references the shared library in ai-devops. Update the service name and you're done.

## License

MIT
