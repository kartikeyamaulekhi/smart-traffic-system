"""
Serves congestion predictions over HTTP.

Run with:
    uvicorn main:app --reload --port 8000

Then:
    POST http://localhost:8000/predict
    { "road_segment_id": 1, "timestamp": "2026-08-29T18:30:00" }  (timestamp optional, defaults to now)
"""
from datetime import datetime
from typing import Optional

import joblib
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from prometheus_client import (CONTENT_TYPE_LATEST, Counter, Histogram,
                               generate_latest)
from starlette.responses import Response

from config import MODEL_PATH, ENCODER_PATH
from features import build_features

app = FastAPI(title="Smart Traffic - ML Prediction Service")

_model = None
_label_encoder = None

PREDICT_REQUESTS = Counter(
    "ml_predict_requests_total",
    "Prediction requests received",
    ["outcome"],
)
PREDICT_LATENCY = Histogram(
    "ml_predict_latency_seconds",
    "Prediction request latency in seconds",
)


def _load_model_if_needed():
    global _model, _label_encoder
    if _model is None:
        if not MODEL_PATH.exists() or not ENCODER_PATH.exists():
            raise HTTPException(
                status_code=503,
                detail="No trained model found yet. Run 'python train.py' first, "
                       "then restart this service."
            )
        _model = joblib.load(MODEL_PATH)
        _label_encoder = joblib.load(ENCODER_PATH)


class PredictRequest(BaseModel):
    road_segment_id: int
    timestamp: Optional[datetime] = None


class PredictResponse(BaseModel):
    road_segment_id: int
    timestamp: datetime
    predicted_congestion_level: str
    confidence: float


@app.get("/health")
def health():
    model_ready = MODEL_PATH.exists() and ENCODER_PATH.exists()
    return {"status": "UP", "model_ready": model_ready}


@app.get("/metrics")
def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    with PREDICT_LATENCY.time():
        try:
            _load_model_if_needed()

            timestamp = request.timestamp or datetime.now()
            X = build_features(request.road_segment_id, timestamp)

            probabilities = _model.predict_proba(X)[0]
            predicted_index = probabilities.argmax()
            predicted_label = _label_encoder.inverse_transform([predicted_index])[0]
            confidence = float(probabilities[predicted_index])

            PREDICT_REQUESTS.labels(outcome="success").inc()
            return PredictResponse(
                road_segment_id=request.road_segment_id,
                timestamp=timestamp,
                predicted_congestion_level=predicted_label,
                confidence=round(confidence, 4),
            )
        except HTTPException:
            PREDICT_REQUESTS.labels(outcome="model_unavailable").inc()
            raise
        except Exception:
            PREDICT_REQUESTS.labels(outcome="error").inc()
            raise
