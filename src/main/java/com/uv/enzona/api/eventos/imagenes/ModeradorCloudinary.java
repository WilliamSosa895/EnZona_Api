package com.uv.enzona.api.eventos.imagenes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.uv.enzona.api.comun.config.EnzonaProperties;

/**
 * {@code IMAGE_MODERATION=cloudinary}: la revisión la hace el add-on de moderación
 * de Cloudinary (AWS Rekognition) en la misma subida, dentro de
 * {@link AlmacenCloudinary#guardar}, que rechaza y borra la imagen si no fue
 * aprobada. Por eso aquí no hay una llamada previa: este moderador solo existe
 * para que la aplicación arranque con una configuración coherente y falle de
 * inmediato si la moderación de Cloudinary se pide sin usar Cloudinary como almacén.
 */
@Component
@ConditionalOnProperty(name = "enzona.imagenes.moderacion", havingValue = "cloudinary")
public class ModeradorCloudinary implements ModeradorImagenes {

    @Autowired
    public ModeradorCloudinary(EnzonaProperties propiedades) {
        this(propiedades.imagenes().proveedor());
    }

    ModeradorCloudinary(String proveedor) {
        if (!"cloudinary".equals(proveedor)) {
            throw new IllegalStateException("IMAGE_MODERATION=cloudinary requiere IMAGE_PROVIDER=cloudinary "
                    + "(el add-on modera la imagen al subirla); el proveedor configurado es '" + proveedor + "'");
        }
    }

    @Override
    public Veredicto moderar(byte[] datos, String tipoMime) {
        return Veredicto.aprobada();     // la decisión real llega con la respuesta de la subida a Cloudinary
    }
}
