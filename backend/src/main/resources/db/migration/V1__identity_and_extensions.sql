-- V1 · Extensiones, enums y bounded context identity.
-- Especificación: docs/05-modelo-datos.md §2 y §3. Forwards-only: no editar tras aplicar.

CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE user_status AS ENUM ('UNVERIFIED', 'ACTIVE', 'LOCKED');
CREATE TYPE oauth_provider AS ENUM ('GOOGLE', 'GITHUB');
CREATE TYPE unit_system AS ENUM ('KG', 'LB');
CREATE TYPE theme AS ENUM ('DARK', 'LIGHT');

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    email citext NOT NULL UNIQUE,
    password_hash text,
    display_name varchar(80) NOT NULL,
    status user_status NOT NULL DEFAULT 'UNVERIFIED',
    locale char(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es', 'en')),
    unit_system unit_system NOT NULL DEFAULT 'KG',
    theme theme NOT NULL DEFAULT 'DARK',
    time_zone varchar(64) NOT NULL DEFAULT 'Europe/Madrid',
    default_rest_seconds smallint NOT NULL DEFAULT 90
        CHECK (default_rest_seconds BETWEEN 15 AND 600),
    timer_sound boolean NOT NULL DEFAULT true,
    email_verified_at timestamptz,
    health_consent_at timestamptz NOT NULL,
    failed_login_count smallint NOT NULL DEFAULT 0,
    locked_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE oauth_accounts (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider oauth_provider NOT NULL,
    provider_account_id varchar(255) NOT NULL,
    email_at_link citext,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_oauth_provider_account UNIQUE (provider, provider_account_id)
);

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts (user_id);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    replaced_by_id uuid REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    user_agent_hash char(64),
    ip_hash char(64)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE email_verification_tokens (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_email_verification_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_email_verification_user_id ON email_verification_tokens (user_id);
CREATE UNIQUE INDEX uq_email_verification_active
    ON email_verification_tokens (user_id)
    WHERE used_at IS NULL;

CREATE TABLE password_reset_tokens (
    id uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_password_reset_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_password_reset_user_id ON password_reset_tokens (user_id);
CREATE UNIQUE INDEX uq_password_reset_active
    ON password_reset_tokens (user_id)
    WHERE used_at IS NULL;
