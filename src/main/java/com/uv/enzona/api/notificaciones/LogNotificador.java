package com.uv.enzona.api.notificaciones;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Notificador de desarrollo (NOTIFICATIONS_MODE=log): escribe los avisos en la bitácora, incluido
 * el código de verificación para poder probar el registro sin un servidor de correo.
 * NUNCA debe usarse en producción.
 */
@Component
@ConditionalOnProperty(name = "enzona.notificaciones.modo", havingValue = "log")
public class LogNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(LogNotificador.class);

    @Override
    public void enviarCodigo(String destino, CanalVerificacion canal, String nombre, String codigo, int minutosVigencia) {
        log.info("[DEV] Código de verificación para {} por {}: {} (vigencia {} min)", destino, canal, codigo,
                minutosVigencia);
    }

    @Override
    public void enviarBoletos(MensajeBoletos mensaje) {
        log.info("[DEV] Correo a {} con {} boleto(s) de \"{}\"", mensaje.destinatario(), mensaje.boletos().size(),
                mensaje.evento());
        mensaje.boletos().forEach(boleto -> log.info("[DEV]   QR: {}", boleto.contenidoQr()));
    }

    @Override
    public void avisarCancelacion(String destinatario, String nombre, String evento, String motivo) {
        log.info("[DEV] Aviso de cancelación de \"{}\" a {}", evento, destinatario);
    }

    @Override
    public void avisarCambio(String destinatario, String nombre, String evento, String cambios) {
        log.info("[DEV] Aviso de cambios ({}) en \"{}\" a {}", cambios, evento, destinatario);
    }
}
