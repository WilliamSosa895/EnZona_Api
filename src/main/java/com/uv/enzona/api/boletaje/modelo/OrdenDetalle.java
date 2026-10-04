package com.uv.enzona.api.boletaje.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Lo que pidió la orden: tipo, cantidad, precio confirmado y asientos (compra múltiple). */
@Entity
@Table(name = "orden_detalle")
public class OrdenDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private Long ordenId;

    @Column(name = "tipo_boleto_id", nullable = false, updatable = false)
    private Long tipoBoletoId;

    @Column(nullable = false, updatable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precioUnitario;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "asientos_ids", nullable = false, updatable = false, columnDefinition = "bigint[]")
    private Long[] asientosIds;

    protected OrdenDetalle() {
    }

    public static OrdenDetalle nuevo(Long ordenId, Long tipoBoletoId, int cantidad, BigDecimal precioUnitario,
            List<Long> asientos) {
        OrdenDetalle detalle = new OrdenDetalle();
        detalle.ordenId = ordenId;
        detalle.tipoBoletoId = tipoBoletoId;
        detalle.cantidad = cantidad;
        detalle.precioUnitario = precioUnitario;
        detalle.asientosIds = asientos.toArray(Long[]::new);
        return detalle;
    }

    public List<Long> asientos() {
        return asientosIds == null ? List.of() : Arrays.asList(asientosIds);
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

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
}
