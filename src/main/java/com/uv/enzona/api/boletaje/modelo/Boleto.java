package com.uv.enzona.api.boletaje.modelo;

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

/**
 * Boleto electrónico firmado y de un solo uso (RF-12, RNF-02). El paso de VALIDO a USADO se
 * hace con una actualización condicional en la base de datos, no modificando esta entidad.
 */
@Entity
@Table(name = "boleto")
public class Boleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private Long ordenId;

    @Column(name = "tipo_boleto_id", nullable = false, updatable = false)
    private Long tipoBoletoId;

    @Column(name = "asiento_id", updatable = false)
    private Long asientoId;

    @Column(nullable = false, unique = true, length = 80, updatable = false)
    private String codigo;

    @Column(name = "qr_firma", nullable = false, length = 255, updatable = false)
    private String qrFirma;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private EstadoBoleto estado = EstadoBoleto.VALIDO;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "fecha_uso")
    private OffsetDateTime fechaUso;

    protected Boleto() {
    }

    public static Boleto emitir(Long ordenId, Long tipoBoletoId, Long asientoId, String codigo, String firma,
            OffsetDateTime ahora) {
        Boleto boleto = new Boleto();
        boleto.ordenId = ordenId;
        boleto.tipoBoletoId = tipoBoletoId;
        boleto.asientoId = asientoId;
        boleto.codigo = codigo;
        boleto.qrFirma = firma;
        boleto.estado = EstadoBoleto.VALIDO;
        boleto.fechaEmision = ahora;
        return boleto;
    }

    /** Contenido del código QR: "codigo.firma" (mismo formato que la app Android). */
    public String contenidoQr() {
        return codigo + "." + qrFirma;
    }

    public Long getId() {
        return id;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public Long getTipoBoletoId() {
        return tipoBoletoId;
    }

    public Long getAsientoId() {
        return asientoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getQrFirma() {
        return qrFirma;
    }

    public EstadoBoleto getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public OffsetDateTime getFechaUso() {
        return fechaUso;
    }
}
