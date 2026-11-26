-- V3 · Bounded context training: sesiones en vivo y rutinas.

CREATE TYPE set_type AS ENUM ('WARMUP', 'NORMAL', 'FAILURE', 'DROP');
CREATE TYPE session_status AS ENUM ('ACTIVE', 'FINISHED', 'AUTO_CLOSED', 'DISCARDED');

CREATE TABLE routines (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name varchar(80) NOT NULL,
    notes varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_routines_user_name ON routines (user_id, lower(name));
CREATE INDEX idx_routines_user_id ON routines (user_id);

CREATE TABLE routine_exercises (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    routine_id uuid NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    exercise_id uuid NOT NULL REFERENCES exercises (id) ON DELETE RESTRICT,
    position smallint NOT NULL CHECK (position >= 0),
    rest_seconds smallint CHECK (rest_seconds BETWEEN 15 AND 600),
    superset_group smallint,
    notes varchar(500),
    CONSTRAINT uq_routine_exercises_position UNIQUE (routine_id, position)
);

CREATE INDEX idx_routine_exercises_exercise_id ON routine_exercises (exercise_id);

CREATE TABLE routine_sets (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    routine_exercise_id uuid NOT NULL REFERENCES routine_exercises (id) ON DELETE CASCADE,
    position smallint NOT NULL CHECK (position >= 0),
    set_type set_type NOT NULL DEFAULT 'NORMAL',
    target_reps smallint NOT NULL CHECK (target_reps BETWEEN 1 AND 1000),
    target_weight numeric(6, 2),
    target_rir smallint CHECK (target_rir BETWEEN 0 AND 10),
    CONSTRAINT uq_routine_sets_position UNIQUE (routine_exercise_id, position)
);

CREATE TABLE workout_sessions (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    routine_id uuid REFERENCES routines (id) ON DELETE SET NULL,
    name varchar(80) NOT NULL,
    status session_status NOT NULL,
    started_at timestamptz NOT NULL,
    finished_at timestamptz,
    duration_seconds int CHECK (duration_seconds >= 0),
    total_volume numeric(10, 2) NOT NULL DEFAULT 0 CHECK (total_volume >= 0),
    notes varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_workout_sessions_finished CHECK (status = 'ACTIVE' OR finished_at IS NOT NULL),
    CONSTRAINT ck_workout_sessions_finished_after_start CHECK (finished_at IS NULL OR finished_at >= started_at)
);

-- RN-08: como máximo una sesión ACTIVE por usuario, garantizado por la BD.
CREATE UNIQUE INDEX uq_workout_sessions_active
    ON workout_sessions (user_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_workout_sessions_user_started
    ON workout_sessions (user_id, started_at DESC);
CREATE INDEX idx_workout_sessions_finished
    ON workout_sessions (user_id)
    WHERE status IN ('FINISHED', 'AUTO_CLOSED');

CREATE TABLE session_exercises (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    session_id uuid NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    exercise_id uuid NOT NULL REFERENCES exercises (id) ON DELETE RESTRICT,
    position smallint NOT NULL CHECK (position >= 0),
    rest_seconds smallint CHECK (rest_seconds BETWEEN 15 AND 600),
    superset_group smallint,
    notes varchar(500),
    CONSTRAINT uq_session_exercises_position UNIQUE (session_id, position)
);

CREATE INDEX idx_session_exercises_exercise_id ON session_exercises (exercise_id);

CREATE TABLE session_sets (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    session_exercise_id uuid NOT NULL REFERENCES session_exercises (id) ON DELETE CASCADE,
    position smallint NOT NULL CHECK (position >= 0),
    set_type set_type NOT NULL DEFAULT 'NORMAL',
    reps smallint NOT NULL CHECK (reps BETWEEN 1 AND 1000),
    -- RN-13: en bodyweight el peso es lastre; negativo = asistencia.
    weight numeric(6, 2) NOT NULL CHECK (weight BETWEEN -1000 AND 1000),
    rir smallint CHECK (rir BETWEEN 0 AND 10),
    notes varchar(500),
    logged_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_session_sets_position UNIQUE (session_exercise_id, position)
);
