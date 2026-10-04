package com.uv.enzona.api.identidad.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Código de un solo uso enviado por correo o SMS. Solo se guarda su hash. */
@Entity
@Table(name = "codigo_verificacion")
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CanalVerificacion canal;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(nullable = false)
    private int intentos;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected CodigoVerificacion() {
    }

    public static CodigoVerificacion nuevo(Long usuarioId, CanalVerificacion canal, String codigoHash,
            OffsetDateTime expiraEn, OffsetDateTime ahora) {
        CodigoVerificacion codigo = new CodigoVerificacion();
        codigo.usuarioId = usuarioId;
        codigo.canal = canal;
        codigo.codigoHash = codigoHash;
        codigo.expiraEn = expiraEn;
        codigo.fechaCreacion = ahora;
        return codigo;
    }

    public boolean expirado(OffsetDateTime ahora) {
        return !expiraEn.isAfter(ahora);
    }

    public boolean intentosAgotados(int maximo) {
        return intentos >= maximo;
    }

    public void registrarIntento() {
        intentos++;
    }

    /** Marca el código como consumido; también se usa para invalidarlo al pedir uno nuevo. */
    public void marcarUsado() {
        usado = true;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public CanalVerificacion getCanal() {
        return canal;
    }

    public String getCodigoHash() {
        return codigoHash;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public int getIntentos() {
        return intentos;
    }
}
