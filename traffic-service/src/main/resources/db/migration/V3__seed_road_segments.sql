-- V3: Seed the baseline Saharanpur road network (traffic-service)
--
-- Every fresh deployment gets these four segments, so the routing graph,
-- predictions and live ingestion have something to work with out of the box.
-- Idempotent: existing rows (created via the API) are left untouched, and the
-- identity sequence is bumped past the seed ids afterwards.

INSERT INTO road_segments (id, name, city, start_lat, start_lng, end_lat, end_lng, created_at)
VALUES
    (1, 'MG Road',          'Saharanpur', 29.968,  77.546, 29.972,  77.551, '2026-01-01 00:00:00+00'),
    (2, 'Civil Lines Road', 'Saharanpur', 29.972,  77.551, 29.9685, 77.555, '2026-01-01 00:00:00+00'),
    (3, 'Station Road',     'Saharanpur', 29.968,  77.546, 29.974,  77.558, '2026-01-01 00:00:00+00'),
    (4, 'Ring Road',        'Saharanpur', 29.974,  77.558, 29.9685, 77.555, '2026-01-01 00:00:00+00')
ON CONFLICT (id) DO NOTHING;

-- Keep the identity sequence ahead of the seeded ids so future API-created
-- segments can never collide with them.
SELECT setval(
    pg_get_serial_sequence('road_segments', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 0) FROM road_segments), 4)
);