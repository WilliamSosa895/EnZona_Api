package com.uv.enzona.api.eventos.repositorio;

import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    /** Bloquea la fila del evento para serializar modificaciones y cancelaciones. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> bloquearPorId(@Param("id") Long id);

    List<Evento> findByOrganizadorIdOrderByFechaHoraInicioDesc(Long organizadorId);

    /** Eventos publicados cuyo inicio ya pasó; el servicio decide cuáles ya terminaron. */
    @Query("select e from Evento e where e.estado = :estado and e.fechaHoraInicio < :ahora order by e.fechaHoraInicio")
    List<Evento> iniciadosEnEstado(@Param("estado") EstadoEvento estado, @Param("ahora") OffsetDateTime ahora,
            Pageable pagina);
}
