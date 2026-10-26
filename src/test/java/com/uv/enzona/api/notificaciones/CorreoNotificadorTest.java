package com.uv.enzona.api.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.identidad.modelo.CanalVerificacion;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Correo SMTP (Resend): estructura MIME, remitente, tolerancia a fallos y arranque seguro.
 * Sin Spring, sin red y sin Docker: el JavaMailSender es un mock que entrega un MimeMessage vacío.
 */
class CorreoNotificadorTest {

    private static final String MENSAJE_SIN_CLAVE =
            "NOTIFICATIONS_MODE=smtp requiere MAIL_PASSWORD o RESEND_API_KEY (clave de Resend); "
                    + "si usas RESEND_API_KEY no definas MAIL_PASSWORD vacía";
    private static final String REMITENTE = "no-reply@enzona.mx";
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final JavaMailSender correo = mock(JavaMailSender.class);
    private final GeneradorQr qr = new GeneradorQr();

    @Test
    void elCorreoDeBoletosLlevaUnQrInlinePorBoleto() throws Exception {
        when(correo.createMimeMessage()).thenReturn(mensajeVacio());
        CorreoNotificador notificador = new CorreoNotificador(correo, qr, propiedades(REMITENTE));

        notificador.enviarBoletos(new MensajeBoletos("ana@example.com", "Ana", "Festival de Jazz",
                OffsetDateTime.parse("2026-11-20T20:00:00-06:00"), "Teatro Llave", List.of(
                        new MensajeBoletos.Boleto("EZ0001", "EZ0001.firma-uno", "General", null),
                        new MensajeBoletos.Boleto("EZ0002", "EZ0002.firma-dos", "VIP", "A-12"))));

        MimeMessage enviado = capturarEnviado();
        assertThat(enviado.getSubject()).isEqualTo("Tus boletos para Festival de Jazz");
        assertThat(direcciones(enviado.getRecipients(Message.RecipientType.TO))).containsExactly("ana@example.com");
        InternetAddress remitente = (InternetAddress) enviado.getFrom()[0];
        assertThat(remitente.getAddress()).isEqualTo(REMITENTE);
        assertThat(remitente.getPersonal()).isEqualTo("EnZona");

        List<BodyPart> inline = partesInline(enviado);
        assertThat(inline).hasSize(2);
        assertThat(contentIds(inline)).containsExactly("<qr0>", "<qr1>");
        for (BodyPart parte : inline) {
            assertThat(parte.isMimeType("image/png")).isTrue();
            assertThat(parte.getInputStream().readNBytes(4)).containsExactly(FIRMA_PNG);
        }
    }

    @Test
    void elCorreoDelCodigoLlevaRemitenteDestinatarioYAsunto() throws Exception {
        when(correo.createMimeMessage()).thenReturn(mensajeVacio());
        CorreoNotificador notificador = new CorreoNotificador(correo, qr, propiedades(REMITENTE));

        notificador.enviarCodigo("ana@example.com", CanalVerificacion.CORREO, "Ana", "123456", 10);

        MimeMessage enviado = capturarEnviado();
        assertThat(enviado.getSubject()).isEqualTo("Tu código de verificación de EnZona");
        assertThat(direcciones(enviado.getFrom())).containsExactly(REMITENTE);
        assertThat(direcciones(enviado.getRecipients(Message.RecipientType.TO))).containsExactly("ana@example.com");
        assertThat(partesInline(enviado)).isEmpty();
    }

    @Test
    void respetaElNombreVisibleSiMailFromYaLoTrae() throws Exception {
        when(correo.createMimeMessage()).thenReturn(mensajeVacio());
        CorreoNotificador notificador =
                new CorreoNotificador(correo, qr, propiedades("Boletos EnZona <boletos@enzona.mx>"));

        notificador.avisarCambio("ana@example.com", "Ana", "Festival de Jazz", "nueva fecha");

        InternetAddress remitente = (InternetAddress) capturarEnviado().getFrom()[0];
        assertThat(remitente.getAddress()).isEqualTo("boletos@enzona.mx");
        assertThat(remitente.getPersonal()).isEqualTo("Boletos EnZona");
    }

    @Test
    void elCodigoPorTelefonoNoSeEnviaPorCorreo() {
        CorreoNotificador notificador = new CorreoNotificador(correo, qr, propiedades(REMITENTE));

        notificador.enviarCodigo("2721234567", CanalVerificacion.TELEFONO, "Ana", "123456", 10);

        verify(correo, never()).send(any(MimeMessage.class));
    }

    @Test
    void unFalloDelServidorDeCorreoNoSePropaga() {
        when(correo.createMimeMessage()).thenReturn(mensajeVacio());
        doThrow(new MailSendException("Conexión rechazada")).when(correo).send(any(MimeMessage.class));
        CorreoNotificador notificador = new CorreoNotificador(correo, qr, propiedades(REMITENTE));

        assertThatCode(() -> notificador.enviarCodigo("ana@example.com", CanalVerificacion.CORREO, "Ana", "123456", 10))
                .doesNotThrowAnyException();
    }

    @Test
    void explicaEnLaBitacoraElRechazoDeResendSinDominioVerificado() {
        when(correo.createMimeMessage()).thenReturn(mensajeVacio());
        doThrow(new MailSendException("Failed messages: 450 You can only send testing emails to your own email address"))
                .when(correo).send(any(MimeMessage.class));
        Logger bitacora = (Logger) LoggerFactory.getLogger(CorreoNotificador.class);
        ListAppender<ILoggingEvent> capturados = new ListAppender<>();
        capturados.start();
        bitacora.addAppender(capturados);
        try {
            new CorreoNotificador(correo, qr, propiedades("onboarding@resend.dev"))
                    .avisarCancelacion("ana@example.com", "Ana", "Festival de Jazz", null);
        } finally {
            bitacora.detachAppender(capturados);
        }

        assertThat(capturados.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(mensaje -> assertThat(mensaje)
                        .contains("dueño de la cuenta")
                        .contains("resend.com/domains"));
    }

    @Test
    void noArrancaConServidorRemotoYUsuarioSinContrasena() {
        JavaMailSenderImpl resend = new JavaMailSenderImpl();
        resend.setHost("smtp.resend.com");
        resend.setUsername("resend");
        resend.setPassword("");

        assertThatThrownBy(() -> new CorreoNotificador(resend, qr, propiedades(REMITENTE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(MENSAJE_SIN_CLAVE);
    }

    @Test
    void noArrancaConServidorRemotoConAutenticacionYSinUsuario() {
        JavaMailSenderImpl resend = new JavaMailSenderImpl();
        resend.setHost("smtp.resend.com");
        resend.setUsername("");
        resend.setPassword("re_clave_de_prueba");
        resend.setJavaMailProperties(propiedadesSmtp("mail.smtp.auth", "true"));

        assertThatThrownBy(() -> new CorreoNotificador(resend, qr, propiedades(REMITENTE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_USERNAME");
    }

    @Test
    void arrancaConContrasenaConServidorLocalSinEllaOConRelaySinAutenticacion() {
        JavaMailSenderImpl resend = new JavaMailSenderImpl();
        resend.setHost("smtp.resend.com");
        resend.setUsername("resend");
        resend.setPassword("re_clave_de_prueba");
        JavaMailSenderImpl local = new JavaMailSenderImpl();
        local.setHost("localhost");
        local.setUsername("");
        local.setPassword("");
        JavaMailSenderImpl relay = new JavaMailSenderImpl();
        relay.setHost("relay.example.com");
        relay.setJavaMailProperties(propiedadesSmtp("mail.smtp.auth", "false"));

        assertThatCode(() -> new CorreoNotificador(resend, qr, propiedades(REMITENTE))).doesNotThrowAnyException();
        assertThatCode(() -> new CorreoNotificador(local, qr, propiedades(REMITENTE))).doesNotThrowAnyException();
        assertThatCode(() -> new CorreoNotificador(relay, qr, propiedades(REMITENTE))).doesNotThrowAnyException();
    }

    @Test
    void noArrancaConUnRemitenteInvalido() {
        assertThatThrownBy(() -> new CorreoNotificador(correo, qr, propiedades("uno@enzona.mx, dos@enzona.mx")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_FROM");
    }

    private static EnzonaProperties propiedades(String remitente) {
        EnzonaProperties propiedades = mock(EnzonaProperties.class);
        when(propiedades.notificaciones()).thenReturn(new EnzonaProperties.Notificaciones("smtp", remitente));
        return propiedades;
    }

    private static Properties propiedadesSmtp(String clave, String valor) {
        Properties propiedades = new Properties();
        propiedades.setProperty(clave, valor);
        return propiedades;
    }

    private static MimeMessage mensajeVacio() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    /** Captura el mensaje entregado al JavaMailSender y fija sus cabeceras como haría el envío real. */
    private MimeMessage capturarEnviado() throws MessagingException {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo).send(captor.capture());
        MimeMessage enviado = captor.getValue();
        enviado.saveChanges();
        return enviado;
    }

    private static List<String> direcciones(Address[] direcciones) {
        List<String> resultado = new ArrayList<>();
        for (Address direccion : direcciones) {
            resultado.add(((InternetAddress) direccion).getAddress());
        }
        return resultado;
    }

    private static List<String> contentIds(List<BodyPart> partes) throws MessagingException {
        List<String> ids = new ArrayList<>();
        for (BodyPart parte : partes) {
            ids.add(parte.getHeader("Content-ID")[0]);
        }
        return ids;
    }

    /** Recorre el árbol MIME (mixed > related) y devuelve las partes con Content-ID, en orden. */
    private static List<BodyPart> partesInline(MimeMessage mensaje) throws MessagingException, IOException {
        List<BodyPart> inline = new ArrayList<>();
        recolectarInline(mensaje.getContent(), inline);
        return inline;
    }

    private static void recolectarInline(Object contenido, List<BodyPart> inline) throws MessagingException, IOException {
        if (!(contenido instanceof Multipart multipart)) {
            return;
        }
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart parte = multipart.getBodyPart(i);
            if (parte.getHeader("Content-ID") != null) {
                inline.add(parte);
            }
            recolectarInline(parte.getContent(), inline);
        }
    }
}
