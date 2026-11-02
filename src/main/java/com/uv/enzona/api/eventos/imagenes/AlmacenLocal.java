package com.uv.enzona.api.eventos.imagenes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.uv.enzona.api.comun.config.EnzonaProperties;

/**
 * Almacén para desarrollo y demostraciones: los archivos se guardan en una carpeta
 * de la máquina donde corre la API y se sirven en {@code /api/imagenes/{archivo}}.
 * La URL que se guarda es relativa a la API, así vale desde cualquier red.
 */
@Component
@ConditionalOnProperty(name = "enzona.imagenes.proveedor", havingValue = "local", matchIfMissing = true)
public class AlmacenLocal implements AlmacenImagenes {

    public static final String RUTA_PUBLICA = "/api/imagenes/";

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path carpeta;

    @Autowired
    public AlmacenLocal(EnzonaProperties propiedades) {
        this(Path.of(propiedades.imagenes().carpetaLocal()));
    }

    AlmacenLocal(Path carpeta) {
        this.carpeta = carpeta.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.carpeta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de imágenes " + this.carpeta, e);
        }
    }

    @Override
    public ImagenGuardada guardar(byte[] datos, String tipoMime, String grupo) {
        String extension = EXTENSIONES.getOrDefault(tipoMime, "bin");
        // El nombre lo decide el servidor (UUID): nunca se usa el nombre que envió el cliente.
        String archivo = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Files.write(carpeta.resolve(archivo), datos);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
        return new ImagenGuardada(archivo, RUTA_PUBLICA + archivo);
    }

    @Override
    public void eliminar(String id) {
        Path destino = resolver(id);
        if (destino == null) {
            return;
        }
        try {
            Files.deleteIfExists(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar la imagen " + id, e);
        }
    }

    /** Ruta del archivo dentro de la carpeta, o null si el nombre intenta salirse de ella. */
    public Path resolver(String archivo) {
        if (archivo == null || archivo.isBlank()) {
            return null;
        }
        Path destino = carpeta.resolve(archivo).normalize();
        return destino.getParent() != null && destino.getParent().equals(carpeta) ? destino : null;
    }

    /** Tipo MIME según la extensión del archivo guardado. */
    public static String tipoMimeDe(String archivo) {
        String nombre = archivo.toLowerCase();
        if (nombre.endsWith(".png")) {
            return "image/png";
        }
        if (nombre.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
