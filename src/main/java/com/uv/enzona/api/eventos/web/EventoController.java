package com.uv.enzona.api.eventos.web;

import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import com.uv.enzona.api.comun.web.Paginacion;
import com.uv.enzona.api.eventos.imagenes.ImagenEventoService;
import com.uv.enzona.api.eventos.servicio.AsientosService;
import com.uv.enzona.api.eventos.servicio.ConsultaEventosService;
import com.uv.enzona.api.eventos.servicio.FiltroEventos;
import com.uv.enzona.api.eventos.servicio.GestionEventosService;
import com.uv.enzona.api.eventos.servicio.ValidadoresEventoService;
import com.uv.enzona.api.eventos.web.dto.AsientoRespuesta;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import com.uv.enzona.api.eventos.web.dto.EventoSolicitud;
import com.uv.enzona.api.eventos.web.dto.ValidadorRespuesta;
import com.uv.enzona.api.eventos.web.dto.ValidadorSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Catálogo, descubrimiento por ubicación y gestión de eventos")
public class EventoController {

    private final ConsultaEventosService consulta;
    private final GestionEventosService gestion;
    private final AsientosService asientos;
    private final ValidadoresEventoService validadores;
    private final ImagenEventoService imagenes;
    private final UsuarioActual usuarioActual;

    public EventoController(ConsultaEventosService consulta, GestionEventosService gestion, AsientosService asientos,
            ValidadoresEventoService validadores, ImagenEventoService imagenes, UsuarioActual usuarioActual) {
        this.consulta = consulta;
        this.gestion = gestion;
        this.asientos = asientos;
        this.validadores = validadores;
        this.imagenes = imagenes;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    @Operation(summary = "Lista eventos publicados vigentes (público)",
            description = "Con lat y lng ordena por cercanía (RF-19). Paginación en cabeceras X-Total-Count, "
                    + "X-Page, X-Page-Size y X-Total-Pages.")
    public ResponseEntity<List<EventoRespuesta>> listar(
            @Parameter(description = "Texto en nombre, descripción o lugar") @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoriaId,
            @Parameter(description = "Alternativa sin ubicación (RNF-11)") @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) Boolean esDePago,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
            @RequestParam(required = false) @DecimalMin("0.1") Double radioKm,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        FiltroEventos filtro = new FiltroEventos(q, categoriaId, ciudad, desde, hasta, esDePago, lat, lng, radioKm,
                Paginacion.pagina(page), Paginacion.tamano(size));
        return Paginacion.respuesta(consulta.listar(filtro));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del evento con sus tipos de boleto y disponibilidad (público)")
    public EventoRespuesta detalle(@PathVariable Long id) {
        return consulta.detalle(id, usuarioActual.idOpcional(), usuarioActual.esAdmin());
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Crea un evento en borrador o publicado (RF-08)")
    public ResponseEntity<EventoRespuesta> crear(@Valid @RequestBody EventoSolicitud solicitud) {
        EventoRespuesta creado = gestion.crear(usuarioActual.id(), solicitud);
        return ResponseEntity.created(URI.create("/api/eventos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Modifica el evento aplicando la política de modificación (RF-13)")
    public EventoRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody EventoSolicitud solicitud) {
        return gestion.actualizar(id, usuarioActual.id(), solicitud);
    }

    @PostMapping("/{id}/publicacion")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Publica un evento en borrador (exige coordenadas y fecha futura)")
    public EventoRespuesta publicar(@PathVariable Long id) {
        return gestion.publicar(id, usuarioActual.id());
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Sube o reemplaza la portada del evento (JPEG, PNG o WebP; campo multipart \"imagen\")",
            description = "La API guarda la imagen en el almacén configurado (local o Cloudinary) y devuelve el evento "
                    + "con su imagenUrl. El cliente nunca recibe credenciales del almacén.")
    public EventoRespuesta subirImagen(@PathVariable Long id, @RequestPart("imagen") MultipartFile imagen) {
        return imagenes.asignar(id, usuarioActual.id(), usuarioActual.esAdmin(), imagen);
    }

    @DeleteMapping("/{id}/imagen")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Quita la portada del evento")
    public EventoRespuesta quitarImagen(@PathVariable Long id) {
        return imagenes.quitar(id, usuarioActual.id(), usuarioActual.esAdmin());
    }

    @GetMapping("/{id}/asientos")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Mapa de asientos con su estado (RF-09)")
    public List<AsientoRespuesta> asientos(@PathVariable Long id) {
        return asientos.listar(id, usuarioActual.idOpcional(), usuarioActual.esAdmin());
    }

    @GetMapping("/{id}/validadores")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Personal de acceso designado para el evento")
    public List<ValidadorRespuesta> validadores(@PathVariable Long id) {
        return validadores.listar(id, usuarioActual.id(), usuarioActual.esAdmin());
    }

    @PostMapping("/{id}/validadores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Designa como validador a una cuenta activa (le agrega el rol VALIDADOR)")
    public ValidadorRespuesta designarValidador(@PathVariable Long id, @Valid @RequestBody ValidadorSolicitud solicitud) {
        return validadores.designar(id, usuarioActual.id(), usuarioActual.esAdmin(), solicitud.correo());
    }

    @DeleteMapping("/{id}/validadores/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
    @Operation(summary = "Retira a un validador del evento")
    public void quitarValidador(@PathVariable Long id, @PathVariable Long usuarioId) {
        validadores.quitar(id, usuarioActual.id(), usuarioActual.esAdmin(), usuarioId);
    }
}
