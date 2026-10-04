package com.uv.enzona.api.boletaje.repositorio;

import com.uv.enzona.api.boletaje.modelo.Boleto;
import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    List<Boleto> findByOrdenIdOrderByIdAsc(Long ordenId);

    Optional<Boleto> findByCodigo(String codigo);

    @Query("select b from Boleto b, Orden o where o.id = b.ordenId and o.asistenteId = :asistenteId "
            + "order by b.fechaEmision desc, b.id desc")
    List<Boleto> delAsistente(@Param("asistenteId") Long asistenteId);

    @Query("select b from Boleto b, Orden o where o.id = b.ordenId and o.eventoId = :eventoId and b.estado in :estados "
            + "order by b.id")
    List<Boleto> delEventoEnEstados(@Param("eventoId") Long eventoId,
            @Param("estados") Collection<EstadoBoleto> estados);

    /** Evento al que pertenece el boleto (a través de su orden). */
    @Query("select o.eventoId from Boleto b, Orden o where o.id = b.ordenId and b.id = :boletoId")
    Optional<Long> eventoDelBoleto(@Param("boletoId") Long boletoId);

    @Query("select count(b) > 0 from Boleto b, Orden o where o.id = b.ordenId and o.asistenteId = :asistenteId "
            + "and o.eventoId = :eventoId and b.estado in :estados")
    boolean existeEnEstados(@Param("asistenteId") Long asistenteId, @Param("eventoId") Long eventoId,
            @Param("estados") Collection<EstadoBoleto> estados);

    /**
     * Marca el boleto como usado solo si sigue VALIDO. Devuelve 1 si esta validación fue la
     * primera y 0 si otro escaneo se adelantó: garantiza el uso único (RNF-02, CP-13).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Boleto b set b.estado = :usado, b.fechaUso = :fechaUso where b.id = :id and b.estado = :valido")
    int marcarUsado(@Param("id") Long id, @Param("fechaUso") OffsetDateTime fechaUso,
            @Param("valido") EstadoBoleto valido, @Param("usado") EstadoBoleto usado);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Boleto b set b.estado = :cancelado where b.estado = :valido "
            + "and b.ordenId in (select o.id from Orden o where o.eventoId = :eventoId)")
    int cancelarDelEvento(@Param("eventoId") Long eventoId, @Param("valido") EstadoBoleto valido,
            @Param("cancelado") EstadoBoleto cancelado);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Boleto b set b.estado = :expirado where b.estado = :valido and b.ordenId in "
            + "(select o.id from Orden o where o.eventoId in (select e.id from Evento e where e.estado = :realizado))")
    int expirarDeEventosRealizados(@Param("valido") EstadoBoleto valido, @Param("expirado") EstadoBoleto expirado,
            @Param("realizado") EstadoEvento realizado);
}
