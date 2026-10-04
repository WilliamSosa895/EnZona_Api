package com.uv.enzona.api.boletaje.repositorio;

import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.Orden;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByAsistenteIdAndClaveOperacion(Long asistenteId, String claveOperacion);

    /** Serializa la confirmación, el rechazo y la expiración de una misma orden. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.id = :id")
    Optional<Orden> bloquearPorId(@Param("id") Long id);

    @Query("select o from Orden o where o.estado = :estado and o.expiraEn < :ahora order by o.expiraEn")
    List<Orden> vencidas(@Param("estado") EstadoOrden estado, @Param("ahora") OffsetDateTime ahora, Pageable pagina);

    List<Orden> findByEventoId(Long eventoId);

    boolean existsByAsistenteIdAndEventoIdAndEstadoIn(Long asistenteId, Long eventoId, Collection<EstadoOrden> estados);
}
