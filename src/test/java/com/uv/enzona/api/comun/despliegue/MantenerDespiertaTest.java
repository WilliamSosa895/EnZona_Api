package com.uv.enzona.api.comun.despliegue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uv.enzona.api.comun.config.DespliegueProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MantenerDespiertaTest {

    @SuppressWarnings("unchecked")
    private final HttpClient http = mock(HttpClient.class);

    @Test
    @SuppressWarnings("unchecked")
    void sePideASiMismaLaSaludPorLaDireccionPublica() throws Exception {
        HttpResponse<Void> respuesta = mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(200);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(respuesta);

        new MantenerDespierta(new DespliegueProperties("https://enzona-api.onrender.com/"), http).latir();

        ArgumentCaptor<HttpRequest> peticion = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(peticion.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(peticion.getValue().uri()).isEqualTo(URI.create("https://enzona-api.onrender.com/actuator/health"));
        assertThat(peticion.getValue().method()).isEqualTo("GET");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinDireccionPublicaNoHaceNada() throws Exception {
        new MantenerDespierta(new DespliegueProperties(""), http).latir();
        new MantenerDespierta(new DespliegueProperties(null), http).latir();

        verify(http, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unFalloDeRedSoloSeRegistraEnLaBitacora() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("sin red"));

        assertThatCode(() -> new MantenerDespierta(new DespliegueProperties("https://enzona-api.onrender.com"), http).latir())
                .doesNotThrowAnyException();
    }

    @Test
    void laDireccionDeSaludNoDuplicaDiagonales() {
        assertThat(MantenerDespierta.urlDeSalud("https://x.onrender.com//")).hasToString("https://x.onrender.com/actuator/health");
        assertThat(MantenerDespierta.urlDeSalud(" https://x.onrender.com ")).hasToString("https://x.onrender.com/actuator/health");
    }
}
