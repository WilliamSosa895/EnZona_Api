package com.uv.enzona.api.reportes;

import com.uv.enzona.api.boletaje.modelo.Boleto;
import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Consultas agregadas del panel del organizador (RF-17). Solo lectura. */
public interface ReporteRepository extends Repository<Boleto, Long> {

    @Query("select b.estado as estado, count(b) as cantidad from Boleto b, Orden o "
            + "where o.id = b.ordenId and o.eventoId = :eventoId group by b.estado")
    List<ConteoPorEstado> boletosPorEstado(@Param("eventoId") Long eventoId);

    @Query("select b.tipoBoletoId as tipoBoletoId, count(b) as cantidad from Boleto b, Orden o "
            + "where o.id = b.ordenId and o.eventoId = :eventoId and b.estado in :estados group by b.tipoBoletoId")
    List<ConteoPorTipo> boletosPorTipo(@Param("eventoId") Long eventoId,
            @Param("estados") Collection<EstadoBoleto> estados);

    @Query("select coalesce(sum(o.total), 0) from Orden o where o.eventoId = :eventoId and o.estado = :estado")
    BigDecimal importePorEstado(@Param("eventoId") Long eventoId, @Param("estado") EstadoOrden estado);

    interface ConteoPorEstado {
        EstadoBoleto getEstado();

        Long getCantidad();
    }

    interface ConteoPorTipo {
        Long getTipoBoletoId();

        Long getCantidad();
    }
}
