package com.uv.enzona.api.notificaciones;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.identidad.modelo.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Asistentes con boletos de un evento en ciertos estados (destinatarios de avisos). */
public interface DestinatariosRepository extends Repository<Usuario, Long> {

    @Query("select distinct u from Usuario u, Orden o, Boleto b "
            + "where o.asistenteId = u.id and b.ordenId = o.id and o.eventoId = :eventoId and b.estado in :estados")
    List<Usuario> conBoletos(@Param("eventoId") Long eventoId, @Param("estados") Collection<EstadoBoleto> estados);
}
