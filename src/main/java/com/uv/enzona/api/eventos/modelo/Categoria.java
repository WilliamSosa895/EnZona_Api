package com.uv.enzona.api.eventos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de categorías (tabla {@code categoria}). */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    protected Categoria() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
