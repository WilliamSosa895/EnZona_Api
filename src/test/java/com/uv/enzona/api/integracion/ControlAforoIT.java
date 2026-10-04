package com.uv.enzona.api.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uv.enzona.api.boletaje.modelo.MetodoPago;
import com.uv.enzona.api.boletaje.servicio.OrdenService;
import com.uv.enzona.api.boletaje.web.dto.CrearOrdenSolicitud;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.identidad.modelo.NombreRol;
import com.uv.enzona.api.identidad.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** CP-11 y CP-MULTI-05: sin sobreventa del último lugar con solicitudes simultáneas. */
class ControlAforoIT extends IntegracionBase {

    private static final int COMPETIDORES = 8;

    @Autowired
    private OrdenService ordenes;
    @Autowired
    private TipoBoletoRepository tipos;

    @Test
    void variasPersonasCompitenPorElUltimoLugarYSoloUnaLoObtiene() throws Exception {
        String correoOrganizador = correoUnico("org");
        crearUsuario(correoOrganizador, NombreRol.ORGANIZADOR);
        String respuesta = mvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token(correoOrganizador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson(false, 1, "null")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long eventoId = leer(respuesta).get("id").asLong();

        List<Usuario> asistentes = new ArrayList<>();
        for (int i = 0; i < COMPETIDORES; i++) {
            asistentes.add(crearUsuario(correoUnico("asis")));
        }

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(COMPETIDORES);
        List<Future<ErrorCodigo>> resultados = new ArrayList<>();
        for (Usuario asistente : asistentes) {
            Callable<ErrorCodigo> compra = () -> {
                salida.await();
                try {
                    ordenes.crear(asistente.getId(), UUID.randomUUID().toString(),
                            new CrearOrdenSolicitud(eventoId, null, 1, List.of(), null, MetodoPago.GRATIS, null));
                    return null;
                } catch (ApiException e) {
                    return e.getCodigo();
                }
            };
            resultados.add(hilos.submit(compra));
        }
        salida.countDown();

        int exitosas = 0;
        int agotadas = 0;
        for (Future<ErrorCodigo> resultado : resultados) {
            ErrorCodigo codigo = resultado.get();
            if (codigo == null) {
                exitosas++;
            } else if (codigo == ErrorCodigo.AFORO_AGOTADO) {
                agotadas++;
            }
        }
        hilos.shutdown();

        assertThat(exitosas).isEqualTo(1);
        assertThat(agotadas).isEqualTo(COMPETIDORES - 1);
        assertThat(tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId).get(0).getCantidadDisponible()).isZero();
    }
}
