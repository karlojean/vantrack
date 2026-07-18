CREATE TABLE users
(
    id       UUID PRIMARY KEY,
    name     VARCHAR(200)        NOT NULL,
    email    VARCHAR(254) UNIQUE NOT NULL,
    password VARCHAR(255)        NOT NULL,
    role     VARCHAR(6)          NOT NULL,

    CONSTRAINT chk_role CHECK ( role IN ('ADMIN', 'DRIVER', 'PARENT') )
);

CREATE TABLE vans
(
    id        UUID PRIMARY KEY,
    plate     VARCHAR(7) UNIQUE NOT NULL,
    driver_id UUID              NOT NULL,

    CONSTRAINT fk_vans_driver_id FOREIGN KEY (driver_id) REFERENCES users (id)
);

CREATE TABLE routes
(
    id     UUID PRIMARY KEY,
    name   VARCHAR(200) NOT NULL,
    van_id UUID         NOT NULL,

    CONSTRAINT fk_routes_van_id FOREIGN KEY (van_id) REFERENCES vans (id)
);

CREATE TABLE user_routes
(
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL,
    route_id     UUID         NOT NULL,
    student_name VARCHAR(200) NOT NULL,

    CONSTRAINT fk_user_routes_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_routes_route_id FOREIGN KEY (route_id) REFERENCES routes (id)
);

CREATE TABLE trips
(
    id         UUID PRIMARY KEY,
    route_id   UUID NOT NULL,
    status     VARCHAR(9) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at   TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_trips_route_id FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT chk_status CHECK ( status IN ('ACTIVE', 'COMPLETED') )
);

CREATE TABLE trip_locations
(
    id         UUID PRIMARY KEY,
    trip_id    UUID NOT NULL,
    latitude   DECIMAL(9,6) NOT NULL,
    longitude  DECIMAL(9,6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_trip_locations_trip_id FOREIGN KEY (trip_id) REFERENCES trips (id)
);