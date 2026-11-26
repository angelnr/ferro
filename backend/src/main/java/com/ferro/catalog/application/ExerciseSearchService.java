package com.ferro.catalog.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.catalog.application.port.ExerciseRepository;
import com.ferro.catalog.domain.Equipment;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.InvalidFilterException;
import com.ferro.catalog.domain.MuscleGroup;
import com.ferro.catalog.domain.PageResult;

@Service
public class ExerciseSearchService {

    private final ExerciseRepository exercises;

    public ExerciseSearchService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Transactional(readOnly = true)
    public PageResult<Exercise> search(UUID userId, String query, String muscle, String equipment, int page, int size) {
        MuscleGroup muscleGroup = parseMuscle(muscle);
        Equipment equipmentFilter = parseEquipment(equipment);
        return exercises.search(userId, query, muscleGroup, equipmentFilter, page, size);
    }

    private MuscleGroup parseMuscle(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MuscleGroup.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidFilterException("Grupo muscular no válido: " + value);
        }
    }

    private Equipment parseEquipment(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Equipment.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidFilterException("Equipamiento no válido: " + value);
        }
    }
}
