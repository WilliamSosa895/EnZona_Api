package com.uv.enzona.api.administracion;

import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import com.uv.enzona.api.comun.web.Paginacion;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import com.uv.enzona.api.identidad.modelo.EstadoUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Gestión de usuarios y supervisión de eventos (RF-18)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@PreAuthorize("hasRole('ADMIN')")
public class AdministracionController {

    private final AdministracionService administracion;
    private final UsuarioActual usuarioActual;

    public AdministracionController(AdministracionService administracion, UsuarioActual usuarioActual) {
        this.administracion = administracion;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Lista cuentas filtrando por estado o texto (nombre o correo)")
    public ResponseEntity<List<UsuarioAdminRespuesta>> usuarios(@RequestParam(required = false) EstadoUsuario estado,
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Paginacion.respuesta(administracion.usuarios(estado, q, Paginacion.pagina(page), Paginacion.tamano(size)));
    }

    @PatchMapping("/usuarios/{id}/estado")
    @Operation(summary = "Bloquea o reactiva una cuenta; el bloqueo aplica de inmediato (CP-46)")
    public UsuarioAdminRespuesta cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoSolicitud solicitud) {
        return administracion.cambiarEstado(id, solicitud.estado(), usuarioActual.id());
    }

    @GetMapping("/eventos")
    @Operation(summary = "Supervisión de eventos en cualquier estado")
    public ResponseEntity<List<EventoRespuesta>> eventos(@RequestParam(required = false) EstadoEvento estado,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return Paginacion.respuesta(administracion.eventos(estado, Paginacion.pagina(page), Paginacion.tamano(size)));
    }
}
