-- V4: Seed the Dehradun road network (Mussoorie Road, NH-72/Premnagar corridor)
-- and dedicated approach roads to the four university campuses + IMS Unison.
-- Idempotent: existing rows are left untouched; identity is bumped to 19.
--
INSERT INTO road_segments (id, name, city, start_lat, start_lng, end_lat, end_lng, created_at) VALUES
    (5, 'Mussoorie Road  Clock Tower to City Junction', 'Dehradun', 30.3248, 78.0413, 30.3157, 78.0334, '2026-01-01 00:00:00+00'),
    (6, 'Mussoorie Road  City Junction to ISBT', 'Dehradun', 30.3157, 78.0334, 30.28782, 77.99803, '2026-01-01 00:00:00+00'),
    (7, 'Mussoorie Road  ISBT to Mahadev Chowk', 'Dehradun', 30.28782, 77.99803, 30.295, 78.015, '2026-01-01 00:00:00+00'),
    (8, 'Mussoorie Road  Mahadev Chowk to Bhagwanpur', 'Dehradun', 30.295, 78.015, 30.31, 78.035, '2026-01-01 00:00:00+00'),
    (9, 'Mussoorie Road  Bhagwanpur to Dallanwala', 'Dehradun', 30.31, 78.035, 30.33, 78.06, '2026-01-01 00:00:00+00'),
    (10, 'Mussoorie Road  Dallanwala to Diversion', 'Dehradun', 30.33, 78.06, 30.376, 78.075, '2026-01-01 00:00:00+00'),
    (11, 'Mussoorie Diversion  DIT University Approach', 'Dehradun', 30.376, 78.075, 30.39934, 78.07535, '2026-01-01 00:00:00+00'),
    (12, 'DIT Campus Link  IMS Unison University', 'Dehradun', 30.39934, 78.07535, 30.39984, 78.0778, '2026-01-01 00:00:00+00'),
    (13, 'Bell Road  Graphic Era University Approach', 'Dehradun', 30.28782, 77.99803, 30.268, 77.99601, '2026-01-01 00:00:00+00'),
    (14, 'Chakrata Road  Shastri Nagar to Premnagar', 'Dehradun', 30.28782, 77.99803, 30.321, 77.976, '2026-01-01 00:00:00+00'),
    (15, 'Chakrata Road  Premnagar', 'Dehradun', 30.321, 77.976, 30.3541, 77.946, '2026-01-01 00:00:00+00'),
    (16, 'Uttaranchal University Approach  Premnagar', 'Dehradun', 30.3541, 77.946, 30.33994, 77.95094, '2026-01-01 00:00:00+00'),
    (17, 'NH-72  Premnagar to Bidholi I', 'Dehradun', 30.3541, 77.946, 30.385, 77.958, '2026-01-01 00:00:00+00'),
    (18, 'NH-72  Bidholi II', 'Dehradun', 30.385, 77.958, 30.4, 77.963, '2026-01-01 00:00:00+00'),
    (19, 'NH-72  Bidholi to UPES', 'Dehradun', 30.4, 77.963, 30.41717, 77.96819, '2026-01-01 00:00:00+00')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('road_segments', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM road_segments), 19));
