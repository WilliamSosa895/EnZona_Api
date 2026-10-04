package com.uv.enzona.api.eventos.repositorio;

import com.uv.enzona.api.eventos.modelo.Asiento;
import com.uv.enzona.api.eventos.modelo.EstadoAsiento;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    List<Asiento> findByEventoId(Long eventoId);

    long countByEventoId(Long eventoId);

    long countByEventoIdAndEstadoNot(Long eventoId, EstadoAsiento estado);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Asiento a where a.eventoId = :eventoId")
    int borrarDelEvento(@Param("eventoId") Long eventoId);

    /**
     * Cambia el estado de varios asientos solo si siguen en el estado esperado. El servicio
     * compara el número de filas afectadas con el de asientos pedidos (todo o nada).
     */
    @Modifying(flushAutomatically = true)
    @Query("update Asiento a set a.estado = :nuevo "
            + "where a.id in :ids and a.eventoId = :eventoId and a.estado = :actual")
    int cambiarEstado(@Param("ids") Collection<Long> ids, @Param("eventoId") Long eventoId,
            @Param("actual") EstadoAsiento actual, @Param("nuevo") EstadoAsiento nuevo);
}
