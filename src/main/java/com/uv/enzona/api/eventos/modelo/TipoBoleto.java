package com.uv.enzona.api.eventos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Modalidad de entrada y su inventario. {@code cantidad_disponible} solo se modifica con
 * actualizaciones condicionales en la base de datos (Anexo E, DA-05), nunca leyendo y
 * escribiendo desde Java, para que no exista sobreventa con compras concurrentes.
 */
@Entity
@Table(name = "tipo_boleto")
public class TipoBoleto {

    public static final String NOMBRE_GRATUITO = "General";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private Long eventoId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "cantidad_total", nullable = false)
    private int cantidadTotal;

    @Column(name = "cantidad_disponible", nullable = false)
    private int cantidadDisponible;

    protected TipoBoleto() {
    }

    public static TipoBoleto nuevo(Long eventoId, String nombre, BigDecimal precio, int cantidad) {
        TipoBoleto tipo = new TipoBoleto();
        tipo.eventoId = eventoId;
        tipo.nombre = nombre;
        tipo.precio = precio;
        tipo.cantidadTotal = cantidad;
        tipo.cantidadDisponible = cantidad;
        return tipo;
    }

    /** Lugares vendidos o reservados en órdenes pendientes. */
    public int comprometidos() {
        return cantidadTotal - cantidadDisponible;
    }

    public boolean tieneVentas() {
        return comprometidos() > 0;
    }

    public Long getId() {
        return id;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getCantidadTotal() {
        return cantidadTotal;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }
}
