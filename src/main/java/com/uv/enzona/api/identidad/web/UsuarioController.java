package com.uv.enzona.api.identidad.web;

import com.uv.enzona.api.comun.config.OpenApiConfig;
import com.uv.enzona.api.comun.seguridad.UsuarioActual;
import com.uv.enzona.api.identidad.servicio.PerfilService;
import com.uv.enzona.api.identidad.web.dto.CambioContrasena;
import com.uv.enzona.api.identidad.web.dto.PerfilActualizacion;
import com.uv.enzona.api.identidad.web.dto.PerfilRespuesta;
import com.uv.enzona.api.identidad.web.dto.UsuarioResumen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me")
@Tag(name = "Perfil", description = "Datos de la cuenta autenticada y rol de organizador (RF-04)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
public class UsuarioController {

    private final PerfilService perfil;
    private final UsuarioActual usuarioActual;

    public UsuarioController(PerfilService perfil, UsuarioActual usuarioActual) {
        this.perfil = perfil;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    @Operation(summary = "Consulta el perfil propio")
    public PerfilRespuesta obtener() {
        return perfil.obtener(usuarioActual.id());
    }

    @PutMapping
    @Operation(summary = "Actualiza nombre y teléfono; un teléfono nuevo debe verificarse de nuevo")
    public PerfilRespuesta actualizar(@Valid @RequestBody PerfilActualizacion cambios) {
        return perfil.actualizar(usuarioActual.id(), cambios);
    }

    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia la contraseña comprobando la actual")
    public void cambiarContrasena(@Valid @RequestBody CambioContrasena cambio) {
        perfil.cambiarContrasena(usuarioActual.id(), cambio);
    }

    @PostMapping("/roles/organizador")
    @Operation(summary = "Activa el rol de organizador en la cuenta")
    public UsuarioResumen activarOrganizador() {
        return perfil.activarOrganizador(usuarioActual.id());
    }

    @DeleteMapping("/roles/organizador")
    @Operation(summary = "Quita el rol de organizador (409 si tiene eventos vigentes)")
    public UsuarioResumen desactivarOrganizador() {
        return perfil.desactivarOrganizador(usuarioActual.id());
    }
}
