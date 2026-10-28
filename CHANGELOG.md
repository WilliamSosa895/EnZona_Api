# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones según [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Agregado
- **Correo por Resend (SMTP):** `RESEND_API_KEY` como alternativa a `MAIL_PASSWORD`, STARTTLS obligatorio
  (`MAIL_STARTTLS_REQUIRED`), TLS implícito opcional (`MAIL_SSL`) y tiempos de espera de conexión, lectura y
  escritura (`MAIL_CONNECTION_TIMEOUT_MS`, `MAIL_TIMEOUT_MS`, `MAIL_WRITE_TIMEOUT_MS`). Guía en
  [docs/CORREO_RESEND.md](docs/CORREO_RESEND.md).
- **Arranque seguro del correo:** con `NOTIFICATIONS_MODE=smtp` y servidor remoto, la API no arranca si hay usuario
  sin contraseña o autenticación sin usuario, y lo explica; un rechazo de Resend por dominio sin verificar se
  explica en la bitácora.
- **Pruebas unitarias de `CorreoNotificador`:** MIME con un QR inline por boleto, remitente, destinatario, asunto y
  fallos del servidor que no se propagan.

### Cambiado
- El remitente lleva nombre visible («EnZona») cuando `MAIL_FROM` trae solo la dirección; «Nombre <correo>» se respeta.
- `GET /actuator/health` ya no depende del servidor de correo (`MAIL_HEALTH_ENABLED=false` por defecto); antes
  respondía `503` cuando no había SMTP alcanzable.

## [0.1.0] - 2026-10-04

Primera versión de la API, que cubre los incrementos 0 a 6 del plan de desarrollo.

### Agregado
- **Cimientos:** proyecto Spring Boot 3.3 con Java 21, migraciones Flyway V1 a V5, errores en formato problem+json, Docker, docker-compose y CI con GitHub Actions.
- **Identidad (RF-01 a RF-04):**
  - Registro con CURP cifrado (AES-256-GCM) e índice ciego.
  - Verificación por código de un solo uso.
  - Sesión JWT por correo o CURP, con bloqueo temporal.
  - Perfil y rol de organizador.
  - Administrador inicial.
- **Eventos (RF-05 a RF-09, RF-13, RF-19):**
  - Catálogo con búsqueda, filtros y cercanía.
  - Alta, publicación y modificación con política.
  - Mapa de asientos y validadores por evento.
- **Boletaje y pagos (RF-10 a RF-12, RF-16):**
  - Órdenes de N boletos, todo o nada, sin sobreventa.
  - Idempotencia con `Idempotency-Key`.
  - Pasarela intercambiable con sandbox y webhook firmado.
  - Boletos firmados con HMAC-SHA256.
  - Reembolsos y tareas de órdenes vencidas.
- **Validación de acceso (RF-15, RNF-08):** uso único atómico, manifiesto y sincronización sin conexión con resolución de conflictos.
- **Cancelación (RF-14):** con reembolso total y avisos.
- **Panel del organizador (RF-17).**
- **Administración (RF-18).**
- **Notificaciones:** correo con QR, avisos de cancelación y de cambios.
- **Pruebas:** unitarias e integración con Testcontainers (aforo concurrente, compra, validación, registro).
- **Documentación:** README, endpoints, ejemplos, autenticación, variables de entorno, modelo de datos, códigos de error, OpenAPI y coherencia con la app Android.
