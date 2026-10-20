package com.uv.enzona.api.eventos.imagenes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.eventos.modelo.DatosEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.eventos.servicio.AccesoEventos;
import com.uv.enzona.api.eventos.servicio.EventoMapper;

/** Moderación de portadas: regla de decisión y garantía de que una imagen rechazada nunca se guarda. */
class ModeracionImagenesTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void laModeracionDeCloudinarySoloArrancaConElAlmacenDeCloudinary() {
        assertThat(new ModeradorCloudinary("cloudinary").moderar(PNG, "image/png").permitida()).isTrue();
        assertThatThrownBy(() -> new ModeradorCloudinary("local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IMAGE_PROVIDER=cloudinary");
    }

    @Test
    void sightengineRechazaDesnudezGoreViolenciaYAutolesion() throws Exception {
        double umbral = 0.7;
        assertThat(ModeradorSightengine.evaluar(json.readTree(
                "{\"nudity\":{\"sexual_activity\":0.6,\"sexual_display\":0.3,\"erotica\":0.02,\"none\":0.05}}"), umbral).permitida()).isFalse();
        assertThat(ModeradorSightengine.evaluar(json.readTree("{\"gore\":{\"prob\":0.91}}"), umbral).motivo()).contains("violencia gráfica");
        assertThat(ModeradorSightengine.evaluar(json.readTree("{\"violence\":{\"prob\":0.75}}"), umbral).permitida()).isFalse();
        assertThat(ModeradorSightengine.evaluar(json.readTree("{\"self-harm\":{\"prob\":0.8}}"), umbral).permitida()).isFalse();
        // Un cartel normal: todo por debajo del umbral
        assertThat(ModeradorSightengine.evaluar(json.readTree(
                "{\"nudity\":{\"sexual_activity\":0.01,\"sexual_display\":0.01,\"erotica\":0.02,\"suggestive\":0.1,\"none\":0.86},"
                        + "\"gore\":{\"prob\":0.01},\"violence\":{\"prob\":0.02},\"self-harm\":{\"prob\":0.0}}"), umbral).permitida()).isTrue();
    }

    @Test
    void cloudinarySoloPublicaLoAprobadoPorSuAddOn() throws Exception {
        assertThat(AlmacenCloudinary.rechazadaPorModeracion(json.readTree("{\"moderation\":[{\"kind\":\"aws_rek\",\"status\":\"approved\"}]}"))).isFalse();
        assertThat(AlmacenCloudinary.rechazadaPorModeracion(json.readTree("{\"moderation\":[{\"kind\":\"aws_rek\",\"status\":\"rejected\"}]}"))).isTrue();
        assertThat(AlmacenCloudinary.rechazadaPorModeracion(json.readTree("{\"moderation\":[{\"kind\":\"aws_rek\",\"status\":\"pending\"}]}"))).isTrue();
        assertThat(AlmacenCloudinary.rechazadaPorModeracion(json.readTree("{\"public_id\":\"x\"}"))).isFalse();
    }

    @Test
    void unaImagenRechazadaNoSeGuardaNiSeAsignaAlEvento() {
        AlmacenImagenes almacen = mock(AlmacenImagenes.class);
        ModeradorImagenes moderador = (datos, tipo) -> ModeradorImagenes.Veredicto.rechazo("La imagen contiene desnudez o contenido sexual");
        AccesoEventos acceso = mock(AccesoEventos.class);
        Evento evento = Evento.crear(3L, new DatosEvento("Noche", null, "Foro", null, "Orizaba", null, null, null,
                OffsetDateTime.now().plusDays(5), null, 50, true, false), OffsetDateTime.now());
        when(acceso.bloquearGestionable(anyLong(), anyLong(), anyBoolean(), anyBoolean())).thenReturn(evento);
        EnzonaProperties propiedades = mock(EnzonaProperties.class);
        when(propiedades.imagenes()).thenReturn(new EnzonaProperties.Imagenes("local", null, "./imagenes", 5, "sightengine", "u", "s", 0.7));
        ImagenEventoService servicio = new ImagenEventoService(almacen, moderador, acceso, mock(TipoBoletoRepository.class),
                mock(EventoMapper.class), propiedades);

        assertThatThrownBy(() -> servicio.asignar(7L, 3L, false, new MockMultipartFile("imagen", "p.png", "image/png", PNG)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCodigo()).isEqualTo(ErrorCodigo.IMAGEN_NO_PERMITIDA))
                .hasMessageContaining("desnudez");
        verify(almacen, never()).guardar(any(), anyString(), anyString());
        assertThat(evento.tieneImagen()).isFalse();
    }
}
