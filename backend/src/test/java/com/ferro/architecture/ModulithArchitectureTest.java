package com.ferro.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.ferro.FerroApplication;

class ModulithArchitectureTest {

    @Test
    void module_boundaries_are_respected() {
        ApplicationModules.of(FerroApplication.class).verify();
    }
}
