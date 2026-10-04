package com.uv.enzona.api.boletaje.web;

import com.uv.enzona.api.boletaje.servicio.ConsultaBoletajeService;
import com.uv.enzona.api.boletaje.web.dto.BoletoRespuesta;
import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boletos")
@Tag(name = "Boletos", description = "Boletos del usuario autenticado con su contenido QR")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
public class BoletoController {

    private final ConsultaBoletajeService consulta;
    private final UsuarioActual usuarioActual;

    public BoletoController(ConsultaBoletajeService consulta, UsuarioActual usuarioActual) {
        this.consulta = consulta;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    @Operation(summary = "Mis boletos, del más reciente al más antiguo")
    public List<BoletoRespuesta> misBoletos() {
        return consulta.boletosDe(usuarioActual.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un boleto propio")
    public BoletoRespuesta obtener(@PathVariable Long id) {
        return consulta.boleto(id, usuarioActual.id());
    }
}
