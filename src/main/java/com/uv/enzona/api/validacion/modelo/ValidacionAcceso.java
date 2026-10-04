package com.uv.enzona.api.validacion.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/** Bitácora de cada intento de acceso, en línea o sin conexión (RF-15, RNF-08). */
@Entity
@Table(name = "validacion_acceso")
public class ValidacionAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boleto_id", nullable = false, updatable = false)
    private Long boletoId;

    @Column(name = "validador_id", nullable = false, updatable = false)
    private Long validadorId;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, updatable = false)
    private ResultadoAcceso resultado;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, updatable = false)
    private MotivoRechazo motivo;

    @Column(nullable = false, updatable = false)
    private boolean offline;

    @Column(nullable = false)
    private boolean sincronizado;

    protected ValidacionAcceso() {
    }

    public static ValidacionAcceso registrar(Long boletoId, Long validadorId, OffsetDateTime fechaHora,
            ResultadoAcceso resultado, MotivoRechazo motivo, boolean offline) {
        ValidacionAcceso validacion = new ValidacionAcceso();
        validacion.boletoId = boletoId;
        validacion.validadorId = validadorId;
        validacion.fechaHora = fechaHora;
        validacion.resultado = resultado;
        validacion.motivo = motivo;
        validacion.offline = offline;
        validacion.sincronizado = true;
        return validacion;
    }

    public Long getId() {
        return id;
    }

    public Long getBoletoId() {
        return boletoId;
    }

    public Long getValidadorId() {
        return validadorId;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public ResultadoAcceso getResultado() {
        return resultado;
    }

    public MotivoRechazo getMotivo() {
        return motivo;
    }

    public boolean isOffline() {
        return offline;
    }
}
