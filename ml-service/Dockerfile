# ML prediction service - Python FastAPI
FROM python:3.11-slim

WORKDIR /app

# Copy dependency manifest first for layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy app source
COPY config.py features.py train.py main.py ./

# Ship a pre-trained baseline model so predictions work on a fresh deployment
# (the compose named volume copies this in on first boot). Retrain in place
# with `docker compose exec ml-service python train.py` as data accumulates.
COPY models/ ./models/

EXPOSE 8010

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8010"]
