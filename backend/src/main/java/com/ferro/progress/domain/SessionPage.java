package com.ferro.progress.domain;

import java.util.List;

public record SessionPage(List<SessionSummary> content, int page, int size, long totalElements, int totalPages) {}
