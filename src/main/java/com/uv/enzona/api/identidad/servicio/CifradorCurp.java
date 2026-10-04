package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Protección del CURP en reposo (RNF-03, LFPDPPP).
 *
 * <ul>
 *   <li>{@link #cifrar(String)}: AES-256-GCM con vector de inicialización aleatorio de 12 bytes.
 *       El resultado es Base64(iv || texto cifrado || etiqueta).</li>
 *   <li>{@link #indice(String)}: HMAC-SHA256 con una clave distinta (índice ciego). Permite
 *       comprobar unicidad e iniciar sesión con CURP sin descifrar ni guardar el dato en claro.</li>
 * </ul>
 */
@Component
public class CifradorCurp {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKey claveCifrado;
    private final SecretKey claveIndice;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public CifradorCurp(EnzonaProperties propiedades) {
        this(propiedades.curp().claveCifrado(), propiedades.curp().claveIndice());
    }

    CifradorCurp(String claveCifradoBase64, String claveIndiceBase64) {
        byte[] cifrado = Base64.getDecoder().decode(claveCifradoBase64);
        if (cifrado.length != 32) {
            throw new IllegalStateException("CURP_ENCRYPTION_KEY debe ser de 32 bytes (AES-256) en Base64");
        }
        byte[] indice = Base64.getDecoder().decode(claveIndiceBase64);
        if (indice.length < 32) {
            throw new IllegalStateException("CURP_INDEX_KEY debe tener al menos 32 bytes en Base64");
        }
        this.claveCifrado = new SecretKeySpec(cifrado, "AES");
        this.claveIndice = new SecretKeySpec(indice, "HmacSHA256");
    }

    public static String normalizar(String curp) {
        return curp == null ? null : curp.trim().toUpperCase(Locale.ROOT);
    }

    public String cifrar(String curp) {
        try {
            byte[] iv = new byte[BYTES_IV];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveCifrado, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cipher.doFinal(normalizar(curp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
                    .put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible cifrar el CURP", e);
        }
    }

    public String descifrar(String textoCifrado) {
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifrado);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveCifrado, new GCMParameterSpec(BITS_ETIQUETA, datos, 0, BYTES_IV));
            byte[] claro = cipher.doFinal(datos, BYTES_IV, datos.length - BYTES_IV);
            return new String(claro, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No fue posible descifrar el CURP", e);
        }
    }

    public String indice(String curp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(claveIndice);
            return HexFormat.of().formatHex(mac.doFinal(normalizar(curp).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible calcular el índice del CURP", e);
        }
    }
}
