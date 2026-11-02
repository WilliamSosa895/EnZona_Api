# Correo con Resend (SMTP)

Con `NOTIFICATIONS_MODE=smtp` la API envía los correos a través del `JavaMailSender` de Spring (`CorreoNotificador`). Resend ofrece un servidor SMTP compatible, así que no hace falta código nuevo: solo variables de entorno. Esta guía explica cómo crear la cuenta, qué limita el plan gratuito, cómo verificar un dominio y cómo comprobar los envíos.

Las variables se describen una por una en [VARIABLES_DE_ENTORNO.md](VARIABLES_DE_ENTORNO.md) y la plantilla está en [`.env.example`](../.env.example).

## Qué correos manda la API

| Correo | Cuándo | Contenido |
|---|---|---|
| «Tu código de verificación de EnZona» | Al registrarse y en `POST /api/auth/verificacion/reenvio` | Código de 6 dígitos y minutos de vigencia (`OTP_VALIDITY_MINUTES`). |
| «Tus boletos para *evento*» | Al confirmar una orden (sin cobro o tras el pago) | Fecha, lugar y, por boleto, tipo, asiento, código y un PNG con el QR incrustado en el cuerpo (`cid:qr0`, `cid:qr1`, ...). |
| «Evento cancelado: *evento*» | Al cancelar un evento, uno por asistente | Motivo (si lo hay) y aviso del reembolso del 100 %. |
| «Cambios en *evento*» | Al modificar un evento publicado con boletos vendidos, uno por asistente | Resumen de los cambios. |

Todos salen **después de confirmar la transacción y en segundo plano**: un fallo del correo nunca revierte el registro, la compra ni la cancelación, y el boleto siempre queda disponible en la app. La API no reintenta ni encola: el correo que el proveedor rechaza se pierde y el fallo queda como `ERROR` en la bitácora.

El canal `TELEFONO` **no tiene proveedor**: el código de teléfono no se envía (solo queda un `WARN`). Resend no cubre SMS.

## 1. Crear la cuenta y la API key

1. Crea una cuenta en <https://resend.com>. El correo con el que te registras es el «dueño de la cuenta» y es el único destinatario posible mientras no verifiques un dominio (sección 3).
2. En **API Keys > Create API Key** elige el permiso **Sending access** (basta para SMTP; *Full access* solo si vas a administrar dominios por API). Si restringes la clave a un dominio, usa el que vas a verificar.
3. Copia la clave (`re_...`): **se muestra una sola vez**. Si la pierdes, crea otra y borra la anterior.
4. Guárdala solo en `.env` (`RESEND_API_KEY=`) o en el gestor de secretos del servidor. Nunca en el repositorio, en capturas ni en mensajes. Si se filtra, revócala en el panel y crea una nueva.

## 2. Configurar la API

```
NOTIFICATIONS_MODE=smtp
MAIL_HOST=smtp.resend.com
MAIL_PORT=587
MAIL_USERNAME=resend
RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxxxxxx
MAIL_FROM="EnZona <no-reply@tu-dominio.mx>"
```

- **Usuario:** literalmente `resend`. **Contraseña:** la API key, en `RESEND_API_KEY` o en `MAIL_PASSWORD` (es lo mismo). `RESEND_API_KEY` solo se usa si `MAIL_PASSWORD` no está definida: una `MAIL_PASSWORD` definida pero vacía la anula, así que no la definas (en `.env.example` va comentada).
- **Puerto:** `587` con STARTTLS (valor por defecto); `2587` si tu red bloquea el 587. Los puertos `465` y `2465` usan TLS implícito y requieren `MAIL_SSL=true`.
- **STARTTLS obligatorio:** `MAIL_STARTTLS_REQUIRED=true` (por defecto) hace fallar el envío si el servidor no ofrece cifrado, para que la clave nunca viaje en claro.
- **Remitente:** `MAIL_FROM` admite «Nombre <correo>». Si solo pones la dirección, la API añade el nombre «EnZona». Si tu forma de cargar `.env` no quita las comillas, usa solo la dirección. Con Resend la dirección debe pertenecer a un dominio verificado (sección 4).
- **Tiempos de espera:** `MAIL_CONNECTION_TIMEOUT_MS=5000`, `MAIL_TIMEOUT_MS=10000` y `MAIL_WRITE_TIMEOUT_MS=10000` por defecto. Sin ellos una conexión colgada dejaría el envío esperando para siempre.
- **Salud:** `MAIL_HEALTH_ENABLED=false` (por defecto) deja `GET /actuator/health` fuera del alcance del proveedor de correo. Con `true` cada sonda abre una sesión SMTP contra Resend y una caída suya marcaría toda la API como `DOWN`.
- **Arranque seguro:** con `NOTIFICATIONS_MODE=smtp` y servidor remoto, la API no arranca si `MAIL_USERNAME` está definido sin clave (`NOTIFICATIONS_MODE=smtp requiere MAIL_PASSWORD o RESEND_API_KEY (clave de Resend); si usas RESEND_API_KEY no definas MAIL_PASSWORD vacía`) ni si `MAIL_SMTP_AUTH=true` (por defecto) con `MAIL_USERNAME` vacío (`... requiere MAIL_USERNAME`).

En desarrollo puedes seguir con `NOTIFICATIONS_MODE=log`: los códigos salen en la consola y no se abre ninguna conexión SMTP.

## 3. Sin dominio verificado: solo al dueño de la cuenta

Mientras no verifiques un dominio, Resend solo entrega correos **a la dirección con la que creaste la cuenta**, y únicamente desde `onboarding@resend.dev`. Cualquier otro destinatario se rechaza con un error como:

```
You can only send testing emails to your own email address (tu@correo.com).
To send emails to other recipients, please verify a domain at resend.com/domains
```

Para la API esto significa que un usuario que se registre con otro correo queda `PENDIENTE` y nunca recibe el código: la respuesta HTTP es normal (`201`) y el fallo solo se ve en la bitácora, donde `CorreoNotificador` lo explica con un `ERROR` que remite a esta guía. Es decir, **sin dominio verificado el registro de terceros no funciona**. Usa este modo solo para probar contigo mismo:

```
MAIL_FROM="EnZona <onboarding@resend.dev>"
```

## 4. Verificar un dominio

1. En **Domains > Add Domain** escribe el dominio (puede ser un subdominio, p. ej. `correo.enzona.mx`, lo que evita tocar los registros del dominio principal) y elige la región.
2. Resend muestra los registros DNS que debes crear en tu proveedor de dominio: **DKIM** (registro TXT `resend._domainkey`), **SPF** (registro TXT con `v=spf1 include:amazonses.com ~all` y un registro MX en el subdominio de envío) y, opcionalmente, **DMARC**.
3. Pulsa **Verify**. La propagación suele tardar minutos, aunque algunos proveedores llegan a 72 h. El estado del dominio cambia a *Verified*.
4. Pon en `MAIL_FROM` una dirección de ese dominio (`no-reply@enzona.mx` o `no-reply@correo.enzona.mx`). No hace falta que exista el buzón; los rebotes los recoge Resend.

## 5. Límites del plan gratuito

| Límite | Plan gratuito |
|---|---|
| Correos por día | 100 |
| Correos por mes | 3 000 |
| Peticiones | 10 por segundo |
| Dominios verificados | 3 |

Comprueba las cifras vigentes en <https://resend.com/pricing>. Consecuencias para EnZona:

- Cada compra genera un correo y cada cancelación o modificación de un evento genera **un correo por asistente**: un solo evento con más de 100 boletos agota el día.
- La API envía en secuencia dentro de un hilo y no reintenta: lo que Resend rechace por cuota (`429`) se pierde, queda como `ERROR` en la bitácora y el boleto sigue disponible en la app.
- Antes de un evento grande, revisa el consumo en el panel o pasa a un plan de pago.

## 6. Comprobar los envíos

- **Dashboard > Emails** lista cada correo con su estado (*Sent*, *Delivered*, *Bounced*, *Complained*); al abrir uno se ve el contenido tal como se entregó y la línea de tiempo de eventos.
- **Dashboard > Logs** registra cada conexión SMTP y llamada a la API con su código de respuesta: es el primer sitio donde buscar un rechazo.
- En la API, los fallos aparecen como `ERROR No se pudo enviar el correo "..."` con la excepción completa; en el perfil `dev` la bitácora de `com.uv.enzona` está en `DEBUG`.
- Prueba de extremo a extremo: registra una cuenta en la app con tu correo y escribe el código recibido; compra un boleto y comprueba que el QR se ve incrustado en el cuerpo (Gmail y Outlook lo muestran en línea; algunos clientes lo presentan como adjunto, y el boleto siempre está en la app).

## Otros proveedores SMTP (mientras no haya dominio verificado)

La API no depende de Resend: `NOTIFICATIONS_MODE=smtp` funciona con cualquier servidor SMTP.
Mientras no se verifique un dominio, estas opciones entregan correo a cualquier destinatario
con las mismas variables (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`):

| Proveedor | Costo | Host y puerto | Usuario | Contraseña | Remitente (`MAIL_FROM`) | Notas |
|---|---|---|---|---|---|---|
| Gmail con contraseña de aplicación | Gratis, ~500 destinatarios/día | `smtp.gmail.com` · 587 (STARTTLS) | la cuenta de Gmail | contraseña de aplicación de 16 letras (requiere verificación en dos pasos) | la misma cuenta de Gmail | La más sencilla y con mejor entrega a Gmail; Google firma los correos. No sirve para volumen alto. |
| Brevo (antes Sendinblue) | Gratis, 300/día | `smtp-relay.brevo.com` · 587 (STARTTLS) o 465 (`MAIL_SSL=true`) | el **Login** que muestra SMTP & API → SMTP (una dirección generada tipo `c0140xxxx@smtp-brevo.com`, no el correo con el que se entró; con el correo da `535 Authentication failed`) | **clave SMTP** (no la clave de API), en SMTP & API → SMTP | un remitente verificado en Brevo | Permite verificar un solo remitente sin dominio. Un remitente `@gmail.com` reenviado por Brevo puede caer en no deseados en Gmail (no lo firma Google). |
| SendGrid | Solo prueba de 60 días (100/día); después de pago | `smtp.sendgrid.net` · 587 | literalmente `apikey` | la API key | remitente verificado (Single Sender) | El plan gratuito permanente se retiró en mayo de 2025. |
| Resend con dominio verificado | Gratis, 100/día y 3 000/mes | `smtp.resend.com` · 587 | `resend` | `RESEND_API_KEY` | `no-reply@<dominio verificado>` | La opción recomendada en cuanto exista el dominio (ver secciones anteriores). |

Al cambiar de proveedor basta editar el `.env` y reiniciar la API; no se toca código ni la app.

## 7. Problemas frecuentes

| Síntoma | Causa | Qué hacer |
|---|---|---|
| `535 5.7.8 Authentication failed` con Brevo | `MAIL_USERNAME` es el correo de la cuenta en vez del Login generado `…@smtp-brevo.com`, o se pegó la clave de API en lugar de la clave SMTP (`xsmtpsib-…`) | Copiar el Login y la clave de la pestaña SMTP & API → SMTP. Brevo reescribe el remitente Gmail como `usuario@<id>.brevosend.com`: es normal mientras no haya dominio propio |
| La API no arranca con `NOTIFICATIONS_MODE=smtp requiere MAIL_PASSWORD o RESEND_API_KEY` | Falta la clave, o `MAIL_PASSWORD` está definida pero vacía y anula a `RESEND_API_KEY` | Define `RESEND_API_KEY` (o `MAIL_PASSWORD`) en `.env`; si usas `RESEND_API_KEY`, no definas `MAIL_PASSWORD` vacía. |
| La API no arranca con `NOTIFICATIONS_MODE=smtp con MAIL_SMTP_AUTH=true requiere MAIL_USERNAME` | Servidor remoto con autenticación (por defecto) y `MAIL_USERNAME` vacío | Con Resend, `MAIL_USERNAME=resend`; con un relay que no pide usuario, `MAIL_SMTP_AUTH=false`. |
| `535 Authentication failed` o `Invalid API key` | Clave incorrecta, revocada o sin permiso de envío | Crea otra clave con *Sending access* y actualiza `.env`. |
| `You can only send testing emails to your own email address` | Dominio sin verificar | Secciones 3 y 4. |
| `The ... domain is not verified` | `MAIL_FROM` usa un dominio que no está verificado en esta cuenta | Verifica el dominio o cambia `MAIL_FROM`. |
| `Couldn't connect to host` o `connect timed out` a los 5 s | El puerto está bloqueado en la red | `MAIL_PORT=2587`, o `465`/`2465` con `MAIL_SSL=true`. |
| `STARTTLS is required but host does not support STARTTLS` | El servidor no ofrece STARTTLS (relay sin cifrado o Mailpit/MailHog en local) | Usa un servidor con STARTTLS o, solo en desarrollo local, `MAIL_STARTTLS=false` y `MAIL_STARTTLS_REQUIRED=false` (y `MAIL_SMTP_AUTH=false` si no pide usuario). |
| `Read timed out` o la conexión se cierra al conectar a `465` o `2465` | Ese puerto es TLS implícito | `MAIL_SSL=true` (o usa `587` con STARTTLS). |
| `429 Too many requests` o cuota diaria agotada | Límites del plan | Espera o pasa a un plan de pago; los correos rechazados no se reenvían solos. |
| El correo llega a no deseados | Dominio recién verificado o sin DMARC; uso de `onboarding@resend.dev` | Añade DMARC, usa siempre el dominio verificado y un remitente estable. |
| `GET /actuator/health` responde `503` con `MAIL_HEALTH_ENABLED=true` | Resend inaccesible o credenciales inválidas | Deja `MAIL_HEALTH_ENABLED=false` (por defecto) o corrige la conexión. |
