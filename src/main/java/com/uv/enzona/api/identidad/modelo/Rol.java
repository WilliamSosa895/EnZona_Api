package com.uv.enzona.api.identidad.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/** Catálogo de roles (tabla {@code rol}). Sus filas las crea la migración V1. */
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    protected Rol() {
    }

    public Short getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean es(NombreRol rol) {
        return rol.name().equals(nombre);
    }

    @Override
    public boolean equals(Object otro) {
        return this == otro || (otro instanceof Rol rol && Objects.equals(nombre, rol.nombre));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nombre);
    }
}
