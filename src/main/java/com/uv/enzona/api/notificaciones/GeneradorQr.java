package com.uv.enzona.api.notificaciones;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Genera la imagen PNG del código QR (ISO/IEC 18004) que viaja en el correo del boleto. */
@Component
public class GeneradorQr {

    private static final int TAMANO_PX = 320;

    public byte[] png(String contenido) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, TAMANO_PX, TAMANO_PX,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No fue posible generar el código QR", e);
        }
    }
}
