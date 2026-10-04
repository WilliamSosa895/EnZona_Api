package com.uv.enzona.api.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.repositorio.RolRepository;
import com.uv.enzona.api.identidad.repositorio.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de las pruebas de integración: levanta PostgreSQL 15 en un contenedor, aplica las
 * migraciones de Flyway y arranca la API completa. Requiere Docker; sin Docker se omiten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class IntegracionBase {

    protected static final String CONTRASENA = "Secreta123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected UsuarioRepository usuarios;
    @Autowired
    protected RolRepository roles;
    @Autowired
    protected PasswordEncoder encoder;
    @Autowired
    protected TransactionTemplate transaccion;

    protected static String correoUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.mx";
    }

    protected Usuario crearUsuario(String correo, NombreRol... rolesExtra) {
        return transaccion.execute(estado -> {
            Usuario usuario = Usuario.nuevo("Persona de prueba", correo, null, null, null, encoder.encode(CONTRASENA),
                    OffsetDateTime.now());
            usuario.activarVerificada();
            usuario.agregarRol(roles.obtener(NombreRol.ASISTENTE));
            for (NombreRol rol : rolesExtra) {
                usuario.agregarRol(roles.obtener(rol));
            }
            return usuarios.save(usuario);
        });
    }

    protected String token(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CONTRASENA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("token").asText();
    }

    protected JsonNode leer(String cuerpo) throws Exception {
        return json.readTree(cuerpo);
    }

    /** Evento publicado en Orizaba dentro de diez días. */
    protected String eventoJson(boolean dePago, int aforo, String tiposJson) {
        return """
                {"nombre":"Evento de prueba","lugar":"Foro Orizaba","ciudad":"Orizaba",
                 "latitud":18.850600,"longitud":-97.100300,"categoriaId":1,
                 "fechaHoraInicio":"%s","aforo":%d,"esDePago":%b,"requiereAsiento":false,
                 "tiposBoleto":%s,"publicar":true}
                """.formatted(OffsetDateTime.now().plusDays(10).withNano(0), aforo, dePago, tiposJson);
    }
}
