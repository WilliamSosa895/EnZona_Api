package com.uv.enzona.api.identidad.servicio;

/** Contraseña de 8 a 72 caracteres con al menos una letra y un dígito (72 es el límite de BCrypt). */
public final class PoliticaContrasena {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";

    public static final String MENSAJE = "La contraseña debe tener de 8 a 72 caracteres e incluir letras y números";

    private PoliticaContrasena() {
    }
}
