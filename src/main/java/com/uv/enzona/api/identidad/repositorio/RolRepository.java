package com.uv.enzona.api.identidad.repositorio;

import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Short> {

    Optional<Rol> findByNombre(String nombre);

    default Rol obtener(NombreRol nombreRol) {
        return findByNombre(nombreRol.name())
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombreRol + " en el catálogo"));
    }
}
