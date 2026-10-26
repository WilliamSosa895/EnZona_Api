# Variables de entorno

La API lee su configuración de variables de entorno. La plantilla es [`.env.example`](../.env.example).

**Nunca subas `.env` al repositorio** (ya está en `.gitignore`) ni publiques valores reales en documentos, capturas o mensajes.

Para generar una clave aleatoria de 32 bytes en Base64:

```bash
openssl rand -base64 32
```

## Obligatorias

| Variable | Para qué sirve | Ejemplo / formato |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL. En Aiven agrega `?sslmode=require`. | `jdbc:postgresql://localhost:5432/enzona` |
| `DB_USER` | Usuario de la base de datos. | `enzona` |
| `DB_PASSWORD` | Contraseña de la base de datos. | (secreto) |
| `JWT_SECRET` | Clave HMAC con la que se firman los tokens de sesión. Base64, mínimo 32 bytes. | `openssl rand -base64 32` |
| `TICKET_HMAC_SECRET` | Clave HMAC-SHA256 con la que se firman los boletos (RNF-02). Si se filtra, alguien podría fabricar boletos. Base64, mínimo 32 bytes. | `openssl rand -base64 32` |
| `CURP_ENCRYPTION_KEY` | Clave AES-256 para cifrar el CURP (RNF-03). Base64, exactamente 32 bytes. Si se pierde, los CURP guardados no se pueden descifrar: respáldala. | `openssl rand -base64 32` |
| `CURP_INDEX_KEY` | Clave del índice ciego del CURP, que permite buscarlo sin descifrarlo. Base64, mínimo 32 bytes. No debe cambiar una vez en uso. | `openssl rand -base64 32` |
| `PAYMENT_WEBHOOK_SECRET` | Secreto compartido con la pasarela para verificar la firma del webhook. | (secreto) |

Si falta alguna, la aplicación no arranca: es intencional, para no operar con configuración incompleta.

## Opcionales (tienen valor por defecto)

| Variable | Por defecto | Para qué sirve |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` o `prod`. En `prod` Swagger UI está apagado salvo que `SWAGGER_ENABLED=true`. |
| `PORT` | `8080` | Puerto HTTP. |
| `DB_POOL_MAX` | `8` | Conexiones máximas del pool. El plan gratuito de Aiven admite 20 en total. |
| `JWT_ISSUER` | `enzona-api` | Emisor (`iss`) de los tokens. |
| `JWT_EXPIRATION_MINUTES` | `60` | Vigencia del token de sesión. |
| `ORDER_RESERVATION_MINUTES` | `10` | Minutos que se reservan lugares y asientos mientras se confirma el pago. |
| `ORDER_MAX_TICKETS` | `10` | Boletos máximos por orden. |
| `OTP_VALIDITY_MINUTES` | `10` | Vigencia del código de verificación. |
| `OTP_MAX_ATTEMPTS` | `5` | Intentos por código antes de exigir uno nuevo. |
| `LOGIN_MAX_ATTEMPTS` | `5` | Intentos fallidos de sesión antes del bloqueo temporal. |
| `LOGIN_LOCK_MINUTES` | `15` | Duración del bloqueo temporal de sesión. |
| `PAYMENT_PROVIDER` | `sandbox` | Pasarela de pago. Por ahora solo existe `sandbox` (simulada). |
| `NOTIFICATIONS_MODE` | `log` | `log`: los avisos se escriben en la consola (solo desarrollo). `smtp`: se envían por correo; con Resend sigue [CORREO_RESEND.md](CORREO_RESEND.md). |
| `MAIL_FROM` | `no-reply@enzona.mx` | Remitente. Admite nombre visible (`EnZona <no-reply@tu-dominio.mx>`); si solo trae la dirección, la API añade «EnZona». Con Resend debe pertenecer a un dominio verificado; para probar sin dominio, `EnZona <onboarding@resend.dev>` (solo entrega al correo del dueño de la cuenta). |
| `MAIL_HOST` | `localhost` | Servidor SMTP (solo con `NOTIFICATIONS_MODE=smtp`). Resend: `smtp.resend.com`. |
| `MAIL_PORT` | `587` | Resend: `587` o `2587` con STARTTLS; `465` o `2465` con `MAIL_SSL=true`. |
| `MAIL_USERNAME` | vacío | Usuario SMTP. Resend: literalmente `resend`. Con servidor remoto y `MAIL_SMTP_AUTH=true` (por defecto), la API no arranca si está vacío. |
| `MAIL_PASSWORD` | vacío | Contraseña SMTP. Resend: la API key (`re_...`). Si no está definida se toma `RESEND_API_KEY`; una `MAIL_PASSWORD` definida pero vacía anula a `RESEND_API_KEY`, así que con Resend no la definas. |
| `RESEND_API_KEY` | vacío | API key de Resend (Dashboard > API Keys, permiso *Sending access*); equivale a `MAIL_PASSWORD` y solo se usa si esta no está definida. Con `NOTIFICATIONS_MODE=smtp`, servidor remoto y `MAIL_USERNAME` definido, la API no arranca si faltan ambas. |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | `true`, `true` | Autenticación y STARTTLS. Resend exige ambas en 587/2587. |
| `MAIL_STARTTLS_REQUIRED` | `true` | El envío falla si el servidor no ofrece STARTTLS: la clave nunca viaja en claro. |
| `MAIL_SSL` | `false` | TLS implícito desde la conexión (puertos 465/2465). |
| `MAIL_CONNECTION_TIMEOUT_MS`, `MAIL_TIMEOUT_MS`, `MAIL_WRITE_TIMEOUT_MS` | `5000`, `10000`, `10000` | Tiempos de espera SMTP (conexión, lectura y escritura) en milisegundos. Sin ellos una conexión colgada bloquearía el envío para siempre. |
| `MAIL_HEALTH_ENABLED` | `false` | Incluye el servidor de correo en `GET /actuator/health`. Apagado, el estado no depende del proveedor de correo; encendido, cada sonda abre una sesión SMTP contra `MAIL_HOST`. |
| `EVENT_DEFAULT_DURATION_HOURS` | `6` | Horas tras el inicio en que un evento sin hora de fin se considera terminado. |
| `EVENT_MAX_RADIUS_KM` | `50` | Radio máximo de la búsqueda por cercanía. |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | vacío | Si se definen, se crea la cuenta de administrador al arrancar (solo si no existe). |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes permitidos de la aplicación web, separados por coma. |
| `SWAGGER_ENABLED` | `false` | Habilita Swagger UI en el perfil `prod`. |

## Advertencias

- `NOTIFICATIONS_MODE=log` escribe los códigos de verificación en la consola: úsalo solo en desarrollo.
- Con Resend sin dominio verificado (`onboarding@resend.dev`), los correos solo llegan al dueño de la cuenta: cualquier otro usuario se registra pero nunca recibe el código. La API lo deja como `ERROR` en la bitácora; verifica un dominio antes de abrir el registro a terceros ([CORREO_RESEND.md](CORREO_RESEND.md)).
- `RESEND_API_KEY` (o `MAIL_PASSWORD`) permite enviar correo en nombre del dominio: solo en `.env` o en el gestor de secretos del servidor. Si se filtra, revócala en Resend y crea otra.
- `PAYMENT_PROVIDER=sandbox` no cobra dinero real. Para producción hay que implementar la pasarela elegida (`PasarelaPago`).
- Rotar `JWT_SECRET` cierra todas las sesiones activas. Rotar `TICKET_HMAC_SECRET` invalida los boletos emitidos: hazlo solo entre eventos.
