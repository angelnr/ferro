package com.ferro.catalog.interfaces.rest;

import java.util.List;

import com.ferro.catalog.domain.PageResult;

public record ExercisePageResponse(
        List<ExerciseResponse> content, int page, int size, long totalElements, int totalPages) {

    public static ExercisePageResponse from(PageResult<ExerciseResponse> page) {
        return new ExercisePageResponse(
                page.content(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
