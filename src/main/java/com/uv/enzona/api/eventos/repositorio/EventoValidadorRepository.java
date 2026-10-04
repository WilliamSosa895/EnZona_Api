package com.uv.enzona.api.eventos.repositorio;

import com.uv.enzona.api.eventos.modelo.EventoValidador;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoValidadorRepository extends JpaRepository<EventoValidador, EventoValidador.Id> {

    @Query("select v from EventoValidador v where v.id.eventoId = :eventoId")
    List<EventoValidador> delEvento(@Param("eventoId") Long eventoId);

    @Query("select count(v) > 0 from EventoValidador v where v.id.eventoId = :eventoId and v.id.usuarioId = :usuarioId")
    boolean estaDesignado(@Param("eventoId") Long eventoId, @Param("usuarioId") Long usuarioId);
}
