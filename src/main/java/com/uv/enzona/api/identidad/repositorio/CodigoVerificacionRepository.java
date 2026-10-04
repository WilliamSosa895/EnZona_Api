package com.uv.enzona.api.identidad.repositorio;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.identidad.modelo.CodigoVerificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    List<CodigoVerificacion> findByUsuarioIdAndCanalAndUsadoFalse(Long usuarioId, CanalVerificacion canal);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndCanalAndUsadoFalseOrderByFechaCreacionDesc(
            Long usuarioId, CanalVerificacion canal);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndCanalOrderByFechaCreacionDesc(
            Long usuarioId, CanalVerificacion canal);
}
