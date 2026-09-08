# 🚦 Smart Traffic Recognition & Prediction System

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
| 11    | Microservices Split           | 🔄 In progress |
| 12    | Database Migrations (Flyway)  | ⬜ |
| 13    | Frontend / UI                 | ⬜ |
| 14    | Containerization (Docker)     | ⬜ |
| 15    | Testing                       | ⬜ |
| 16    | Security Hardening            | ⬜ |
| 17    | CI/CD                         | ⬜ |
| 18    | Cloud Deployment              | ⬜ |
| 19    | Observability & Monitoring    | ⬜ |
| 20    | Resilience & Scale            | ⬜ |

### Phase 11 — Microservices Split (in progress)

| Sub-task | Status |
|----------|--------|
| 11a. auth-service — register/login/JWT issuing, own DB | ✅ Done |
| 11b. traffic-service — road-segments, traffic-data, predictions, TomTom polling, Redis, Kafka producer | ✅ Done |
| 11c. routing-service — Dijkstra routing, Kafka consumer + local cache | ⬜ |
| 11d. api-gateway — Spring Cloud Gateway, single entry point | ⬜ |
| 11e. Decommission old `backend` module once nothing depends on it | ⬜ |

## Stack

- **Backend:** Java 17, Spring Boot 3.3.4, Maven
- **DB:** PostgreSQL (dev/prod), H2 (test)
- **Auth:** Spring Security + JWT (jjwt)
- **ML:** Python / FastAPI / scikit-learn (added in Phase 6)
- **Messaging:** Kafka (added in Phase 9)
- **Cache:** Redis (added in Phase 8)

## Running locally (Phase 1 checkpoint)

Prerequisites: Java 17, Maven (bundled with IntelliJ, or install separately), PostgreSQL running locally with a `smart_traffic` database.

```bash
cd backend
mvn spring-boot:run
```

Then check:

```bash
curl http://localhost:8080/health
```

Expected:

```json
{"status":"UP","service":"smart-traffic-backend","timestamp":"..."}
```

## Route recommendation (Phase 10)

`POST /api/routes` finds the fastest path between two points through your
road network, using Dijkstra's algorithm weighted by predicted travel time
(distance ÷ each segment's most recent real speed reading, or a 40 km/h
default for segments with no history yet).

**How it works:** every road segment's start/end coordinates become nodes in
a graph; every segment is a bidirectional edge. Your origin/destination get
"snapped" to the nearest existing node (within 2 km) so you don't need to
know exact graph coordinates.

```powershell
$body = @{ originLat = 29.968; originLng = 77.546; destinationLat = 29.972; destinationLng = 77.551 } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/routes -Method Post -ContentType "application/json" -Headers $headers -Body $body
```

With the 4-segment network (MG Road, Civil Lines Road, Station Road, Ring
Road), this specific request has two real path choices — a direct one
(MG Road or Civil Lines Road) vs. a 2-hop one (Station Road → Ring Road) —
and returns whichever is currently faster.

## Event streaming with Kafka (Phase 9)

`TrafficIngestionService` (the TomTom poller from Phase 5) no longer writes to
Postgres directly. It now **publishes** each reading onto a Kafka topic
(`traffic-readings`), and a separate `TrafficDataKafkaConsumer` is what
actually persists it. Manual `POST /api/traffic` calls still write directly
(a client expects an immediate response with the created record, which an
async event wouldn't give you) — only the automated ingestion pipeline is
decoupled this way.

**Setup:**
```bash
cd smart-traffic-system   # the project root, where docker-compose.yml lives
docker compose up -d
docker ps   # confirm a container named smart-traffic-kafka is running
```

**How to see it working:** restart the Java backend, wait ~1 minute, and
watch for TWO log lines per polling cycle instead of one:
```
Published traffic reading to Kafka for segment 1 (MG Road): 51 km/h (100% of free-flow) -> LOW
Persisted traffic reading from Kafka for segment 1: 51.0 km/h -> LOW
```
The first is the producer (ingestion), the second is the consumer (persistence) — two separate log statements from two separate classes, connected only by Kafka.

## Caching predictions with Redis (Phase 8)

`GET /api/predictions/{id}` is now cached for 1 hour, keyed by segment + hour
(minute-level differences don't matter since the model's features are
hour-granular anyway). A repeated call within that hour skips ml-service
entirely and returns straight from Redis.

**Setup:** install Memurai (Redis-compatible, free, native Windows) from
https://www.memurai.com/get-memurai — it installs as a background Windows
service, no code changes needed since it speaks the same protocol as real Redis.

**How to see it working:**
1. Call `GET /api/predictions/1` once — watch the Java console for `Cache miss - calling ML service for segment 1 at ...`
2. Call it again immediately with the exact same URL — **no new log line appears**, but you still get a response. That's the cache.
3. Inspect it directly (optional): open Memurai's CLI (`memurai-cli` or `redis-cli` if you have one), run `KEYS *` to see the cache key, then `GET predictions::1_2026-08-29T20:00` (adjust to your actual key).

## Predictions via the Java API (Phase 7)

The Java backend now calls ml-service directly, so clients only ever talk to one API.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/predictions/{roadSegmentId}` | Predicted congestion right now (requires auth, same as other `/api/**`) |
| GET | `/api/predictions/{roadSegmentId}?timestamp=2026-08-30T09:00:00` | Predicted congestion for a specific future time |

Requires **both** services running: the Java backend (port 8080) AND `ml-service`'s
`uvicorn main:app --port 8000`. If ml-service is down or has no trained model,
this endpoint returns a `503` with a clear message rather than a generic error.

```powershell
$headers = @{ Authorization = "Bearer $($auth.token)" }
Invoke-RestMethod -Uri http://localhost:8080/api/predictions/1 -Headers $headers
```

## ML prediction service (Phase 6)

A separate Python service in `ml-service/` trains and serves a congestion-level
predictor from real traffic history in Postgres.

**What it predicts:** given a road segment + a time (hour of day, day of week),
what congestion level is typical? It intentionally does NOT use current speed
as input — that would be cheating, since you won't know the current speed for
a *future* time you're trying to predict.

**One-time setup:**
```bash
cd ml-service
python -m venv venv
venv\Scripts\activate          # Windows
pip install -r requirements.txt
copy .env.example .env         # Windows (or `cp` on Mac/Linux)
# edit .env if your Postgres credentials differ from the defaults
```

**Train the model** (re-run any time you want it to learn from fresh data):
```bash
python train.py
```
With only a little data, it'll skip evaluation and just fit on everything it has — that's expected. Let the Java backend's scheduler (Phase 5) keep running for a while (it adds ~1 row per segment per minute) before expecting a meaningful accuracy report.

**Serve predictions:**
```bash
uvicorn main:app --reload --port 8000
```
Then:
```bash
curl -X POST http://localhost:8000/predict -H "Content-Type: application/json" -d "{\"road_segment_id\": 1}"
```
Returns something like:
```json
{"road_segment_id":1,"timestamp":"2026-08-29T20:15:00","predicted_congestion_level":"LOW","confidence":0.62}
```

## Live traffic ingestion (Phase 5)

Every `poll-interval-ms` (default 60s), `TrafficIngestionService` fetches live speed data from
TomTom's Flow Segment Data API for every existing road segment's midpoint, converts it into a
congestion level, and saves it exactly like a manual `POST /api/traffic` would.

**Setup:**
1. Get a free key at https://developer.tomtom.com/
2. Set it as an environment variable named `TOMTOM_API_KEY` (see below) — do NOT paste it directly into `application.yml`.
3. Restart the app. Watch the console for lines like:
   `Ingested live traffic for segment 1 (MG Road): 38 km/h (64% of free-flow) -> MEDIUM`

**Setting the environment variable in IntelliJ:** Run → Edit Configurations → select `BackendApplication` → Environment variables → add `TOMTOM_API_KEY=<your key>`.

**One-time database fix required:** the `traffic_data.source` column has a CHECK constraint
that only allowed the original 4 sources. Adding `API` as a 5th source needs a manual SQL fix
(Hibernate's `ddl-auto: update` does not alter existing constraints). Run this once in psql:
```sql
ALTER TABLE traffic_data DROP CONSTRAINT traffic_data_source_check;
ALTER TABLE traffic_data ADD CONSTRAINT traffic_data_source_check
    CHECK (source IN ('SENSOR','CAMERA','GPS','MANUAL','API'));
```

Without a real key configured, ingestion safely no-ops (logs a debug line, does nothing) rather
than throwing errors — so the app runs fine even before you've set up TomTom.

## Authentication (Phase 4)

`/api/**` now requires a JWT. `/auth/**` and `/health` stay open.

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/auth/register` | Create an account `{ "email": "...", "password": "..." }` (min 8 chars) → returns a JWT |
| POST   | `/auth/login` | `{ "email": "...", "password": "..." }` → returns a JWT |

To call any `/api/**` endpoint, add the header: `Authorization: Bearer <token>`

```powershell
$auth = Invoke-RestMethod -Uri http://localhost:8080/auth/register -Method Post -ContentType "application/json" -Body '{"email":"you@example.com","password":"password123"}'
$headers = @{ Authorization = "Bearer $($auth.token)" }
Invoke-RestMethod -Uri http://localhost:8080/api/road-segments -Headers $headers
```

## API endpoints (Phase 2 checkpoint)

**Road Segments** — `/api/road-segments`
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/road-segments` | Create a road segment |
| GET    | `/api/road-segments` | List all road segments |
| GET    | `/api/road-segments/{id}` | Get one road segment |
| PUT    | `/api/road-segments/{id}` | Update a road segment |
| DELETE | `/api/road-segments/{id}` | Delete a road segment |

**Traffic Data** — `/api/traffic`
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/traffic` | Ingest a traffic reading |
| GET    | `/api/traffic/{id}` | Get one reading |
| GET    | `/api/traffic/segment/{roadSegmentId}` | Full history for a segment |
| GET    | `/api/traffic/segment/{roadSegmentId}/latest` | Most recent reading for a segment |
| DELETE | `/api/traffic/{id}` | Delete a reading |

Example request body for `POST /api/road-segments`:
```json
{
  "name": "MG Road - Sector 14",
  "city": "Saharanpur",
  "startLat": 29.9680,
  "startLng": 77.5460,
  "endLat": 29.9720,
  "endLng": 77.5510
}
```

Example request body for `POST /api/traffic` (use the `id` returned above as `roadSegmentId`):
```json
{
  "roadSegmentId": 1,
  "vehicleCount": 145,
  "avgSpeedKmh": 22.5,
  "congestionLevel": "HIGH",
  "source": "SENSOR"
}
```

## Project layout

```
smart-traffic-system/
└── backend/
    ├── pom.xml
    ├── src/main/java/com/smarttraffic/backend/
    │   ├── BackendApplication.java
    │   ├── config/        # SecurityConfig (temporary, replaced in Phase 4)
    │   ├── controller/    # RoadSegmentController, TrafficDataController, HealthController
    │   ├── service/       # RoadSegmentService, TrafficDataService
    │   ├── repository/    # RoadSegmentRepository, TrafficDataRepository
    │   ├── model/         # RoadSegment, TrafficData, CongestionLevel, TrafficSource
    │   ├── dto/           # *Request / *Response objects
    │   └── exception/     # ResourceNotFoundException, GlobalExceptionHandler, ErrorResponse
    └── src/main/resources/application.yml
```
