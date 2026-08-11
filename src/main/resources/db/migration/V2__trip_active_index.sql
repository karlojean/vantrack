CREATE UNIQUE INDEX idx_trip_active_by_route
ON trips (route_id)
WHERE status = 'ACTIVE';