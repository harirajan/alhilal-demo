# Sidecar Pattern

## What it is
Deploy a helper container alongside the main service container
in the same Kubernetes pod. The sidecar handles cross-cutting
concerns without any changes to the main service code.

## What the sidecar handles
- Logging (all requests/responses)
- Metrics (Prometheus scraping)
- mTLS encryption (service-to-service)
- Circuit breaking and retries
- Distributed tracing (Jaeger/Zipkin)

## How to deploy
```bash
kubectl apply -f sidecar-deployment.yaml
kubectl get pods
# Shows: customer-service-with-sidecar-xxx   2/2   Running
#        ^^ 2 containers in same pod ^^
```

## Key point
Main service container: handles business logic only
Sidecar container:      handles infrastructure concerns
Both share: same network (localhost), same storage volumes
