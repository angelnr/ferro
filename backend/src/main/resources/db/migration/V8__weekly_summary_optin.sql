-- V8 · Opt-in del resumen semanal por email (RF-066).
ALTER TABLE users ADD COLUMN email_weekly_summary boolean NOT NULL DEFAULT false;
