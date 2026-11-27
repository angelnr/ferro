package com.ferro.platform.interfaces.rest;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Config")
public class FeatureFlagController {

    private final JdbcTemplate jdbc;

    public FeatureFlagController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/flags")
    @Operation(summary = "Feature flags públicas (sin secretos)")
    public List<FeatureFlag> flags() {
        return jdbc.query(
                "SELECT key, enabled, description FROM feature_flags ORDER BY key",
                (rs, rowNum) ->
                        new FeatureFlag(rs.getString("key"), rs.getBoolean("enabled"), rs.getString("description")));
    }

    public record FeatureFlag(String key, boolean enabled, String description) {}
}
