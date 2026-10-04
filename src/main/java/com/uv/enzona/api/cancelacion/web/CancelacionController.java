package com.uv.enzona.api.cancelacion.web;

import com.uv.enzona.api.cancelacion.CancelacionService;
import com.uv.enzona.api.cancelacion.PoliticaCancelacion;
import com.uv.enzona.api.cancelacion.web.dto.CancelacionRespuesta;
import com.uv.enzona.api.cancelacion.web.dto.CancelacionSolicitud;
import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
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
@RequestMapping("/api/eventos/{id}")
@Tag(name = "Eventos")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
public class CancelacionController {

    private final CancelacionService cancelacion;
    private final UsuarioActual usuarioActual;

    public CancelacionController(CancelacionService cancelacion, UsuarioActual usuarioActual) {
        this.cancelacion = cancelacion;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/politica-cancelacion")
    @Operation(summary = "Calcula si el evento puede cancelarse y cuánto se reembolsaría")
    public PoliticaCancelacion politica(@PathVariable Long id) {
        return cancelacion.politica(id, usuarioActual.id(), usuarioActual.esAdmin());
    }

    @PostMapping("/cancelacion")
    @Operation(summary = "Cancela el evento, anula boletos, reembolsa y avisa a los asistentes (RF-14)")
    public CancelacionRespuesta cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) CancelacionSolicitud solicitud) {
        return cancelacion.cancelar(id, usuarioActual.id(), usuarioActual.esAdmin(), solicitud);
    }
}
