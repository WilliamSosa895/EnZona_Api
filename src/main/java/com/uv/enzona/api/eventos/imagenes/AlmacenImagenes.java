package com.uv.enzona.api.eventos.imagenes;

/**
 * Dónde se guardan las portadas de los eventos. La API es la única que habla con
 * el almacén: el cliente sube el archivo a la API y recibe una URL pública, así
 * las credenciales del proveedor (Cloudinary) nunca salen del servidor.
 *
 * Implementaciones: {@link AlmacenLocal} (archivos en disco servidos por la API,
 * para desarrollo y demostraciones) y {@link AlmacenCloudinary}.
 */
public interface AlmacenImagenes {

    /** Imagen guardada: identificador en el almacén y URL pública. */
    record ImagenGuardada(String id, String url) {
    }

    /**
     * Guarda la imagen y devuelve su identificador y su URL.
     *
     * @param datos    bytes del archivo
     * @param tipoMime image/jpeg, image/png o image/webp (ya validado)
     * @param carpeta  agrupación lógica, por ejemplo {@code eventos/7}
     */
    ImagenGuardada guardar(byte[] datos, String tipoMime, String carpeta);

    /** Elimina la imagen; no falla si ya no existe. */
    void eliminar(String id);
}
