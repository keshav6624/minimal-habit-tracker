-- V2: habits.
-- Deviation from an array column: schedule_days is TEXT holding a JSON array of
-- ISO day-of-week ints 1..7 (e.g. '[1,3,7]') so the DDL stays portable across
-- PostgreSQL and H2; the app maps it through a JPA AttributeConverter.
CREATE TABLE habits (
    id            UUID NOT NULL,
    user_id       UUID NOT NULL,
    name          VARCHAR(80) NOT NULL,
    description   VARCHAR(500),
    color         VARCHAR(24),
    icon          VARCHAR(40),
    schedule_type VARCHAR(20) NOT NULL,
    schedule_days TEXT NOT NULL DEFAULT '[]',
    start_date    DATE NOT NULL,
    archived_at   TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_habits PRIMARY KEY (id),
    CONSTRAINT fk_habits_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_habits_schedule_type
        CHECK (schedule_type IN ('DAILY', 'WEEKDAYS', 'CUSTOM'))
);

CREATE INDEX idx_habits_user_archived ON habits (user_id, archived_at);
