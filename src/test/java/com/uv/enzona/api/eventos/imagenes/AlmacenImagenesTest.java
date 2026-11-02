package com.uv.enzona.api.eventos.imagenes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Portadas de eventos: almacén local, firma de Cloudinary y comprobación del tipo de archivo. */
class AlmacenImagenesTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8'};

    @Test
    void elAlmacenLocalGuardaConNombrePropioYSirveUnaUrlRelativa(@TempDir Path carpeta) throws Exception {
        AlmacenLocal almacen = new AlmacenLocal(carpeta);

        AlmacenImagenes.ImagenGuardada guardada = almacen.guardar(PNG, "image/png", "eventos/7");

        assertThat(guardada.id()).matches("[0-9a-f]{32}\\.png");
        assertThat(guardada.url()).isEqualTo("/api/imagenes/" + guardada.id());
        assertThat(Files.readAllBytes(carpeta.resolve(guardada.id()))).isEqualTo(PNG);
        assertThat(almacen.resolver(guardada.id())).isEqualTo(carpeta.toAbsolutePath().normalize().resolve(guardada.id()));

        almacen.eliminar(guardada.id());
        assertThat(Files.exists(carpeta.resolve(guardada.id()))).isFalse();
        almacen.eliminar(guardada.id());   // borrar dos veces no falla
    }

    @Test
    void elAlmacenLocalNoDejaSalirDeSuCarpeta(@TempDir Path carpeta) {
        AlmacenLocal almacen = new AlmacenLocal(carpeta);
        assertThat(almacen.resolver("../secreto.txt")).isNull();
        assertThat(almacen.resolver("sub/archivo.png")).isNull();
        assertThat(almacen.resolver("")).isNull();
        assertThat(AlmacenLocal.tipoMimeDe("a.webp")).isEqualTo("image/webp");
        assertThat(AlmacenLocal.tipoMimeDe("a.jpg")).isEqualTo("image/jpeg");
    }

    @Test
    void laFirmaDeCloudinaryEsElSha1DeLosParametrosOrdenadosMasElSecreto() throws Exception {
        // Regla de Cloudinary: parámetros ordenados por nombre, unidos con &, seguidos del api_secret, en SHA-1 hexadecimal
        Map<String, String> parametros = new java.util.LinkedHashMap<>();
        parametros.put("timestamp", "1315060510");
        parametros.put("public_id", "sample_image");
        parametros.put("eager", "w_400,c_crop");
        byte[] esperado = java.security.MessageDigest.getInstance("SHA-1")
                .digest("eager=w_400,c_crop&public_id=sample_image&timestamp=1315060510abcd".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(AlmacenCloudinary.firmar(parametros, "abcd")).isEqualTo(java.util.HexFormat.of().formatHex(esperado));
        assertThat(AlmacenCloudinary.firmar(parametros, "abcd")).hasSize(40);
        assertThat(AlmacenCloudinary.firmar(parametros, "otro")).isNotEqualTo(AlmacenCloudinary.firmar(parametros, "abcd"));
    }

    @Test
    void lasCredencialesSeLeenDeLaUrlDeCloudinary() {
        AlmacenCloudinary.Credenciales c = AlmacenCloudinary.Credenciales.de("cloudinary://123456:secreto-xyz@mi-nube");
        assertThat(c.cloud()).isEqualTo("mi-nube");
        assertThat(c.apiKey()).isEqualTo("123456");
        assertThat(c.apiSecret()).isEqualTo("secreto-xyz");
        assertThatThrownBy(() -> AlmacenCloudinary.Credenciales.de("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AlmacenCloudinary.Credenciales.de("cloudinary://sin-secreto@nube")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void soloSeAceptanArchivosQueRealmenteSonImagenes() {
        assertThat(ImagenEventoService.esImagen(PNG)).isTrue();
        assertThat(ImagenEventoService.esImagen(JPEG)).isTrue();
        assertThat(ImagenEventoService.esImagen(WEBP)).isTrue();
        assertThat(ImagenEventoService.esImagen("<script>alert(1)</script>".getBytes())).isFalse();
        assertThat(ImagenEventoService.esImagen(new byte[3])).isFalse();
    }
}
