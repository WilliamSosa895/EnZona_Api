package com.uv.enzona.api.comun.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Conversión de fechas a la zona horaria del área de estudio (Orizaba, centro de México). */
public final class Fechas {

    public static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");

    private Fechas() {
    }

    /** Devuelve el mismo instante expresado con el desfase de la Ciudad de México, o {@code null}. */
    public static OffsetDateTime enZonaMx(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.atZoneSameInstant(ZONA_MX).toOffsetDateTime();
    }
}
