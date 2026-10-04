package com.uv.enzona.api.eventos.modelo;

import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/**
 * Evento publicable (tabla {@code evento}). Las referencias a otros agregados (organizador,
 * categoría) se guardan por identificador para mantener los módulos desacoplados.
 */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizador_id", nullable = false, updatable = false)
    private Long organizadorId;

    @Column(name = "categoria_id")
    private Integer categoriaId;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false, length = 160)
    private String lugar;

    @Column(length = 240)
    private String direccion;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private OffsetDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private OffsetDateTime fechaHoraFin;

    @Column(nullable = false)
    private int aforo;

    @Column(name = "es_de_pago", nullable = false)
    private boolean esDePago;

    @Column(name = "requiere_asiento", nullable = false)
    private boolean requiereAsiento;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private EstadoEvento estado = EstadoEvento.BORRADOR;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private OffsetDateTime fechaCancelacion;

    @Column(name = "cancelado_por")
    private Long canceladoPor;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    protected Evento() {
    }

    public static Evento crear(Long organizadorId, DatosEvento datos, OffsetDateTime ahora) {
        Evento evento = new Evento();
        evento.organizadorId = organizadorId;
        evento.esDePago = datos.esDePago();
        evento.requiereAsiento = datos.requiereAsiento();
        evento.estado = EstadoEvento.BORRADOR;
        evento.fechaCreacion = ahora;
        evento.aplicar(datos);
        return evento;
    }

    /** Copia los datos editables. La modalidad (de pago, con asiento) se cambia con {@link #cambiarModalidad}. */
    public void aplicar(DatosEvento datos) {
        this.nombre = datos.nombre();
        this.descripcion = datos.descripcion();
        this.lugar = datos.lugar();
        this.direccion = datos.direccion();
        this.ciudad = datos.ciudad();
        this.latitud = datos.latitud();
        this.longitud = datos.longitud();
        this.categoriaId = datos.categoriaId();
        this.fechaHoraInicio = datos.fechaHoraInicio();
        this.fechaHoraFin = datos.fechaHoraFin();
        this.aforo = datos.aforo();
    }

    public void cambiarModalidad(boolean esDePago, boolean requiereAsiento) {
        this.esDePago = esDePago;
        this.requiereAsiento = requiereAsiento;
    }

    // ------------------------------------------------------------------ ciclo de vida

    /** Publicar exige ubicación exacta (coordenadas) y una fecha de inicio futura. */
    public void publicar(OffsetDateTime ahora) {
        if (estado != EstadoEvento.BORRADOR) {
            throw new ApiException(ErrorCodigo.ESTADO_INVALIDO, "Solo se publica un evento en borrador");
        }
        if (!tieneCoordenadas()) {
            throw ApiException.reglaNegocio("Para publicar, fija la ubicación del evento (latitud y longitud)");
        }
        if (!fechaHoraInicio.isAfter(ahora)) {
            throw ApiException.reglaNegocio("La fecha de inicio debe ser futura");
        }
        estado = EstadoEvento.PUBLICADO;
    }

    public void cancelar(Long usuarioId, String motivo, OffsetDateTime ahora) {
        if (!esCancelable(ahora)) {
            throw new ApiException(ErrorCodigo.EVENTO_NO_CANCELABLE,
                    "Solo se cancelan eventos en borrador o publicados que aún no inician");
        }
        estado = EstadoEvento.CANCELADO;
        fechaCancelacion = ahora;
        canceladoPor = usuarioId;
        motivoCancelacion = motivo;
    }

    public boolean esCancelable(OffsetDateTime ahora) {
        boolean estadoPermite = estado == EstadoEvento.BORRADOR || estado == EstadoEvento.PUBLICADO;
        return estadoPermite && fechaHoraInicio.isAfter(ahora);
    }

    public void marcarRealizado() {
        if (estado == EstadoEvento.PUBLICADO) {
            estado = EstadoEvento.REALIZADO;
        }
    }

    /** Un evento sin fecha de fin se considera terminado {@code horasPorDefecto} después de su inicio. */
    public OffsetDateTime finEfectivo(int horasPorDefecto) {
        return fechaHoraFin != null ? fechaHoraFin : fechaHoraInicio.plusHours(horasPorDefecto);
    }

    public boolean iniciado(OffsetDateTime ahora) {
        return !fechaHoraInicio.isAfter(ahora);
    }

    public boolean esModificable() {
        return estado == EstadoEvento.BORRADOR || estado == EstadoEvento.PUBLICADO;
    }

    public boolean visibleAlPublico() {
        return estado != EstadoEvento.BORRADOR;
    }

    public boolean perteneceA(Long usuarioId) {
        return Objects.equals(organizadorId, usuarioId);
    }

    public boolean tieneCoordenadas() {
        return latitud != null && longitud != null;
    }

    // ------------------------------------------------------------------ lectura

    public Long getId() {
        return id;
    }

    public Long getOrganizadorId() {
        return organizadorId;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLugar() {
        return lugar;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public OffsetDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public OffsetDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public int getAforo() {
        return aforo;
    }

    public boolean isEsDePago() {
        return esDePago;
    }

    public boolean isRequiereAsiento() {
        return requiereAsiento;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public Long getCanceladoPor() {
        return canceladoPor;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }
}
