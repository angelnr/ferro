-- V7 · Series objetivo pendientes al iniciar una sesión desde una rutina (RN-24).
-- Se guardan aparte de session_sets para no mezclar objetivos con series reales.

CREATE TABLE session_target_sets (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    session_exercise_id uuid NOT NULL REFERENCES session_exercises (id) ON DELETE CASCADE,
    position smallint NOT NULL CHECK (position >= 0),
    set_type set_type NOT NULL DEFAULT 'NORMAL',
    target_reps smallint NOT NULL CHECK (target_reps BETWEEN 1 AND 1000),
    target_weight numeric(6, 2),
    target_rir smallint CHECK (target_rir BETWEEN 0 AND 10),
    CONSTRAINT uq_session_target_sets_position UNIQUE (session_exercise_id, position)
);

CREATE INDEX idx_session_target_sets_exercise ON session_target_sets (session_exercise_id);
