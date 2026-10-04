package com.uv.enzona.api.boletaje.modelo;

import com.uv.enzona.api.comun.util.Dinero;
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
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/** Movimiento contra la pasarela. Solo guarda referencias, nunca datos de tarjeta (Anexo E, DA-07). */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private Long ordenId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(name = "referencia_pasarela", length = 120)
    private String referenciaPasarela;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @Column(name = "referencia_reembolso", length = 120)
    private String referenciaReembolso;

    @Column(name = "fecha_reembolso")
    private OffsetDateTime fechaReembolso;

    protected Pago() {
    }

    public static Pago registrar(Long ordenId, BigDecimal monto, EstadoPago estado, String referencia,
            OffsetDateTime ahora) {
        Pago pago = new Pago();
        pago.ordenId = ordenId;
        pago.monto = monto;
        pago.moneda = Dinero.MONEDA;
        pago.estado = estado;
        pago.referenciaPasarela = referencia;
        pago.fecha = ahora;
        return pago;
    }

    public void marcarReembolsado(String referencia, OffsetDateTime ahora) {
        this.estado = EstadoPago.REEMBOLSADO;
        this.referenciaReembolso = referencia;
        this.fechaReembolso = ahora;
    }

    public Long getId() {
        return id;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public String getReferenciaReembolso() {
        return referenciaReembolso;
    }
}
