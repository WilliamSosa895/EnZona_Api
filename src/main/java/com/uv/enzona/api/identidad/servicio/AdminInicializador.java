package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.RolRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea la cuenta de administrador inicial a partir de ADMIN_EMAIL y ADMIN_PASSWORD, solo si
 * no existe. Así no hay credenciales en el repositorio ni en las migraciones.
 */
@Component
public class AdminInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public AdminInicializador(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder,
            EnzonaProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        EnzonaProperties.Admin admin = propiedades.admin();
        if (admin == null || !admin.configurado()) {
            return;
        }
        String correo = Correos.normalizar(admin.correo());
        if (usuarios.existsByCorreo(correo)) {
            return;
        }
        String nombre = admin.nombre() == null || admin.nombre().isBlank() ? "Administrador EnZona" : admin.nombre();
        Usuario usuario = Usuario.nuevo(nombre, correo, null, null, null, encoder.encode(admin.contrasena()),
                OffsetDateTime.now(reloj));
        usuario.activarVerificada();
        usuario.agregarRol(roles.obtener(NombreRol.ADMIN));
        usuario.agregarRol(roles.obtener(NombreRol.ASISTENTE));
        usuarios.save(usuario);
        log.info("Se creó la cuenta de administrador inicial");
    }
}
