package com.uv.enzona.api.eventos.imagenes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sin servicio de moderación configurado ({@code IMAGE_MODERATION=off}): acepta
 * todo y deja constancia en la bitácora. Solo para desarrollo y demostraciones;
 * en producción configura {@code sightengine} o {@code cloudinary}.
 */
@Component
@ConditionalOnProperty(name = "enzona.imagenes.moderacion", havingValue = "off", matchIfMissing = true)
public class ModeradorDesactivado implements ModeradorImagenes {

    private static final Logger log = LoggerFactory.getLogger(ModeradorDesactivado.class);

    @Override
    public Veredicto moderar(byte[] datos, String tipoMime) {
        log.warn("Moderación de imágenes desactivada (IMAGE_MODERATION=off): la portada se acepta sin revisar");
        return Veredicto.aprobada();
    }
}
