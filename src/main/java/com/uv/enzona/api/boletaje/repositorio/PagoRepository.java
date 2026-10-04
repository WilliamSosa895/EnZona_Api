package com.uv.enzona.api.boletaje.repositorio;

import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.EstadoPago;
import com.uv.enzona.api.boletaje.modelo.Pago;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findFirstByOrdenIdOrderByFechaDesc(Long ordenId);

    Optional<Pago> findFirstByOrdenIdAndEstado(Long ordenId, EstadoPago estado);

    /**
     * Pagos aprobados que deben devolverse: los de órdenes pagadas de eventos cancelados y los
     * que la pasarela aprobó después de que la reserva había vencido (orden cancelada).
     */
    @Query("select p from Pago p, Orden o, Evento e "
            + "where o.id = p.ordenId and e.id = o.eventoId and p.estado = :aprobado "
            + "and ((o.estado = :pagada and e.estado = :cancelado) or o.estado = :cancelada) "
            + "order by p.fecha")
    List<Pago> pendientesDeReembolso(@Param("aprobado") EstadoPago aprobado, @Param("pagada") EstadoOrden pagada,
            @Param("cancelada") EstadoOrden cancelada, @Param("cancelado") EstadoEvento cancelado, Pageable pagina);
}
