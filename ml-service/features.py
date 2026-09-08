"""
Feature engineering shared between training and serving.

IMPORTANT: train.py and main.py must build features the exact same way,
or the model will silently make wrong predictions. Keeping this logic in
one place is what guarantees that.
"""
from datetime import datetime

import pandas as pd


def build_features(road_segment_id: int, timestamp: datetime) -> pd.DataFrame:
    """
    Turns (road_segment_id, timestamp) into the same feature row shape
    the model was trained on: road segment id, hour of day, day of week.
    """
    return pd.DataFrame([{
        "road_segment_id": road_segment_id,
        "hour_of_day": timestamp.hour,
        "day_of_week": timestamp.weekday(),  # Monday=0 ... Sunday=6
    }])


def build_training_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Same feature extraction, applied to a full dataframe of historical
    traffic_data rows (must have a 'recorded_at' column) for training.
    """
    features = pd.DataFrame({
        "road_segment_id": df["road_segment_id"],
        "hour_of_day": df["recorded_at"].dt.hour,
        "day_of_week": df["recorded_at"].dt.dayofweek,
    })
    return features
