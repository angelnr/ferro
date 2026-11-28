-- V10 · La aducción de cadera pertenece a aductores, no a glúteos.
UPDATE exercises
SET muscle_group = 'ADDUCTORS',
    secondary_muscles = array_remove(secondary_muscles, 'GLUTES'::muscle_group)
WHERE name_es = 'Aducción de cadera en máquina';

-- La sentadilla profunda y variantes trabajan también aductores como secundarios.
UPDATE exercises
SET secondary_muscles = array_append(secondary_muscles, 'ADDUCTORS'::muscle_group)
WHERE name_es IN (
    'Sentadilla búlgara',
    'Sentadilla búlgara con barra',
    'Sentadilla búlgara con mancuernas'
)
AND NOT ('ADDUCTORS'::muscle_group = ANY(secondary_muscles));
