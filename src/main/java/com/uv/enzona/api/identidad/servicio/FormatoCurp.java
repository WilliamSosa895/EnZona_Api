package com.uv.enzona.api.identidad.servicio;

/**
 * Estructura oficial del CURP: 4 letras, fecha AAMMDD, sexo (H/M), entidad federativa,
 * 3 consonantes internas, homoclave y dígito verificador.
 */
public final class FormatoCurp {

    public static final String REGEX = "^[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HMX]"
            + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
            + "[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$";

    public static final String MENSAJE = "El CURP no tiene un formato válido";

    private FormatoCurp() {
    }

    public static boolean esValido(String curp) {
        return curp != null && curp.trim().toUpperCase(java.util.Locale.ROOT).matches(REGEX);
    }
}
