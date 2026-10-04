package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Código y firma HMAC-SHA256 de cada boleto (RNF-02, Anexo E DA-04). La firma cubre el código y
 * el evento, de modo que un boleto no sirve para otro evento. La clave solo existe en el servidor.
 */
@Component
public class FirmaBoletos {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final SecretKey clave;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public FirmaBoletos(EnzonaProperties propiedades) {
        this(propiedades.boletos().secretoHmac());
    }

    FirmaBoletos(String claveBase64) {
        byte[] bytes = Base64.getDecoder().decode(claveBase64);
        if (bytes.length < 32) {
            throw new IllegalStateException("TICKET_HMAC_SECRET debe tener al menos 32 bytes en Base64");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Código aleatorio no adivinable: "EZ" + 16 caracteres Base64URL (96 bits). */
    public String nuevoCodigo() {
        byte[] bytes = new byte[12];
        aleatorio.nextBytes(bytes);
        return "EZ" + BASE64_URL.encodeToString(bytes);
    }

    public String firmar(String codigo, Long eventoId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(clave);
            byte[] firma = mac.doFinal((codigo + "|" + eventoId).getBytes(StandardCharsets.UTF_8));
            return BASE64_URL.encodeToString(firma);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible firmar el boleto", e);
        }
    }

    /** Comparación en tiempo constante para no filtrar información por tiempos de respuesta. */
    public boolean verificar(String codigo, Long eventoId, String firma) {
        if (codigo == null || firma == null) {
            return false;
        }
        return MessageDigest.isEqual(firmar(codigo, eventoId).getBytes(StandardCharsets.US_ASCII),
                firma.getBytes(StandardCharsets.US_ASCII));
    }
}
