package com.uv.enzona.api.validacion.repositorio;

import com.uv.enzona.api.validacion.modelo.ResultadoAcceso;
import com.uv.enzona.api.validacion.modelo.ValidacionAcceso;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValidacionAccesoRepository extends JpaRepository<ValidacionAcceso, Long> {

    /** Evita registrar dos veces el mismo escaneo si el dispositivo reenvía el lote. */
    boolean existsByBoletoIdAndValidadorIdAndFechaHora(Long boletoId, Long validadorId, OffsetDateTime fechaHora);

    @Query("select count(v) from ValidacionAcceso v, Boleto b, Orden o "
            + "where b.id = v.boletoId and o.id = b.ordenId and o.eventoId = :eventoId and v.resultado = :resultado")
    long contarDelEvento(@Param("eventoId") Long eventoId, @Param("resultado") ResultadoAcceso resultado);
}
