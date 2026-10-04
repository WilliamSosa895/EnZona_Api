package com.uv.enzona.api.identidad.repositorio;

import com.uv.enzona.api.identidad.modelo.Usuario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByCurpHash(String curpHash);

    boolean existsByCorreo(String correo);

    boolean existsByCurpHash(String curpHash);

    /** Bloquea la fila del usuario para serializar operaciones por persona (p. ej., RSVP). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearPorId(@Param("id") Long id);
}
