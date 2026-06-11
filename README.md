# Al Hilal Bank — Microservices Banking Platform Demo

A complete banking microservices platform demonstrating enterprise-grade
architecture patterns, built as a Solutions Architect portfolio project.

---

## Architecture Overview

```
Mobile App / Browser
        │
        ▼
┌─────────────────┐
│   API Gateway   │  ← JWT Auth, Rate Limiting, Routing
│  (Kong equiv.)  │    Spring Cloud Gateway + Redis
└────────┬────────┘
         │
    ┌────▼────┐
    │   BFF   │  ← Backend for Frontend
    │ Service │    Parallel calls, Circuit Breaker
    └────┬────┘
         │
   ┌─────┴──────┐
   ▼            ▼
Customer     Account
Service      Service
   │            │
   └─────┬──────┘
         │
       Kafka
   (Event Streaming)
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| API Gateway | Spring Cloud Gateway |
| Event Streaming | Apache Kafka 3.7 (KRaft — no Zookeeper) |
| Cache / Rate Limiting | Redis 7.2 |
| Database | H2 (dev) / PostgreSQL (prod) |
| Container | Docker |
| Orchestration | Kubernetes (minikube) |
| Package Manager | Helm |
| GitOps CD | ArgoCD |
| Monitoring | Prometheus + Grafana |
| CI/CD | GitHub Actions |

---

## Project Structure

```
alhilal-demo/
├── api-gateway/              # Spring Cloud Gateway (port 8080)
├── bff-retail-service/       # Backend for Frontend (port 8085)
├── customer-service/         # Customer domain + JWT auth (port 8082)
├── account-service/          # Account domain (port 8083)
├── helm/                     # Helm charts for K8s deployment
│   └── alhilal-demo/
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── values-prod.yaml
│       └── templates/
├── k8s/                      # Raw K8s manifests
├── monitoring/               # Prometheus + Grafana config
└── patterns/                 # Design pattern implementations
    ├── saga-choreography/     # Saga (event-driven, Kafka)
    │   ├── payment-service/   # port 8091
    │   ├── account-service/   # port 8092
    │   └── notification-service/ # port 8093
    ├── saga-orchestration/    # Saga (command-driven)
    │   ├── orchestrator-service/ # port 8094
    │   ├── account-service/   # port 8095
    │   └── notification-service/ # port 8096
    ├── cqrs/                  # CQRS pattern
    │   ├── command-service/   # port 8097 (WRITE)
    │   └── query-service/     # port 8098 (READ)
    ├── event-sourcing/        # Event Sourcing with time travel
    │   └── event-store-service/ # port 8099
    ├── sidecar/               # Sidecar K8s YAML + docs
    └── strangler-fig/         # Strangler Fig routing config + docs
```

---

## Core Features

### Security
- JWT authentication (HMAC-SHA256 signature validation)
- Rate limiting via Redis token bucket algorithm
- Role-based access control (RBAC) — CUSTOMER / ADMIN roles
- X-Customer-Id header injection by Gateway

### Resilience
- Circuit Breaker (Resilience4j) — 3 states: CLOSED / OPEN / HALF-OPEN
- Fallback responses — partial data instead of blank errors
- Health probes — liveness + readiness in K8s
- Kafka retry and DLQ support

### Scalability
- HPA (Horizontal Pod Autoscaler) — CPU-based auto-scaling
- Kafka partitioned topics for parallel processing
- CQRS — independent read/write service scaling
- Helm values per environment (dev/prod replicas)

### Observability
- Prometheus metrics exposed via /actuator/prometheus
- Grafana dashboards — request rate, error rate, latency
- Structured logging with correlation IDs
- ArgoCD deployment visibility

---

## Design Patterns Implemented

| Pattern | Implementation | Port | Status |
|---------|---------------|------|--------|
| API Gateway | Spring Cloud Gateway | 8080 | ✅ Built + Tested |
| BFF | bff-retail-service | 8085 | ✅ Built + Tested |
| Circuit Breaker | Resilience4j | - | ✅ Built + Tested |
| Rate Limiting | Redis Token Bucket | - | ✅ Built + Tested |
| Saga Choreography | Kafka events | 8091-8093 | ✅ Built + Tested |
| Saga Orchestration | Central coordinator | 8094-8096 | ✅ Built + Tested |
| CQRS | Command/Query split | 8097-8098 | ✅ Built + Tested |
| Event Sourcing | Time travel queries | 8099 | ✅ Built + Tested |
| Database per Service | Separate H2 DBs | - | ✅ Implemented |
| Service Discovery | K8s CoreDNS | - | ✅ Built-in |
| Sidecar | K8s YAML (Envoy) | - | ✅ YAML + Docs |
| Strangler Fig | Gateway routing | - | ✅ Config + Docs |
| Outbox Pattern | Kafka + DB | - | ✅ Documented |

---

## Quick Start

### Prerequisites
- Java 21
- Docker
- minikube
- Helm 3
- kubectl

### Local (Docker Compose)
```bash
# Clone the repo
git clone https://github.com/harirajan/alhilal-demo.git
cd alhilal-demo

# Start all services
docker-compose up

# Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"abdullah@test.com","password":"password123"}'
```

### Kubernetes (minikube)
```bash
# Start minikube
minikube start --driver=docker --memory=6144 --cpus=4
eval $(minikube docker-env)

# Build images inside minikube
docker build -t alhilal/customer-service:1.0 ./customer-service
docker build -t alhilal/account-service:1.0 ./account-service
docker build -t alhilal/bff-retail-service:1.0 ./bff-retail-service
docker build -t alhilal/api-gateway:1.0 ./api-gateway

# Deploy with Helm
helm install alhilal-demo helm/alhilal-demo

# Watch pods come up
kubectl get pods -w

# Access API Gateway
kubectl port-forward deployment/api-gateway 8080:8080 &
```

### GitOps with ArgoCD
```bash
# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Deploy application (watches GitHub, auto-deploys on push)
kubectl apply -f argocd-app.yaml

# Access ArgoCD UI
kubectl port-forward svc/argocd-server -n argocd 8888:443 &
# https://localhost:8888
```

---

## API Examples

### Authentication
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"abdullah@test.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

### BFF Home Screen
```bash
# Get aggregated home screen data (balance + accounts + KYC)
curl http://localhost:8080/bff/v1/home/CUST-C09DDEE2 \
  -H "Authorization: Bearer $TOKEN"
```

### Saga Choreography — Payment Transfer
```bash
# Happy path (ACC-001 has AED 5000)
curl -X POST http://localhost:8091/api/v1/saga/payments/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC-001","toAccount":"ACC-002","amount":500,"customerId":"CUST-001"}'

# Failure path (ACC-003 only has AED 100)
curl -X POST http://localhost:8091/api/v1/saga/payments/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC-003","toAccount":"ACC-001","amount":500,"customerId":"CUST-003"}'

# Check saga status
curl http://localhost:8091/api/v1/saga/payments
```

### Saga Orchestration — Payment Transfer
```bash
curl -X POST http://localhost:8094/api/v1/orch/payments/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC-001","toAccount":"ACC-002","amount":500,"customerId":"CUST-001"}'
```

### CQRS
```bash
# WRITE — create account (command-service)
curl -X POST http://localhost:8097/api/v1/cqrs/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","accountType":"SAVINGS","currency":"AED"}'

# WRITE — deposit
curl -X POST http://localhost:8097/api/v1/cqrs/accounts/{accountId}/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":1000}'

# READ — get pre-aggregated data (query-service)
curl http://localhost:8098/api/v1/cqrs/accounts/{accountId}
# Returns: balance + totalDeposited + totalWithdrawn + totalTransactions
```

### Event Sourcing
```bash
# Create account
curl -X POST http://localhost:8099/api/v1/es/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","currency":"AED"}'

# Deposit salary
curl -X POST http://localhost:8099/api/v1/es/accounts/{id}/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"description":"Salary credit"}'

# Get current state (replays all events)
curl http://localhost:8099/api/v1/es/accounts/{id}

# TIME TRAVEL — get balance at specific point in time
curl "http://localhost:8099/api/v1/es/accounts/{id}/balance-at?at=2026-06-11T10:30:00"

# Complete audit trail
curl http://localhost:8099/api/v1/es/accounts/{id}/history
```

---

## Test Credentials

| Customer | Customer ID | Password | KYC Status |
|----------|------------|---------|-----------|
| Abdullah Al-Rashid | CUST-C09DDEE2 | password123 | VERIFIED |
| Fatima Al-Hassan | CUST-AA385311 | password456 | PENDING |

### Saga Test Accounts

| Account | Balance | Use for |
|---------|---------|---------|
| ACC-001 | AED 5,000 | Happy path transfers |
| ACC-002 | AED 1,000 | Receiving transfers |
| ACC-003 | AED 100 | Insufficient funds test |

---

## Monitoring

```bash
# Start Prometheus + Grafana
docker-compose -f monitoring/docker-compose.yml up

# Access
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000 (admin/admin)
```

### Key PromQL Queries
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Circuit breaker state
resilience4j_circuitbreaker_state

# P99 latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

---

## CI/CD Pipeline

GitHub Actions pipeline (`.github/workflows/ci.yml`):

```
Push to main
    │
    ▼
1. Compile + Test (mvn verify)
    │
    ▼
2. Build Docker Image
    │
    ▼
3. Push to Docker Hub
    │
    ▼
4. Update Helm values (image.tag)
    │
    ▼
ArgoCD detects Git change
    │
    ▼
Auto-deploy to K8s (rolling update)
```

---

## Deployment Strategies

| Strategy | Use case | How |
|----------|----------|-----|
| Rolling Update | Regular releases | K8s default — pods replaced gradually |
| Blue-Green | Major releases, DB migrations | Switch Service selector instantly |
| Canary | Risky features, payment changes | Shift traffic % gradually |

---

## Banking Domain Context

Designed to mirror Al Hilal Bank's actual architecture:

| Our Demo | Al Hilal Production |
|----------|-------------------|
| Spring Cloud Gateway | Kong API Gateway |
| JWT + Redis | ForgeRock Identity |
| H2 in-memory | Temenos T24 Core Banking |
| Logged SMS | Infobip notifications |
| Local Kafka | Azure Event Hubs |
| minikube | Azure AKS |
| GitHub Actions | Azure DevOps Pipelines |
| ArgoCD | ArgoCD / Azure DevOps Release |

---

## Interview Talking Points

### Architecture
- BFF pattern aggregates customer + account data in parallel (CompletableFuture)
- API Gateway validates JWT by recalculating HMAC-SHA256 — never stores tokens
- Circuit Breaker returns partial data — "Good morning, Unavailable" instead of 500 error
- Service Discovery via K8s CoreDNS — no Eureka needed in K8s

### Kafka
- KRaft mode (no Zookeeper) — Kafka 3.7
- Consumer groups: same groupId = load balanced, different groupId = broadcast
- Offset stored per consumer group per partition in __consumer_offsets
- Auto-offset-reset: earliest only applies when group has no committed offset

### Kubernetes
- HPA auto-scaled from 1→4 pods under load (proved live)
- ArgoCD self-healing: manual kubectl scale reversed within 3 minutes
- Rolling update: old pod serves traffic until new pod passes readiness probe
- Helm rollback creates NEW revision — full audit trail preserved

### Patterns
- Saga Choreography: loose coupling, events (past tense), no single point of failure
- Saga Orchestration: central coordinator, commands (imperative), easy debugging
- CQRS: pre-aggregated READ DB, eventual consistency, independent scaling
- Event Sourcing: immutable events, time travel queries, CBUAE compliance

---

## Author

Java/Spring Boot developer with 20+ years experience
specializing in banking and fintech systems (Zafin background).
Building ClearFlow — a comprehensive Core Banking + Billing Engine platform.

---

*Built for SA interview preparation — June 2026*
