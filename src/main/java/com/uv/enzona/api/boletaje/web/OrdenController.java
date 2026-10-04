package com.uv.enzona.api.boletaje.web;

import com.uv.enzona.api.boletaje.servicio.ConsultaBoletajeService;
import com.uv.enzona.api.boletaje.servicio.OrdenService;
import com.uv.enzona.api.boletaje.servicio.ResultadoOrden;
import com.uv.enzona.api.boletaje.web.dto.CrearOrdenSolicitud;
import com.uv.enzona.api.boletaje.web.dto.OrdenRespuesta;
import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
@Tag(name = "Órdenes", description = "Compra de boletos y confirmación de asistencia (RF-10, RF-11, RF-12)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
public class OrdenController {

    private final OrdenService ordenes;
    private final ConsultaBoletajeService consulta;
    private final UsuarioActual usuarioActual;

    public OrdenController(OrdenService ordenes, ConsultaBoletajeService consulta, UsuarioActual usuarioActual) {
        this.ordenes = ordenes;
        this.consulta = consulta;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @Operation(summary = "Crea la orden de N boletos (todo o nada) y cobra o confirma",
            description = "201 si la orden es nueva; 200 si la Idempotency-Key ya se había usado (misma orden, "
                    + "sin volver a cobrar). Un pago rechazado responde 201 con estadoOrden CANCELADA.")
    public ResponseEntity<OrdenRespuesta> crear(
            @Parameter(description = "Clave única por intento de compra, 8 a 64 caracteres")
            @RequestHeader(name = "Idempotency-Key", required = false) String claveOperacion,
            @Valid @RequestBody CrearOrdenSolicitud solicitud) {
        ResultadoOrden resultado = ordenes.crear(usuarioActual.id(), claveOperacion, solicitud);
        if (!resultado.nueva()) {
            return ResponseEntity.ok(resultado.orden());
        }
        return ResponseEntity.created(URI.create("/api/ordenes/" + resultado.orden().ordenId())).body(resultado.orden());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una orden propia con sus boletos")
    public OrdenRespuesta obtener(@PathVariable Long id) {
        return consulta.orden(id, usuarioActual.id(), usuarioActual.esAdmin());
    }
}
