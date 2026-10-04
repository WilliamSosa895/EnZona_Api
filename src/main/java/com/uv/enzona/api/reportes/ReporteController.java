package com.uv.enzona.api.reportes;

import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Organizador")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
public class ReporteController {

    private final ReporteService reportes;
    private final UsuarioActual usuarioActual;

    public ReporteController(ReporteService reportes, UsuarioActual usuarioActual) {
        this.reportes = reportes;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/api/eventos/{id}/reportes")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    @Operation(summary = "Panel del organizador: asistentes, ventas, aforo y accesos validados (RF-17)")
    public ReporteRespuesta reporte(@PathVariable Long id) {
        return reportes.reporte(id, usuarioActual.id(), usuarioActual.esAdmin());
    }
}
