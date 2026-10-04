package com.uv.enzona.api.eventos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/** Butaca de un evento con asientos numerados. Único por (evento, sección, fila, número). */
@Entity
@Table(name = "asiento")
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private Long eventoId;

    @Column(length = 40)
    private String seccion;

    @Column(length = 10)
    private String fila;

    @Column(length = 10)
    private String numero;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private EstadoAsiento estado = EstadoAsiento.DISPONIBLE;

    protected Asiento() {
    }

    public static Asiento nuevo(Long eventoId, String seccion, String fila, String numero) {
        Asiento asiento = new Asiento();
        asiento.eventoId = eventoId;
        asiento.seccion = seccion;
        asiento.fila = fila;
        asiento.numero = numero;
        asiento.estado = EstadoAsiento.DISPONIBLE;
        return asiento;
    }

    /** "Platea · Fila A · Asiento 5". */
    public String etiqueta() {
        return seccion + " · Fila " + fila + " · Asiento " + numero;
    }

    public Long getId() {
        return id;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public String getSeccion() {
        return seccion;
    }

    public String getFila() {
        return fila;
    }

    public String getNumero() {
        return numero;
    }

    public EstadoAsiento getEstado() {
        return estado;
    }
}
