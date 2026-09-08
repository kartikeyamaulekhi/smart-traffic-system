# ML prediction service - Python FastAPI
FROM python:3.11-slim

WORKDIR /app

# Copy dependency manifest first for layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy app source
COPY config.py features.py train.py main.py ./

EXPOSE 8010

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8010"]
