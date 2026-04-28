# Architecture Overview

## System Design

```
                    ┌─────────────┐
                    │   DNS/LB    │
                    │  (OCI LB)   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ OCI WAF     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ API Gateway │
                    │   (OCI)     │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──┐  ┌─────▼────┐  ┌───▼──────┐
       │  Auth   │  │   API    │  │ Frontend │
       │ Service │  │ Service  │  │  (CDN)   │
       │ (Node)  │  │ (TBD)   │  │          │
       └────┬────┘  └────┬────┘  └──────────┘
            │             │
       ┌────▼─────────────▼────┐
       │    Database Layer     │
       │  (OCI MySQL/Auto DB)  │
       └───────────────────────┘
```

## Environments
- **dev**: Auto-deploy on merge to develop branch
- **staging**: Auto-deploy on merge to main, full clone of prod
- **prod**: Manual approval, canary rollout via ArgoCD

## Security Layers
1. OCI WAF - DDoS, SQL injection, XSS protection
2. API Gateway - JWT validation, rate limiting, CORS
3. Network Policies - Service-to-service isolation
4. Pod Security - Non-root, read-only FS, no privilege escalation
5. Image Scanning - Trivy on every build
6. Secret Management - OCI Vault + External Secrets Operator
