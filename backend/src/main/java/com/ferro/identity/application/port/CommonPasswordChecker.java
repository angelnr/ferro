package com.ferro.identity.application.port;

/** Comprueba si una contraseña está en la lista de las 10 000 más comunes (RN-14). */
public interface CommonPasswordChecker {

    boolean isCommon(String rawPassword);
}
