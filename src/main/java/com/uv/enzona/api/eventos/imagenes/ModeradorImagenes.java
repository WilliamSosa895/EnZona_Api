package com.uv.enzona.api.eventos.imagenes;

/**
 * Revisa una portada antes de guardarla: desnudez, contenido sexual, violencia
 * gráfica y autolesiones se rechazan para que nunca lleguen a los asistentes.
 *
 * Vive en el servidor a propósito: una comprobación en la app se puede saltar
 * llamando a la API con otro cliente. Implementaciones: {@link ModeradorSightengine}
 * y {@link ModeradorDesactivado} (desarrollo). Con el almacén de Cloudinary también
 * puede usarse su add-on de moderación (ver {@link AlmacenCloudinary}).
 */
public interface ModeradorImagenes {

    /** Resultado de la revisión. {@code motivo} es el texto que verá quien subió la imagen si se rechaza. */
    record Veredicto(boolean permitida, String motivo) {
        public static Veredicto aprobada() {
            return new Veredicto(true, null);
        }

        public static Veredicto rechazo(String motivo) {
            return new Veredicto(false, motivo);
        }
    }

    Veredicto moderar(byte[] datos, String tipoMime);
}
