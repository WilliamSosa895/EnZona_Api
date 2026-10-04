package com.uv.enzona.api.eventos.web;

import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import com.uv.enzona.api.eventos.servicio.ConsultaEventosService;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizador")
@Tag(name = "Organizador", description = "Eventos propios del organizador")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
public class OrganizadorController {

    private final ConsultaEventosService consulta;
    private final UsuarioActual usuarioActual;

    public OrganizadorController(ConsultaEventosService consulta, UsuarioActual usuarioActual) {
        this.consulta = consulta;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/eventos")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    @Operation(summary = "Todos mis eventos, en cualquier estado (incluye borradores)")
    public List<EventoRespuesta> misEventos() {
        return consulta.delOrganizador(usuarioActual.id());
    }
}
