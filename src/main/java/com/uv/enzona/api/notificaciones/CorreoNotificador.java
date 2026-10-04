package com.uv.enzona.api.notificaciones;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Notificador por correo SMTP (NOTIFICATIONS_MODE=smtp). Un fallo de correo no interrumpe la
 * operación: el boleto siempre está disponible en la app (Anexo E, riesgo E.13).
 */
@Component
@ConditionalOnProperty(name = "enzona.notificaciones.modo", havingValue = "smtp")
public class CorreoNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(CorreoNotificador.class);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm 'h'", Locale.forLanguageTag("es-MX"));

    private final JavaMailSender correo;
    private final GeneradorQr qr;
    private final String remitente;

    public CorreoNotificador(JavaMailSender correo, GeneradorQr qr, EnzonaProperties propiedades) {
        this.correo = correo;
        this.qr = qr;
        this.remitente = propiedades.notificaciones().remitente();
    }

    @Override
    public void enviarCodigo(String destino, CanalVerificacion canal, String nombre, String codigo, int minutosVigencia) {
        if (canal == CanalVerificacion.TELEFONO) {
            log.warn("Envío de SMS no configurado: el código de teléfono no se envió (pendiente de proveedor)");
            return;
        }
        enviar(destino, "Tu código de verificación de EnZona", """
                <p>Hola %s:</p>
                <p>Tu código de verificación es <strong style="font-size:22px;letter-spacing:4px">%s</strong>.</p>
                <p>Vence en %d minutos. Si no creaste una cuenta en EnZona, ignora este correo.</p>
                """.formatted(e(nombre), codigo, minutosVigencia), null);
    }

    @Override
    public void enviarBoletos(MensajeBoletos mensaje) {
        StringBuilder cuerpo = new StringBuilder("""
                <p>Hola %s:</p>
                <p>Estos son tus boletos para <strong>%s</strong> · %s · %s.</p>
                <p>Presenta cada código QR en la entrada; cada uno sirve una sola vez.</p>
                """.formatted(e(mensaje.nombre()), e(mensaje.evento()),
                FORMATO_FECHA.format(Fechas.enZonaMx(mensaje.fechaHoraInicio())), e(mensaje.lugar())));
        for (int i = 0; i < mensaje.boletos().size(); i++) {
            MensajeBoletos.Boleto boleto = mensaje.boletos().get(i);
            cuerpo.append("""
                    <hr><p><strong>Boleto %d de %d</strong> · %s%s<br>Código: %s</p>
                    <img src="cid:qr%d" alt="Código QR del boleto" width="220" height="220">
                    """.formatted(i + 1, mensaje.boletos().size(), e(boleto.tipo()),
                    boleto.asiento() == null ? "" : " · " + e(boleto.asiento()), boleto.codigo(), i));
        }
        enviar(mensaje.destinatario(), "Tus boletos para " + mensaje.evento(), cuerpo.toString(), mensaje);
    }

    @Override
    public void avisarCancelacion(String destinatario, String nombre, String evento, String motivo) {
        enviar(destinatario, "Evento cancelado: " + evento, """
                <p>Hola %s:</p>
                <p>El evento <strong>%s</strong> fue cancelado%s.</p>
                <p>Tus boletos quedaron anulados. Si pagaste, recibirás el reembolso del 100 %% en el mismo medio de pago.</p>
                """.formatted(e(nombre), e(evento), motivo == null ? "" : " (" + e(motivo) + ")"), null);
    }

    @Override
    public void avisarCambio(String destinatario, String nombre, String evento, String cambios) {
        enviar(destinatario, "Cambios en " + evento, """
                <p>Hola %s:</p>
                <p>El organizador de <strong>%s</strong> actualizó: %s.</p>
                <p>Revisa los datos actualizados en la app. Tus boletos siguen siendo válidos.</p>
                """.formatted(e(nombre), e(evento), e(cambios)), null);
    }

    private void enviar(String destinatario, String asunto, String html, MensajeBoletos conQr) {
        try {
            MimeMessage mensaje = correo.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            ayudante.setFrom(remitente);
            ayudante.setTo(destinatario);
            ayudante.setSubject(asunto);
            ayudante.setText("<div style=\"font-family:Arial,sans-serif;color:#1B2432\">" + html
                    + "<p style=\"color:#5D6D7E\">EnZona · Eventos cerca de ti</p></div>", true);
            if (conQr != null) {
                for (int i = 0; i < conQr.boletos().size(); i++) {
                    ayudante.addInline("qr" + i, new ByteArrayResource(qr.png(conQr.boletos().get(i).contenidoQr())),
                            "image/png");
                }
            }
            correo.send(mensaje);
        } catch (MessagingException | MailException e) {
            log.error("No se pudo enviar el correo \"{}\"", asunto, e);
        }
    }

    private static String e(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto);
    }
}
