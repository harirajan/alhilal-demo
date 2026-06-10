# Al Hilal Banking Demo — Local Learning Project

A step-by-step project that mirrors Al Hilal Bank's actual architecture.
Run everything locally. Learn by doing.

---

## What this project teaches you (in order)

```
PHASE 1 — Basic microservices talking to each other
  ✓ customer-service  (its own DB, its own API)
  ✓ account-service   (its own DB, its own API)
  ✓ bff-retail-service (calls both, assembles response)
  ✓ Spring Cloud Gateway (routes traffic like Kong)

PHASE 2 — Kafka events between services
  ✓ account-service publishes AccountOpenedEvent
  ✓ customer-service listens and updates

PHASE 3 — Security
  ✓ JWT login
  ✓ Protected endpoints
  ✓ RBAC roles

PHASE 4 — Resilience
  ✓ Circuit breaker on BFF → domain calls
  ✓ Retry logic
```

---

## Architecture (mirrors Al Hilal exactly)

```
📱 Postman / Browser (your "mobile app")
        │
        │ HTTP
        ▼
┌─────────────────────────┐
│   Spring Cloud Gateway  │  ← local version of Kong
│   localhost:8080        │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│   bff-retail-service    │  ← BFF
│   localhost:8081        │
└────────────┬────────────┘
             │ calls both
    ┌────────┴──────────┐
    ▼                   ▼
┌──────────┐     ┌──────────────┐
│ customer │     │   account    │
│ service  │     │   service    │
│ :8082    │     │   :8083      │
│ H2 DB    │     │   H2 DB      │
└──────────┘     └──────────────┘
        │               │
        └───────┬────────┘
                ▼
           Kafka :9092
        (events flow here)
```

---

## How to run

### Prerequisites
- Java 21
- Maven
- Docker Desktop

### Start infrastructure (Kafka only)
```bash
docker-compose up -d
```

### Start each service (4 terminals)
```bash
# Terminal 1
cd customer-service && mvn spring-boot:run

# Terminal 2
cd account-service && mvn spring-boot:run

# Terminal 3
cd bff-retail-service && mvn spring-boot:run

# Terminal 4
cd api-gateway && mvn spring-boot:run
```

### Test it
Open: http://localhost:8080/swagger-ui.html

---

## Learning path — do these in order

### Step 1: Understand the structure
- Open each service's `pom.xml` — notice they are separate Maven projects
- Each has its own `application.yml` with its own port and DB
- They are independent — you can start/stop any one

### Step 2: Call the APIs directly (bypass BFF)
```bash
# Call customer-service directly
GET http://localhost:8082/api/v1/customers

# Call account-service directly
GET http://localhost:8083/api/v1/accounts
```

### Step 3: Call through BFF
```bash
# BFF calls BOTH services and assembles response
GET http://localhost:8081/bff/v1/home/CUST-001
```
Compare the response — BFF returns a tailored view, not raw data.

### Step 4: Call through Gateway
```bash
# Same call but through the gateway
GET http://localhost:8080/bff/v1/home/CUST-001
```
Gateway routes to BFF automatically.

### Step 5: Watch Kafka events
Create an account → watch customer-service log → it received the event.

---

## Ports reference
| Service | Port | Swagger |
|---------|------|---------|
| API Gateway | 8080 | N/A (just routes) |
| BFF Retail | 8081 | http://localhost:8081/swagger-ui.html |
| Customer Service | 8082 | http://localhost:8082/swagger-ui.html |
| Account Service | 8083 | http://localhost:8083/swagger-ui.html |
| Kafka | 9092 | N/A |
| Kafka UI | 8090 | http://localhost:8090 |
