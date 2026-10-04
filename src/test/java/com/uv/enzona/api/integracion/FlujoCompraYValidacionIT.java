package com.uv.enzona.api.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Flujo de punta a punta: compra de pago, idempotencia, rechazo, validación en puerta y seguridad. */
class FlujoCompraYValidacionIT extends IntegracionBase {

    private String tokenOrganizador;
    private String tokenAsistente;
    private long eventoId;
    private long tipoId;

    @BeforeEach
    void prepararEvento() throws Exception {
        String correoOrganizador = correoUnico("org");
        String correoAsistente = correoUnico("asis");
        crearUsuario(correoOrganizador, NombreRol.ORGANIZADOR);
        crearUsuario(correoAsistente);
        tokenOrganizador = token(correoOrganizador);
        tokenAsistente = token(correoAsistente);

        JsonNode evento = leer(mvc.perform(post("/api/eventos").header("Authorization", "Bearer " + tokenOrganizador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson(true, 3, "[{\"nombre\":\"General\",\"precio\":150.00,\"cantidad\":3}]")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        eventoId = evento.get("id").asLong();
        tipoId = evento.get("tiposBoleto").get(0).get("id").asLong();
    }

    private String orden(int cantidad, String token) {
        return """
                {"eventoId":%d,"tipoBoletoId":%d,"cantidad":%d,"asientosIds":[],"metodo":"TARJETA","tokenPago":"%s"}
                """.formatted(eventoId, tipoId, cantidad, token);
    }

    @Test
    void compraDosBoletosEsIdempotenteYDescuentaElInventario() throws Exception {
        String primera = mvc.perform(post("/api/ordenes").header("Authorization", "Bearer " + tokenAsistente)
                        .header("Idempotency-Key", "compra-prueba-0001").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(2, "tok_aprobado")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("PAGADA"))
                .andExpect(jsonPath("$.total").value("300.00"))
                .andExpect(jsonPath("$.boletos.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        // Mismo intento (doble toque): misma orden, sin cobrar ni emitir de nuevo (CP-MULTI-07).
        String repetida = mvc.perform(post("/api/ordenes").header("Authorization", "Bearer " + tokenAsistente)
                        .header("Idempotency-Key", "compra-prueba-0001").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(2, "tok_aprobado")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(leer(repetida).get("ordenId")).isEqualTo(leer(primera).get("ordenId"));

        // Solo queda un lugar: pedir dos se rechaza completo (CP-MULTI-05).
        mvc.perform(post("/api/ordenes").header("Authorization", "Bearer " + tokenAsistente)
                        .header("Idempotency-Key", "compra-prueba-0002").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(2, "tok_aprobado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AFORO_AGOTADO"));

        mvc.perform(get("/api/eventos/" + eventoId)).andExpect(jsonPath("$.disponibles").value(1));
    }

    @Test
    void unPagoRechazadoNoEmiteBoletosNiConsumeLugares() throws Exception {
        mvc.perform(post("/api/ordenes").header("Authorization", "Bearer " + tokenAsistente)
                        .header("Idempotency-Key", "compra-rechazada-01").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(1, "tok_rechazado")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("CANCELADA"))
                .andExpect(jsonPath("$.estadoPago").value("RECHAZADO"))
                .andExpect(jsonPath("$.boletos.length()").value(0));

        mvc.perform(get("/api/eventos/" + eventoId)).andExpect(jsonPath("$.disponibles").value(3));
    }

    @Test
    void elBoletoSeAdmiteUnaSolaVezYUnaFirmaAlteradaSeRechaza() throws Exception {
        JsonNode orden = leer(mvc.perform(post("/api/ordenes").header("Authorization", "Bearer " + tokenAsistente)
                        .header("Idempotency-Key", "compra-validacion-1").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(1, "tok_aprobado")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String qr = orden.get("boletos").get(0).get("contenidoQr").asText();
        String validacion = "{\"eventoId\":%d,\"qr\":\"%s\"}";

        mvc.perform(post("/api/validaciones").header("Authorization", "Bearer " + tokenOrganizador)
                        .contentType(MediaType.APPLICATION_JSON).content(validacion.formatted(eventoId, qr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("PERMITIDO"));

        mvc.perform(post("/api/validaciones").header("Authorization", "Bearer " + tokenOrganizador)
                        .contentType(MediaType.APPLICATION_JSON).content(validacion.formatted(eventoId, qr)))
                .andExpect(jsonPath("$.resultado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivo").value("YA_UTILIZADO"));

        String alterado = qr.substring(0, qr.length() - 1) + (qr.endsWith("A") ? "B" : "A");
        mvc.perform(post("/api/validaciones").header("Authorization", "Bearer " + tokenOrganizador)
                        .contentType(MediaType.APPLICATION_JSON).content(validacion.formatted(eventoId, alterado)))
                .andExpect(jsonPath("$.motivo").value("FIRMA_INVALIDA"));
    }

    @Test
    void lasRutasProtegidasExigenTokenYRol() throws Exception {
        mvc.perform(get("/api/boletos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/eventos/" + eventoId + "/reportes").header("Authorization", "Bearer " + tokenAsistente))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/eventos")).andExpect(status().isOk());
    }
}
