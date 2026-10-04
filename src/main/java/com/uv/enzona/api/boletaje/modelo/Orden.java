package com.uv.enzona.api.boletaje.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/** Intención de compra o confirmación de un asistente para un evento (tabla {@code orden}). */
@Entity
@Table(name = "orden")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asistente_id", nullable = false, updatable = false)
    private Long asistenteId;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private Long eventoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "clave_operacion", length = 64, updatable = false)
    private String claveOperacion;

    @Column(name = "expira_en")
    private OffsetDateTime expiraEn;

    protected Orden() {
    }

    public static Orden nueva(Long asistenteId, Long eventoId, BigDecimal total, String claveOperacion,
            OffsetDateTime expiraEn, OffsetDateTime ahora) {
        Orden orden = new Orden();
        orden.asistenteId = asistenteId;
        orden.eventoId = eventoId;
        orden.total = total;
        orden.claveOperacion = claveOperacion;
        orden.expiraEn = expiraEn;
        orden.fechaCreacion = ahora;
        orden.estado = EstadoOrden.PENDIENTE;
        return orden;
    }

    public boolean pendiente() {
        return estado == EstadoOrden.PENDIENTE;
    }

    public boolean gratuita() {
        return total.signum() == 0;
    }

    public void marcarPagada() {
        exigirPendiente();
        estado = EstadoOrden.PAGADA;
        expiraEn = null;
    }

    /** Pago rechazado o reserva vencida: la orden se cancela y libera sus lugares. */
    public void cancelar() {
        exigirPendiente();
        estado = EstadoOrden.CANCELADA;
        expiraEn = null;
    }

    /**
     * Efecto de cancelar el evento: las órdenes pendientes y las gratuitas se cancelan; las
     * pagadas con importe quedan PAGADA hasta que el reembolso se confirme (y pasan a REEMBOLSADA).
     */
    public void anularPorCancelacionDelEvento() {
        if (estado == EstadoOrden.PENDIENTE || (estado == EstadoOrden.PAGADA && gratuita())) {
            estado = EstadoOrden.CANCELADA;
            expiraEn = null;
        }
    }

    public void marcarReembolsada() {
        estado = EstadoOrden.REEMBOLSADA;
    }

    public boolean perteneceA(Long usuarioId) {
        return Objects.equals(asistenteId, usuarioId);
    }

    private void exigirPendiente() {
        if (estado != EstadoOrden.PENDIENTE) {
            throw new IllegalStateException("La orden " + id + " ya no está pendiente (" + estado + ")");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAsistenteId() {
        return asistenteId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getClaveOperacion() {
        return claveOperacion;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }
}
