package com.ferro.training.domain;

import java.util.List;

public record RoutinePage(List<RoutineSummary> content, int page, int size, long totalElements, int totalPages) {}
