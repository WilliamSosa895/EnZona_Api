package com.uv.enzona.api.boletaje.servicio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uv.enzona.api.boletaje.web.dto.WebhookPagoSolicitud;
import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Atiende la notificación de la pasarela (Anexo E: POST /api/pagos/webhook). Solo se acepta si
 * la firma HMAC-SHA256 del cuerpo coincide con el secreto compartido; si no, responde 401.
 */
@Service
public class WebhookPagoService {

    private static final String PREFIJO_CLAVE = "orden-";

    private final ConfirmacionService confirmaciones;
    private final ObjectMapper json;
    private final byte[] secreto;

    public WebhookPagoService(ConfirmacionService confirmaciones, ObjectMapper json, EnzonaProperties propiedades) {
        this.confirmaciones = confirmaciones;
        this.json = json;
        this.secreto = propiedades.pasarela().secretoWebhook().getBytes(StandardCharsets.UTF_8);
    }

    public void procesar(String cuerpo, String firma) {
        if (firma == null || !MessageDigest.isEqual(calcularFirma(cuerpo).getBytes(StandardCharsets.US_ASCII),
                firma.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw new ApiException(ErrorCodigo.FIRMA_WEBHOOK_INVALIDA);
        }
        WebhookPagoSolicitud notificacion = leer(cuerpo);
        Long ordenId = ordenDe(notificacion.claveIdempotencia());
        switch (notificacion.estado()) {
            case APROBADO -> confirmaciones.confirmarPago(ordenId, notificacion.referencia());
            case RECHAZADO -> confirmaciones.rechazarPago(ordenId, notificacion.referencia());
            case PENDIENTE, DESCONOCIDO -> {
                // Sin cambio: la orden sigue reservada hasta que llegue el resultado o venza.
            }
        }
    }

    /** Firma en hexadecimal; la pasarela calcula la misma con el secreto compartido. */
    public String calcularFirma(String cuerpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible verificar la firma del webhook", e);
        }
    }

    private WebhookPagoSolicitud leer(String cuerpo) {
        try {
            WebhookPagoSolicitud notificacion = json.readValue(cuerpo, WebhookPagoSolicitud.class);
            if (notificacion.claveIdempotencia() == null || notificacion.estado() == null) {
                throw new ApiException(ErrorCodigo.SOLICITUD_INVALIDA, "Faltan claveIdempotencia o estado");
            }
            return notificacion;
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCodigo.SOLICITUD_INVALIDA, "El cuerpo de la notificación no es JSON válido");
        }
    }

    private static Long ordenDe(String clave) {
        if (!clave.startsWith(PREFIJO_CLAVE)) {
            throw new ApiException(ErrorCodigo.SOLICITUD_INVALIDA, "Clave de idempotencia desconocida");
        }
        try {
            return Long.valueOf(clave.substring(PREFIJO_CLAVE.length()));
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCodigo.SOLICITUD_INVALIDA, "Clave de idempotencia desconocida");
        }
    }
}
