package com.uv.enzona.api.eventos.servicio;

import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del catálogo (RF-05, RF-06, RF-07, RF-19) como especificaciones JPA combinables. */
final class EspecificacionesEvento {

    private EspecificacionesEvento() {
    }

    /** Publicados que no han terminado: fin futuro o, sin fin, iniciados hace menos de la duración por defecto. */
    static Specification<Evento> publicadosVigentes(OffsetDateTime ahora, int horasDuracion) {
        return (raiz, consulta, cb) -> cb.and(
                cb.equal(raiz.get("estado"), EstadoEvento.PUBLICADO),
                cb.or(
                        cb.greaterThanOrEqualTo(raiz.<OffsetDateTime>get("fechaHoraFin"), ahora),
                        cb.and(cb.isNull(raiz.get("fechaHoraFin")),
                                cb.greaterThanOrEqualTo(raiz.<OffsetDateTime>get("fechaHoraInicio"), ahora.minusHours(horasDuracion)))));
    }

    static Specification<Evento> delOrganizador(Long organizadorId) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("organizadorId"), organizadorId);
    }

    static Specification<Evento> texto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String patron = "%" + escaparLike(texto.trim().toLowerCase(Locale.ROOT)) + "%";
        return (raiz, consulta, cb) -> cb.or(
                cb.like(cb.lower(raiz.<String>get("nombre")), patron, '\\'),
                cb.like(cb.lower(raiz.<String>get("descripcion")), patron, '\\'),
                cb.like(cb.lower(raiz.<String>get("lugar")), patron, '\\'));
    }

    static Specification<Evento> categoria(Integer categoriaId) {
        return categoriaId == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("categoriaId"), categoriaId);
    }

    static Specification<Evento> ciudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return null;
        }
        String valor = ciudad.trim().toLowerCase(Locale.ROOT);
        return (raiz, consulta, cb) -> cb.equal(cb.lower(raiz.<String>get("ciudad")), valor);
    }

    static Specification<Evento> desde(OffsetDateTime desde) {
        return desde == null ? null : (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.<OffsetDateTime>get("fechaHoraInicio"), desde);
    }

    static Specification<Evento> hasta(OffsetDateTime hasta) {
        return hasta == null ? null : (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.<OffsetDateTime>get("fechaHoraInicio"), hasta);
    }

    static Specification<Evento> esDePago(Boolean esDePago) {
        return esDePago == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("esDePago"), esDePago);
    }

    static Specification<Evento> dentroDe(Distancia.Caja caja) {
        return (raiz, consulta, cb) -> cb.and(
                cb.between(raiz.<BigDecimal>get("latitud"), BigDecimal.valueOf(caja.latMin()), BigDecimal.valueOf(caja.latMax())),
                cb.between(raiz.<BigDecimal>get("longitud"), BigDecimal.valueOf(caja.lonMin()), BigDecimal.valueOf(caja.lonMax())));
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
