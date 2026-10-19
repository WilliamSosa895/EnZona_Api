package com.uv.enzona.api.eventos.imagenes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;

/**
 * Almacén en Cloudinary mediante su API REST de subida firmada (sin SDK): la firma
 * SHA-1 de los parámetros con el api_secret se calcula aquí, en el servidor, y el
 * cliente nunca conoce las credenciales ni puede subir por su cuenta.
 *
 * Configuración: {@code CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name}.
 */
@Component
@ConditionalOnProperty(name = "enzona.imagenes.proveedor", havingValue = "cloudinary")
public class AlmacenCloudinary implements AlmacenImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenCloudinary.class);
    private static final String CARPETA_RAIZ = "enzona";

    private final Credenciales credenciales;
    private final HttpClient http;
    private final ObjectMapper json;
    private final Clock reloj;

    @Autowired
    public AlmacenCloudinary(EnzonaProperties propiedades, ObjectMapper json, Clock reloj) {
        this(Credenciales.de(propiedades.imagenes().cloudinaryUrl()), HttpClient.newHttpClient(), json, reloj);
    }

    AlmacenCloudinary(Credenciales credenciales, HttpClient http, ObjectMapper json, Clock reloj) {
        this.credenciales = credenciales;
        this.http = http;
        this.json = json;
        this.reloj = reloj;
    }

    /** Partes de {@code cloudinary://api_key:api_secret@cloud_name}. */
    record Credenciales(String cloud, String apiKey, String apiSecret) {
        static Credenciales de(String url) {
            if (url == null || !url.startsWith("cloudinary://")) {
                throw new IllegalStateException("Falta CLOUDINARY_URL (cloudinary://api_key:api_secret@cloud_name)");
            }
            URI uri = URI.create(url);
            String[] usuario = uri.getUserInfo() == null ? new String[0] : uri.getUserInfo().split(":", 2);
            if (usuario.length != 2 || uri.getHost() == null) {
                throw new IllegalStateException("CLOUDINARY_URL no tiene el formato cloudinary://api_key:api_secret@cloud_name");
            }
            return new Credenciales(uri.getHost(), usuario[0], usuario[1]);
        }
    }

    @Override
    public ImagenGuardada guardar(byte[] datos, String tipoMime, String grupo) {
        String publicId = CARPETA_RAIZ + "/" + grupo + "/" + UUID.randomUUID().toString().replace("-", "");
        Map<String, String> parametros = new TreeMap<>();
        parametros.put("public_id", publicId);
        parametros.put("timestamp", String.valueOf(reloj.instant().getEpochSecond()));
        parametros.put("overwrite", "true");
        JsonNode respuesta = enviar("upload", parametros, datos, tipoMime);
        return new ImagenGuardada(respuesta.path("public_id").asText(publicId), respuesta.path("secure_url").asText());
    }

    @Override
    public void eliminar(String id) {
        Map<String, String> parametros = new TreeMap<>();
        parametros.put("public_id", id);
        parametros.put("timestamp", String.valueOf(reloj.instant().getEpochSecond()));
        try {
            enviar("destroy", parametros, null, null);
        } catch (ApiException e) {
            log.warn("No se pudo borrar la imagen {} en Cloudinary: {}", id, e.getMessage());
        }
    }

    /** Firma de Cloudinary: SHA-1 de los parámetros ordenados ({@code a=1&b=2}) seguidos del api_secret, en hexadecimal. */
    static String firmar(Map<String, String> parametros, String apiSecret) {
        StringBuilder cadena = new StringBuilder();
        new TreeMap<>(parametros).forEach((clave, valor) -> {
            if (!cadena.isEmpty()) {
                cadena.append('&');
            }
            cadena.append(clave).append('=').append(valor);
        });
        cadena.append(apiSecret);
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-1").digest(cadena.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode enviar(String accion, Map<String, String> parametros, byte[] archivo, String tipoMime) {
        String limite = "----EnZona" + UUID.randomUUID();
        List<byte[]> partes = new ArrayList<>();
        parametros.forEach((clave, valor) -> partes.add(campo(limite, clave, valor)));
        partes.add(campo(limite, "api_key", credenciales.apiKey()));
        partes.add(campo(limite, "signature", firmar(parametros, credenciales.apiSecret())));
        if (archivo != null) {
            partes.add(("--" + limite + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"imagen\"\r\n"
                    + "Content-Type: " + tipoMime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            partes.add(archivo);
            partes.add("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        partes.add(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        byte[] cuerpo = concatenar(partes);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + credenciales.cloud() + "/image/" + accion))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                log.warn("Cloudinary respondió {} a {}: {}", respuesta.statusCode(), accion, respuesta.body());
                throw new ApiException(ErrorCodigo.SERVICIO_EXTERNO_NO_DISPONIBLE, "El almacén de imágenes rechazó la operación");
            }
            return json.readTree(respuesta.body());
        } catch (IOException e) {
            throw new ApiException(ErrorCodigo.SERVICIO_EXTERNO_NO_DISPONIBLE, "No se pudo contactar al almacén de imágenes");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCodigo.SERVICIO_EXTERNO_NO_DISPONIBLE, "La subida de la imagen se interrumpió");
        }
    }

    private static byte[] campo(String limite, String nombre, String valor) {
        return ("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor + "\r\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concatenar(List<byte[]> partes) {
        int total = partes.stream().mapToInt(p -> p.length).sum();
        byte[] todo = new byte[total];
        int posicion = 0;
        for (byte[] parte : partes) {
            System.arraycopy(parte, 0, todo, posicion, parte.length);
            posicion += parte.length;
        }
        return todo;
    }
}
