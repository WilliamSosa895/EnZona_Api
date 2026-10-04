package com.uv.enzona.api.integracion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import com.uv.enzona.api.notificaciones.Notificador;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

/** CP-01, CP-02, CP-04 y CP-05: registro, verificación por código y sesión; el CURP nunca se expone. */
class RegistroYVerificacionIT extends IntegracionBase {

    @MockBean
    private Notificador notificador;

    @Test
    void registraVerificaEIniciaSesion() throws Exception {
        String correo = correoUnico("nuevo");
        String registro = """
                {"nombre":"Ana Pérez","correo":"%s","curp":"PERA000229MDFRRSA1","contrasena":"Secreta123",
                 "aceptaAvisoPrivacidad":true}
                """.formatted(correo);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // CURP repetido (CP-02)
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro.replace(correo, correoUnico("otro"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CURP_REGISTRADO"));

        // Sin verificar no hay sesión
        mvc.perform(post("/api/auth/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"%s\",\"contrasena\":\"Secreta123\"}".formatted(correo)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_PENDIENTE"));

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(notificador, timeout(5000)).enviarCodigo(eq(correo), eq(CanalVerificacion.CORREO), any(),
                codigo.capture(), anyInt());

        // Código incorrecto (CP-04)
        String incorrecto = codigo.getValue().equals("000000") ? "111111" : "000000";
        mvc.perform(post("/api/auth/verificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, incorrecto)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/verificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, codigo.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // Inicio de sesión con CURP (CP-21) y perfil sin CURP
        String sesion = mvc.perform(post("/api/auth/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"PERA000229MDFRRSA1\",\"contrasena\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + leer(sesion).get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.curp").doesNotExist());
    }
}
