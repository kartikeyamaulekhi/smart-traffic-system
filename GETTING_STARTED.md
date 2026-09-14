# 🚀 Getting started

Quick-start guide for running the **Smart Traffic Recognition & Prediction System** on your
own laptop. Two minutes from clone to a working dashboard.

## Prerequisites

- **Docker Desktop** — installed and running (this is the *only* hard requirement)
- **Git** (to clone) — or download the repo as a ZIP
- Terminal: **PowerShell** (Windows), **Terminal.app** (macOS), or **bash** (Linux)

> No Java, Python or Node install needed — everything runs inside containers.

## 1. Clone

```bash
git clone https://github.com/kartikeyamaulekhi/smart-traffic-system.git
cd smart-traffic-system
```

## 2. Create the secrets file

Generate a JWT signing secret, then create `.env` from the template:

```bash
openssl rand -base64 48
copy .env.prod.example .env     # Windows
cp .env.prod.example .env       # macOS / Linux
```

Open `.env` and paste the generated value into `JWT_SECRET=`.
`POSTGRES_PASSWORD` can stay as the dev default. `TOMTOM_API_KEY` is optional —
without it live traffic ingestion simply no-ops.

## 3. Build & start the stack

```bash
docker compose up -d --build
```

First run builds 6 images (a few minutes) and starts **11 containers**. Watch them come up:

```bash
docker ps
```

Postgres / Redis / Kafka should show `(healthy)`; every `smart-traffic-*` should be `Up`.

## 4. Train the ML model (one-time)

Predictions return `503` until a model exists. Train it once:

```bash
docker compose exec ml-service python train.py
docker compose restart ml-service
```

## 5. Open the app

Go to **http://localhost:3000** — register an account and you're in:

| What you can do | Where |
|-----------------|-------|
| Browse 19 road segments + live/predicted congestion | **Dashboard** |
| Plan a fastest route over a real Dehradun map (try *DIT University → UPES*) | **Route planner** |
| View history and manually ingest a reading | **History** |

Other UIs in the stack:

| URL | What it is |
|-----|------------|
| http://localhost:3000 | App (frontend + reverse proxy) |
| http://localhost:8080/actuator/health | Gateway aggregate health |
| http://localhost:9090 | Prometheus metrics |
| http://localhost:3001 | Grafana dashboards (login `admin` / `admin`) |

## Everyday commands

```bash
docker compose up -d            # start after a reboot / after down
docker compose down             # stop everything (databases persist in volumes)
docker compose logs -f traffic-service   # follow one service's logs
docker compose pull && docker compose up -d   # pull latest images
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `JWT_SECRET must be set in .env` on startup | `.env` is missing/empty — redo step 2 |
| Prediction endpoint returns `503` | ML model not trained — do step 4 |
| `port is already allocated` (3000 / 8080) | Another app uses those ports — change the host port in `docker-compose.yml` or stop that app |
| Map background is grey/blank on the Route planner | Map tiles come from OpenStreetMap — check your internet connection |
| Kafka container restarts a few times before `healthy` | Normal on first boot; wait ~30s and check `docker ps` |
| Containers started but app shows nothing | `docker compose logs api-gateway` and check for a JWT/DB error |

## Next steps

- **Production deployment** — see README → *Production deployment (Phase 18)* for the
  hardened compose overlay and the one-click SSH deploy workflow.
- **Resilience demo** — stop the ML container while a route planner hits predictions:
  `docker compose stop ml-service` — observe circuit breaker state in Grafana/Prometheus.