# 🚦 Smart Traffic Recognition & Prediction System

A production-style, microservice-based traffic monitoring platform. En route, anomaly and flow sensors (simulated), TomTom live traffic, and manual readings feed a small processing pipeline that predicts congestion, recommends routes, and exposes everything through a polished web UI.

![Status](https://img.shields.io/badge/status-production--ready-success)

## Roadmap & status

| Phase | Name                        | Status |
|-------|------------------------------|--------|
| 0     | Requirements & Design         | ✅ Done |
| 1     | Project Setup                 | ✅ Done |
| 2     | Basic Java Backend             | ✅ Done |
| 3     | Database                      | ✅ Done |
| 4     | Authentication                | ✅ Done |
| 5     | Traffic Data Collection       | ✅ Done |
| 6     | ML Model                      | ✅ Done |
| 7     | Java ↔ ML Integration          | ✅ Done |
| 8     | Redis                         | ✅ Done |
| 9     | Kafka                         | ✅ Done |
| 10    | Route Recommendation          | ✅ Done |
| 11    | Microservices Split           | ✅ Done |
| 12    | Database Migrations (Flyway)  | ✅ Done |
| 13    | Frontend / UI                 | ✅ Done |
| 14    | Containerization (Docker)     | ✅ Done |
| 15    | Testing                       | ✅ Done |
| 16    | Security Hardening            | ✅ Done |
| 17    | CI/CD                         | ✅ Done |
| 18    | Cloud Deployment              | ✅ Done |
| 19    | Observability & Monitoring    | ✅ Done |
| 20    | Resilience & Scale            | ✅ Done |
| 21    | Frontend Production Polish    | ✅ Done |

## Architecture

```
                        ┌──────────────────────────────────────────────┐
   Browser (React SPA)  │  frontend  (nginx: static + reverse proxy)   │
   localhost:3000 (dev) │                                              │
   port 80/443 (prod)   └──────────────────────┬───────────────────────┘
                                               │ /api/**  /auth/**  /actuator/health
                                        ┌──────▼────────┐
                                        │   api-gateway   │  Spring Cloud Gateway (port 8080)
                                        │   (single entry)│  aggregates downstream health
                                        └─┬──┬──┬──┬──┬───┘
                              ┌────┬──────┴─┐│  │  │└──┬──────┬──────┐
                              ▼    ▼        ▼ ▼  ▼    ▼      ▼      ▼
                        ┌────────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐
                        │ auth   │ │traffic│ │routing│ │  ml  │ │prometheus│
                        │ 8091   │ │ 8082  │ │ 8083  │ │ 8010 │ │  grafana │
                        │ JWT/BCR│ │ segs  │ │Dijkstra│ │FastAPI│ │  (obs)   │
                        └───┬────┘ └───┬───┘ └───┬───┘ └──┬───┘ └────┬─────┘
                            │          │         │         │          │
                       ┌────▼───┐ ┌────▼────┐ ┌──▼────┐ ┌──▼──────┐   │
                       │Postgres│ │  Redis  │ │ Kafka │ │  models │   │
                       │        │ │ (cache) │ │ (raw +│ │ joblib  │   │
                       └────────┘ └─────────┘ │ routed)│ └─────────┘   │
                                             └────────┘                  │
                                  ┌──────────────────────────────────────┘
```

**Data flow (simplified):**
1. **Ingestion** — TomTom Flow Segment API (polled by `traffic-service`, ~1/min) OR manual
   `POST /api/traffic` → published to Kafka topic `traffic-readings`.
2. **Persistence** — `traffic-service` Kafka consumer persists readings into Postgres.
3. **Cache** — predictions are cached in Redis (1h, keyed by segment + hour); repeated
   calls skip `ml-service`.
4. **ML** — `ml-service` (FastAPI + scikit-learn) trains on history and predicts congestion
   for a segment at a given hour of day.
5. **Routing** — `routing-service` consumes `traffic-readings` to keep live edge weights,
   then runs Dijkstra on demand.
6. **UI** — React SPA talks only to the gateway; the gateway talks to services directly.

## Tech stack

| Layer        | Tech |
|--------------|------|
| Java backend | Java 17, Spring Boot 3.3, Spring Cloud Gateway, Resilience4j, Flyway |
| Python ML    | FastAPI, scikit-learn, joblib, prometheus-client |
| Frontend     | React 18, TypeScript, Vite, nginx |
| Data store   | PostgreSQL 16, Redis 7, Apache Kafka 3.8 |
| Auth         | Spring Security, JWT (jjwt), BCrypt |
| Observability| Prometheus, Grafana, structured JSON (ECS) logs |
| CI/CD        | GitHub Actions → ghcr.io images |

## Running locally (docker compose)

**Prerequisites:** Docker + Docker Compose (v2.24+ for the resource-limit syntax in the
prod overlay; the dev file itself has no such requirement).

```bash
# 1. Create secrets file (generate JWT_SECRET first: openssl rand -base64 48)
copy .env.prod.example .env     # then fill in real values

# 2. Start the whole stack
docker compose up -d --build

# 3. Wait for everything to be healthy
docker ps
```

### Ports (local dev)

| Service        | Host port | Purpose |
|----------------|-----------|---------|
| frontend       | 3000      | React SPA + reverse proxy → gateway |
| api-gateway    | 8080      | Single API entry point |
| auth-service   | 8091      | (internal, exposed for debugging) |
| traffic-service| 8082      | (internal, exposed for debugging) |
| routing-service| 8083      | (internal, exposed for debugging) |
| ml-service     | 8010      | (internal, exposed for debugging) |
| prometheus     | 9090      | Metrics UI |
| grafana        | 3001      | Dashboards |
| postgres       | 5433      | DB (maps to :5432 in container) |
| redis          | 6379      | Cache |
| kafka          | 9092      | Messaging |

**Take the UI for a spin:** open http://localhost:3000, register, then the app lets you
browse the road network (19 seeded segments incl. Dehradun: DIT, UPES, colleges, etc.),
view live/predicted congestion, plan routes (e.g. DIT → UPES), and manually ingest readings.

Run tests on any service:

```bash
cd auth-service && mvn test
```

## Production deployment (Phase 18)

The base `docker-compose.yml` is tuned for local development (all ports exposed, ready for
debugging). The **production overlay** hardens it:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

What changes:

- Infra + internal service ports are stripped (`!reset`) — Postgres/Redis/Kafka/Prometheus/
  Grafana/ml/auth/traffic/routing are reachable **only** on the internal compose network.
- Only two entry points stay published: `frontend` on `${FRONTEND_PORT:-80}` and the
  gateway on `${GATEWAY_PORT:-8080}`.
- Every container gets `restart: unless-stopped` (survives reboots/crashes).
- Resource limits (CPU/memory) applied via `deploy.resources`.

### Secrets & environment

Copy `.env.prod.example` to `.env` on the server and fill in:

- `JWT_SECRET` — random base64 (`openssl rand -base64 48`), **identical across all services**.
- `POSTGRES_PASSWORD` — superuser password for the Postgres container.
- `TOMTOM_API_KEY` — optional; ingestion no-ops without it.
- `FRONTEND_PORT` / `GATEWAY_PORT`.

**TLS:** terminate HTTPS at your cloud load balancer or an nginx/caddy reverse proxy in
front of `frontend`/`api-gateway`. The gateway ships a self-signed dev keystore
(`tls` profile) for local experiments only — never expose it to the internet.

### Deploying to a cloud host

Images are built and published to GHCR on every push to `main` (the `CI` workflow).
To stand up a server:

1. Point the `Deploy` workflow at your VPS → add repo secrets (`VPS_HOST`, `VPS_USER`,
   `VPS_SSH_KEY`) plus `JWT_SECRET` / `POSTGRES_PASSWORD`) → run the workflow manually
   from the Actions tab. It copies the compose files + `.env`, pulls the GHCR images,
   starts the stack and health-checks the gateway.
2. Manually (any host with Docker): copy `docker-compose*.yml` + `.env`, then
   `docker compose -f docker-compose.yml -f docker-compose.prod.yml pull && ... up -d`.

The `CI` images are tagged `latest` and `${{ sha }}` on
`ghcr.io/kartikeyamaulekhi/smart-traffic-system/<service>`.

## API reference (via the gateway)

All `/api/**` endpoints require `Authorization: Bearer <token>`.

### Auth — `/auth/**` (no token needed)

| Method | Path            | Body / notes |
|--------|-----------------|--------------|
| POST   | `/auth/register`| `{ "email": "...", "password": "..." }` (≥8 chars) → `{ token, email, role, expiresInMs }` |
| POST   | `/auth/login`   | same body → same response shape |

```powershell
$auth = Invoke-RestMethod -Uri http://localhost:3000/auth/register -Method Post `
  -ContentType "application/json" -Body '{"email":"you@example.com","password":"password123"}'
$headers = @{ Authorization = "Bearer $($auth.token)" }
```

### Road segments — `/api/road-segments`

| Method | Path                     | Description |
|--------|--------------------------|-------------|
| GET    | `/api/road-segments`     | List (seeded: 4 classic + Dehradun seed set) |
| POST   | `/api/road-segments`     | Create `{ name, city, startLat, startLng, endLat, endLng }` |
| GET    | `/api/road-segments/{id}`| One segment |
| PUT    | `/api/road-segments/{id}`| Update |
| DELETE | `/api/road-segments/{id}`| Delete |

### Traffic data — `/api/traffic`

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/traffic` | Ingest `{ roadSegmentId, vehicleCount, avgSpeedKmh, congestionLevel, source }` |
| GET    | `/api/traffic/{id}` | One reading |
| GET    | `/api/traffic/segment/{roadSegmentId}` | Full history |
| GET    | `/api/traffic/segment/{roadSegmentId}/latest` | Most recent |
| DELETE | `/api/traffic/{id}` | Delete |

### Predictions — `/api/predictions`

`GET /api/predictions/{roadSegmentId}` (optionally `?timestamp=2026-09-14T09:00:00`)
returns the ML model's predicted congestion for the segment at that hour. Responses are
cached 1h in Redis (key `predictions::{segment}_{yyyy-MM-dd'T'HH:00}`), so repeated calls
never hit `ml-service`. If `ml-service` is down or never trained, the circuit breaker
degrades this to a fresh `503` instead of hanging.

### Routes — `/api/routes`

`POST /api/routes` with `{ originLat, originLng, destinationLat, destinationLng }`
returns the fastest path via Dijkstra, weighted by live/predicted travel time:

```powershell
$body = @{ originLat = 29.968; originLng = 77.546; destinationLat = 29.972; destinationLng = 77.551 } `
  | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:3000/api/routes -Method Post -ContentType "application/json" `
  -Headers $headers -Body $body
```

### Health

`GET /actuator/health` on the gateway aggregates auth / traffic / routing downstream
health (each with the resilience "circuitBreakers" + "retries" indicators). The frontend
shows a live green/yellow/red dot from this endpoint.

## Resilience (Phase 20)

Every inter-service call is protected by a **circuit breaker + retry** (Resilience4j,
programmatic, no aspect weaving):

- `traffic-service → ml-service` (predict path) — breaker instance `ml`
- `routing-service → traffic-service` (route time path) — breaker instance `traffic`

Config (both services): sliding window 10 calls, min 5 to evaluate, fail-open 50%,
wait 10 s in OPEN, 3 permitted calls in HALF_OPEN; retry 3 attempts with 1 s backoff.

Watch it degrade in production dashboards:

```
resilience4j_circuitbreaker_state
resilience4j_circuitbreaker_failed_calls
resilience4j_retry_calls_total{name="ml"}
```

Actuator exposes probe state too: `GET /actuator/circuitbreakers`, `GET /actuator/retries`.

## Observability (Phase 19)

- **Prometheus** scrapes every service (`/actuator/prometheus`, `ml-service /metrics`)
  into one fleet of dashboards at http://localhost:9090.
- **Grafana** (http://localhost:3001, default `admin/admin`) is pre-provisioned with the
  data source + a "Smart Traffic" dashboard: request rates, JVM, DB pools, Kafka lag, ML
  predictions, circuit-breaker states.
- All Java services emit **structured JSON (ECS) logs** — ready for Loki/CloudWatch with no
  code changes.
- Grafana datasources/dashboards live in `docker/grafana/` and are baked into the stack, no
  manual setup.

## Testing & CI/CD (Phases 15 & 17)

- **Unit/API tests** — 75 tests across the 4 Java services (auth 18, traffic 27,
  routing 27, gateway 3) covering security, services, controllers and resilience.
- **GitHub Actions (`CI`)** on every push/PR: `mvn test` for the 4 services, then builds &
  pushes all 6 images (`auth-service`, `traffic-service`, `routing-service`, `api-gateway`,
  `ml-service`, `frontend`) to GHCR with `latest` + SHAs tags.
- **`Deploy`** workflow (manual) scrolls the image stack onto a VPS over SSH (see above).

## Project layout

```
smart-traffic-system/
├── docker-compose.yml          # local-dev stack (all ports exposed)
├── docker-compose.prod.yml     # production overlay (hardened, limited ports)
├── .env                        # local secrets (gitignored)
├── .env.prod.example           # production secrets template
├── docker/                     # postgres-init, prometheus.yml, grafana provisioning
├── .github/workflows/          # ci.yml (test+build+push), deploy.yml (SSH deploy)
├── api-gateway/                # Spring Cloud Gateway, downstream health, TLS profile
├── auth-service/               # Spring Security + JWT + BCrypt, its own DB (Flyway)
├── traffic-service/            # segments, traffic-data, predictions, TomTom polling,
│                               # Redis cache, Kafka producer+consumer, ml client (CB+RT)
├── routing-service/            # Dijkstra routing, Kafka consumer + local graph cache,
│                               # traffic client (CB+RT)
├── ml-service/                 # FastAPI + scikit-learn training & prediction service
└── frontend/                   # React 18 + TS + Vite SPA (nginx reverse proxy)
```

## How the parts talk (key flows)

**Ingestion ⇄ Kafka** — the poller streams readings to `traffic-readings`; `traffic-service`
and `routing-service` each have a consumer. Manual `POST /api/traffic` still writes
synchronously (the caller expects an immediate response). Watch two log lines per poll:

```
Published traffic reading to Kafka for segment 1 (MG Road): 38 km/h -> MEDIUM
Persisted traffic reading from Kafka for segment 1: 38.0 km/h -> MEDIUM
```

**Prediction caching (Redis)** — because the model's features are hour-granular, the key is
`predictions::{segment}_{yyyy-MM-dd'T'HH:00}` and TTL is 1 h. A second call within the same
hour returns straight from Redis — the console shows only one "Cache miss … calling ML".
Inspect live with Memurai/`redis-cli`:

```
GET predictions::1_2026-09-14T09:00
```

**Manual ML training** — `ml-service` won't predict until it has a model:

```bash
docker compose exec ml-service python train.py
docker compose restart ml-service
```

It learns hour-of-day / day-of-week patterns from the traffic history in Postgres
(intentionally **not** current speed — you can't know current speed for a future time).
Once it's ready, `POST /predict` returns e.g.
`{ "road_segment_id": 1, "timestamp": "...", "predicted_congestion_level": "LOW", "confidence": 0.62 }`.

## Notes

- Local dev Postgres exposes `5433` (never 5432 against a system Postgres). The init
  scripts under `docker/postgres-init` create the per-service databases on first boot.
- The old monolithic `backend/` module was retired mid-Phase 11 and lives in `_archive/`.