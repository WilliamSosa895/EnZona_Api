package com.uv.enzona.api.validacion.web;

import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import com.uv.enzona.api.validacion.servicio.ValidacionService;
import com.uv.enzona.api.validacion.web.dto.ManifiestoRespuesta;
import com.uv.enzona.api.validacion.web.dto.SincronizacionRespuesta;
import com.uv.enzona.api.validacion.web.dto.SincronizacionSolicitud;
import com.uv.enzona.api.validacion.web.dto.ValidacionRespuesta;
import com.uv.enzona.api.validacion.web.dto.ValidacionSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Validación de acceso", description = "Escaneo del QR en la puerta y modo sin conexión (RF-15, RNF-08)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@PreAuthorize("hasAnyRole('VALIDADOR','ORGANIZADOR','ADMIN')")
public class ValidacionController {

    private final ValidacionService validacion;
    private final UsuarioActual usuarioActual;

    public ValidacionController(ValidacionService validacion, UsuarioActual usuarioActual) {
        this.validacion = validacion;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/validaciones")
    @Operation(summary = "Valida un QR y lo marca como usado de forma atómica",
            description = "Responde 200 tanto si permite como si rechaza el acceso; el motivo explica el rechazo.")
    public ValidacionRespuesta validar(@Valid @RequestBody ValidacionSolicitud solicitud) {
        return validacion.validar(usuarioActual.id(), usuarioActual.esAdmin(), solicitud.eventoId(), solicitud.qr());
    }

    @GetMapping("/eventos/{id}/manifiesto")
    @Operation(summary = "Descarga la lista de boletos del evento para validar sin conexión")
    public ManifiestoRespuesta manifiesto(@PathVariable Long id) {
        return validacion.manifiesto(usuarioActual.id(), usuarioActual.esAdmin(), id);
    }

    @PostMapping("/validaciones/sincronizar")
    @Operation(summary = "Sube el lote de escaneos hechos sin conexión y resuelve conflictos")
    public SincronizacionRespuesta sincronizar(@Valid @RequestBody SincronizacionSolicitud solicitud) {
        return validacion.sincronizar(usuarioActual.id(), usuarioActual.esAdmin(), solicitud);
    }
}
