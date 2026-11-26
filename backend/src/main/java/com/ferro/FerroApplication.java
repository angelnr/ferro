package com.ferro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de Ferro.
 *
 * <p>Monolito modular con arquitectura hexagonal por bounded context. Las reglas de
 * acoplamiento están descritas en {@code docs/04-arquitectura.md} §4 y se verifican con
 * ArchUnit y Spring Modulith.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FerroApplication {

    public static void main(String[] args) {
        SpringApplication.run(FerroApplication.class, args);
    }
}
