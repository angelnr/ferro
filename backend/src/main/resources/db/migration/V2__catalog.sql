-- V2 · Bounded context catalog. El seed de ejercicios llega en M2; aquí solo el esquema.

CREATE TYPE muscle_group AS ENUM (
    'CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'QUADS', 'HAMSTRINGS',
    'GLUTES', 'CALVES', 'CORE', 'FOREARMS', 'NECK', 'FULL_BODY'
);

CREATE TYPE equipment AS ENUM (
    'BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BODYWEIGHT', 'KETTLEBELL', 'BAND', 'OTHER'
);

CREATE TYPE mechanics AS ENUM ('COMPOUND', 'ISOLATION');

CREATE TABLE exercises (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    name_es varchar(120) NOT NULL,
    name_en varchar(120) NOT NULL,
    muscle_group muscle_group NOT NULL,
    secondary_muscles muscle_group[] NOT NULL DEFAULT '{}',
    equipment equipment NOT NULL,
    mechanics mechanics NOT NULL,
    bodyweight boolean NOT NULL DEFAULT false,
    is_custom boolean NOT NULL DEFAULT false,
    owner_id uuid REFERENCES users (id) ON DELETE CASCADE,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_exercises_custom_owner CHECK (is_custom = (owner_id IS NOT NULL))
);

CREATE INDEX idx_exercises_name_es_trgm ON exercises USING gin (name_es gin_trgm_ops);
CREATE INDEX idx_exercises_name_en_trgm ON exercises USING gin (name_en gin_trgm_ops);
CREATE INDEX idx_exercises_muscle_group ON exercises (muscle_group);
CREATE INDEX idx_exercises_equipment ON exercises (equipment);
CREATE INDEX idx_exercises_custom_owner
    ON exercises (owner_id)
    WHERE is_custom AND NOT archived;
