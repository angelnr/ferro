-- V5 · Flags globales (RF-082). El override se consulta en caliente; la tabla es la
-- fuente persistente y los properties del entorno son el valor por defecto.

CREATE TABLE feature_flags (
    key varchar(80) PRIMARY KEY,
    enabled boolean NOT NULL,
    description varchar(200) NOT NULL
);

INSERT INTO feature_flags (key, enabled, description) VALUES
    ('email.welcome', true, 'Email de bienvenida tras el registro'),
    ('email.security', true, 'Aviso de actividad de seguridad'),
    ('email.weekly-summary', false, 'Resumen semanal opcional (opt-in)'),
    ('email.session-autoclosed', true, 'Aviso de sesión cerrada automáticamente'),
    ('email.account-deleted', true, 'Confirmación de baja de cuenta'),
    ('timer.sound-default', true, 'Sonido del temporizador activado por defecto'),
    ('demo-account', false, 'Cuenta demo y datos sintéticos');
