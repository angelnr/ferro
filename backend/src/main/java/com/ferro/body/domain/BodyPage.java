package com.ferro.body.domain;

import java.util.List;

public record BodyPage(List<BodyMeasurement> content, int page, int size, long totalElements, int totalPages) {}
