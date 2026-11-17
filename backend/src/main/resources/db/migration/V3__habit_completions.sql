-- V3: habit_completions (one row per habit per local calendar day).
CREATE TABLE habit_completions (
    id           UUID NOT NULL,
    habit_id     UUID NOT NULL,
    local_date   DATE NOT NULL,
    completed    BOOLEAN NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_habit_completions PRIMARY KEY (id),
    CONSTRAINT fk_habit_completions_habit FOREIGN KEY (habit_id)
        REFERENCES habits (id) ON DELETE CASCADE,
    CONSTRAINT uq_habit_completions_habit_date UNIQUE (habit_id, local_date)
);
