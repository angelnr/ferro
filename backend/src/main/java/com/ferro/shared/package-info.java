/**
 * Kernel compartido mínimo: tipos base de dominio, errores comunes y utilidades transversales.
 *
 * <p>Deliberadamente pequeño. Si algo necesita compartirse masivamente, es señal de un contexto mal cortado.
 *
 * <p>Es un módulo abierto para Spring Modulith: expone todos sus tipos a los módulos de negocio.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.ferro.shared;
