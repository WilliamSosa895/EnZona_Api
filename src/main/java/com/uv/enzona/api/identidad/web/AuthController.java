package com.uv.enzona.api.identidad.web;

import com.uv.enzona.api.identidad.servicio.CodigosVerificacionService;
import com.uv.enzona.api.identidad.servicio.RecuperacionContrasenaService;
import com.uv.enzona.api.identidad.servicio.RegistroService;
import com.uv.enzona.api.identidad.servicio.SesionService;
import com.uv.enzona.api.identidad.web.dto.RecuperacionSolicitud;
import com.uv.enzona.api.identidad.web.dto.ReenvioSolicitud;
import com.uv.enzona.api.identidad.web.dto.RestablecimientoSolicitud;
import com.uv.enzona.api.identidad.web.dto.RegistroRespuesta;
import com.uv.enzona.api.identidad.web.dto.RegistroSolicitud;
import com.uv.enzona.api.identidad.web.dto.SesionRespuesta;
import com.uv.enzona.api.identidad.web.dto.SesionSolicitud;
import com.uv.enzona.api.identidad.web.dto.VerificacionRespuesta;
import com.uv.enzona.api.identidad.web.dto.VerificacionSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, verificación por código y sesión (RF-01, RF-02, RF-03)")
public class AuthController {

    private final RegistroService registro;
    private final CodigosVerificacionService verificacion;
    private final SesionService sesion;
    private final RecuperacionContrasenaService recuperacion;

    public AuthController(RegistroService registro, CodigosVerificacionService verificacion, SesionService sesion,
            RecuperacionContrasenaService recuperacion) {
        this.registro = registro;
        this.verificacion = verificacion;
        this.sesion = sesion;
        this.recuperacion = recuperacion;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea la cuenta en estado PENDIENTE y envía los códigos de verificación")
    public RegistroRespuesta registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        return registro.registrar(solicitud);
    }

    @PostMapping("/verificacion")
    @Operation(summary = "Confirma un código de un solo uso; la cuenta se activa al completar las verificaciones")
    public VerificacionRespuesta verificar(@Valid @RequestBody VerificacionSolicitud solicitud) {
        return verificacion.verificar(solicitud);
    }

    @PostMapping("/verificacion/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envía un código nuevo (responde 202 aunque el correo no exista)")
    public void reenviar(@Valid @RequestBody ReenvioSolicitud solicitud) {
        verificacion.reenviar(solicitud);
    }

    @PostMapping("/contrasena/recuperacion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envía al correo un código para restablecer la contraseña (202 aunque el correo no exista)")
    public void solicitarRecuperacion(@Valid @RequestBody RecuperacionSolicitud solicitud) {
        recuperacion.solicitar(solicitud);
    }

    @PostMapping("/contrasena/restablecimiento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia la contraseña con el código recibido por correo")
    public void restablecerContrasena(@Valid @RequestBody RestablecimientoSolicitud solicitud) {
        recuperacion.restablecer(solicitud);
    }

    @PostMapping("/sesion")
    @Operation(summary = "Inicia sesión con correo o CURP y devuelve el JWT")
    public SesionRespuesta iniciarSesion(@Valid @RequestBody SesionSolicitud solicitud) {
        return sesion.iniciar(solicitud);
    }
}
