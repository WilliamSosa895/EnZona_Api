package com.uv.enzona.api.eventos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/** Personal de acceso designado por el organizador para un evento (tabla {@code evento_validador}). */
@Entity
@Table(name = "evento_validador")
public class EventoValidador {

    @EmbeddedId
    private Id id;

    protected EventoValidador() {
    }

    public EventoValidador(Long eventoId, Long usuarioId) {
        this.id = new Id(eventoId, usuarioId);
    }

    public Long getEventoId() {
        return id.getEventoId();
    }

    public Long getUsuarioId() {
        return id.getUsuarioId();
    }

    /** Llave compuesta (evento_id, usuario_id). */
    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "evento_id")
        private Long eventoId;

        @Column(name = "usuario_id")
        private Long usuarioId;

        protected Id() {
        }

        public Id(Long eventoId, Long usuarioId) {
            this.eventoId = eventoId;
            this.usuarioId = usuarioId;
        }

        public Long getEventoId() {
            return eventoId;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        @Override
        public boolean equals(Object otro) {
            return this == otro || (otro instanceof Id id
                    && Objects.equals(eventoId, id.eventoId) && Objects.equals(usuarioId, id.usuarioId));
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventoId, usuarioId);
        }
    }
}
