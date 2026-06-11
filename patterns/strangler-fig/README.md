# Strangler Fig Pattern

## What it is
Gradually migrate a legacy monolith to microservices
without a risky big-bang rewrite.
Named after the strangler fig tree that grows around
a host tree and eventually replaces it.

## Migration phases

### Phase 1 — All traffic to monolith
  /* → legacy-monolith:8080

### Phase 2 — Customer APIs migrated
  /api/customers/** → customer-service:8082 (new)
  /**              → legacy-monolith:8080   (rest)

### Phase 3 — Canary release for payments
  /api/payments/** → payment-service:8091   (10% new)
  /api/payments/** → legacy-monolith:8080   (90% old)
  /**              → legacy-monolith:8080   (rest)

### Phase 4 — Full migration
  /api/customers/** → customer-service:8082
  /api/accounts/**  → account-service:8083
  /api/payments/**  → payment-service:8091
  # Monolith decommissioned ✓

## Key enabler
API Gateway (Kong at Al Hilal / Spring Cloud Gateway in demo)
routes traffic to old and new services simultaneously.
Traffic shifting happens with zero downtime.
