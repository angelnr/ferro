package com.ferro.identity.infrastructure.security;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ferro.identity.application.port.CommonPasswordChecker;

/** Carga en memoria el top-10k de SecLists; inmutable y compartido. */
@Component
public class FileCommonPasswordChecker implements CommonPasswordChecker {

    private final Set<String> commonPasswords;

    public FileCommonPasswordChecker() {
        Set<String> loaded = new HashSet<>();
        try (InputStream input = getClass().getResourceAsStream("/security/common-passwords.txt")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró /security/common-passwords.txt");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(loaded::add);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo cargar la lista de contraseñas comunes", exception);
        }
        this.commonPasswords = Collections.unmodifiableSet(loaded);
    }

    @Override
    public boolean isCommon(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        return commonPasswords.contains(rawPassword) || commonPasswords.contains(rawPassword.toLowerCase(Locale.ROOT));
    }
}
