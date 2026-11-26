-- V4 · Bounded contexts progress y body.

CREATE TYPE pr_type AS ENUM ('MAX_WEIGHT', 'MAX_E1RM');

CREATE TABLE personal_records (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id uuid NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    pr_type pr_type NOT NULL,
    value numeric(8, 2) NOT NULL,
    achieved_at timestamptz NOT NULL,
    session_set_id uuid REFERENCES session_sets (id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_personal_records_current
    ON personal_records (user_id, exercise_id, pr_type, value DESC);
CREATE INDEX idx_personal_records_session_set_id ON personal_records (session_set_id);

CREATE TABLE body_measurements (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    measured_at timestamptz NOT NULL,
    weight numeric(5, 2) CHECK (weight BETWEEN 20 AND 400),
    chest numeric(5, 1) CHECK (chest BETWEEN 10 AND 250),
    waist numeric(5, 1) CHECK (waist BETWEEN 10 AND 250),
    hip numeric(5, 1) CHECK (hip BETWEEN 10 AND 250),
    arm numeric(5, 1) CHECK (arm BETWEEN 10 AND 250),
    thigh numeric(5, 1) CHECK (thigh BETWEEN 10 AND 250),
    notes varchar(500),
    CONSTRAINT ck_body_measurements_any_value CHECK (
        weight IS NOT NULL OR chest IS NOT NULL OR waist IS NOT NULL
        OR hip IS NOT NULL OR arm IS NOT NULL OR thigh IS NOT NULL
    )
);

CREATE INDEX idx_body_measurements_user_measured
    ON body_measurements (user_id, measured_at DESC);
