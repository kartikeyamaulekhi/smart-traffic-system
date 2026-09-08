"""
Trains a congestion-level classifier from real traffic_data history in Postgres.

Run this manually whenever you want to retrain on fresh data:
    python train.py

What it predicts: given a road segment and a time (hour of day + day of week),
what congestion level (LOW/MEDIUM/HIGH/SEVERE) is typical?

It deliberately does NOT use avg_speed_kmh or vehicle_count as input features -
those describe *current* conditions, not something you'd know in advance.
The whole point is to predict from time/location alone.
"""
import sys

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

from config import get_engine, MODEL_PATH, ENCODER_PATH
from features import build_training_features

MIN_ROWS_TO_TRAIN = 20
MIN_ROWS_FOR_TEST_SPLIT = 50


def load_training_data() -> pd.DataFrame:
    engine = get_engine()
    query = """
        SELECT road_segment_id, recorded_at, congestion_level
        FROM traffic_data
        ORDER BY recorded_at
    """
    df = pd.read_sql(query, engine, parse_dates=["recorded_at"])
    return df


def main():
    print("Loading traffic_data from Postgres...")
    df = load_training_data()
    print(f"Loaded {len(df)} rows.")

    if len(df) < MIN_ROWS_TO_TRAIN:
        print(
            f"\nNot enough data yet to train a meaningful model "
            f"(have {len(df)} rows, want at least {MIN_ROWS_TO_TRAIN}).\n"
            f"Let the ingestion scheduler in the Java backend run longer "
            f"(it adds ~1 row per road segment per minute), then re-run this script."
        )
        sys.exit(0)

    X = build_training_features(df)
    y_raw = df["congestion_level"]

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(y_raw)

    model = RandomForestClassifier(n_estimators=200, random_state=42, class_weight="balanced")

    if len(df) >= MIN_ROWS_FOR_TEST_SPLIT:
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        print(f"\nHold-out accuracy: {accuracy_score(y_test, y_pred):.2%}")
        print("\nClassification report:")
        print(classification_report(
            y_test, y_pred,
            labels=range(len(label_encoder.classes_)),
            target_names=label_encoder.classes_,
            zero_division=0,
        ))

        # Retrain on the FULL dataset for the model we actually ship -
        # the split above was only to measure how good it is.
        model.fit(X, y)
    else:
        print(
            f"\nOnly {len(df)} rows - too few for a reliable train/test split "
            f"(want {MIN_ROWS_FOR_TEST_SPLIT}+). Training on all available data "
            f"without a held-out evaluation for now."
        )
        model.fit(X, y)

    joblib.dump(model, MODEL_PATH)
    joblib.dump(label_encoder, ENCODER_PATH)
    print(f"\nSaved model to {MODEL_PATH}")
    print(f"Saved label encoder to {ENCODER_PATH}")


if __name__ == "__main__":
    main()
