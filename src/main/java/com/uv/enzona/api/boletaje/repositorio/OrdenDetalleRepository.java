package com.uv.enzona.api.boletaje.repositorio;

import com.uv.enzona.api.boletaje.modelo.OrdenDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenDetalleRepository extends JpaRepository<OrdenDetalle, Long> {

    List<OrdenDetalle> findByOrdenId(Long ordenId);
}
