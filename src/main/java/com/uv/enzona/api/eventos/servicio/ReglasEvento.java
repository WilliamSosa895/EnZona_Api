package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.eventos.web.dto.EventoSolicitud;
import com.uv.enzona.api.eventos.web.dto.SeccionSolicitud;
import com.uv.enzona.api.eventos.web.dto.TipoBoletoSolicitud;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas de consistencia de un evento que no dependen de la base de datos. Están aisladas en
 * una clase sin dependencias para probarlas con pruebas unitarias (CP-31).
 */
public final class ReglasEvento {

    private ReglasEvento() {
    }

    public static void validar(EventoSolicitud s, boolean esCreacion) {
        if (s.fechaHoraFin() != null && s.fechaHoraFin().isBefore(s.fechaHoraInicio())) {
            throw ApiException.reglaNegocio("La fecha de fin no puede ser anterior a la de inicio");
        }
        if ((s.latitud() == null) != (s.longitud() == null)) {
            throw ApiException.reglaNegocio("La latitud y la longitud se envían juntas o ninguna");
        }
        validarTipos(s);
        validarAsientos(s, esCreacion);
    }

    private static void validarTipos(EventoSolicitud s) {
        if (!s.esDePago()) {
            if (!s.tiposONada().isEmpty()) {
                throw ApiException.reglaNegocio(
                        "Los eventos gratuitos no llevan tipos de boleto: el cupo es el aforo");
            }
            return;
        }
        if (s.tiposONada().isEmpty()) {
            throw ApiException.reglaNegocio("Un evento de pago necesita al menos un tipo de boleto");
        }
        Set<String> nombres = new HashSet<>();
        int suma = 0;
        for (TipoBoletoSolicitud tipo : s.tiposONada()) {
            if (tipo.precio().compareTo(BigDecimal.ZERO) <= 0) {
                throw ApiException.reglaNegocio("En un evento de pago cada tipo de boleto debe tener precio mayor a 0");
            }
            if (!nombres.add(tipo.nombre().trim().toLowerCase(Locale.ROOT))) {
                throw ApiException.reglaNegocio("Hay dos tipos de boleto con el nombre \"" + tipo.nombre() + "\"");
            }
            suma += tipo.cantidad();
        }
        if (suma > s.aforo()) {
            throw ApiException.reglaNegocio("La suma de boletos (" + suma + ") supera el aforo (" + s.aforo() + ")");
        }
    }

    private static void validarAsientos(EventoSolicitud s, boolean esCreacion) {
        if (!s.requiereAsiento()) {
            if (!s.seccionesONada().isEmpty()) {
                throw ApiException.reglaNegocio("Solo los eventos con asiento numerado llevan secciones");
            }
            return;
        }
        if (s.seccionesONada().isEmpty()) {
            if (esCreacion) {
                throw ApiException.reglaNegocio("Define las secciones del mapa de asientos");
            }
            return;
        }
        Set<String> nombres = new HashSet<>();
        int total = 0;
        for (SeccionSolicitud seccion : s.seccionesONada()) {
            if (!nombres.add(seccion.nombre().trim().toLowerCase(Locale.ROOT))) {
                throw ApiException.reglaNegocio("Hay dos secciones con el nombre \"" + seccion.nombre() + "\"");
            }
            total += seccion.totalAsientos();
        }
        if (total != s.aforo()) {
            throw ApiException.reglaNegocio("El total de asientos (" + total + ") debe ser igual al aforo ("
                    + s.aforo() + ")");
        }
    }
}
