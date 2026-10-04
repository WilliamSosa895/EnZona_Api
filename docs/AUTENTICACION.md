# Autenticación y autorización

La API no guarda sesiones (Anexo E, DA-03). Cada petición protegida envía un **JWT** firmado con HS256 en la cabecera `Authorization`.

## 1. Crear la cuenta y verificarla (RF-01, RF-02)

1. `POST /api/auth/registro` con nombre, correo, CURP, contraseña y `aceptaAvisoPrivacidad: true`. La cuenta queda `PENDIENTE` y se envía un código de 6 dígitos al correo (y otro al teléfono, si se registró uno).
2. `POST /api/auth/verificacion` con el correo, el canal (`CORREO` o `TELEFONO`) y el código. Cuando el correo y, si lo hay, el teléfono están verificados, la cuenta pasa a `ACTIVO`.
3. Si el código venció o se agotaron sus intentos: `POST /api/auth/verificacion/reenvio`.

Reglas de los códigos:
- Duran 10 minutos (`OTP_VALIDITY_MINUTES`) y admiten 5 intentos (`OTP_MAX_ATTEMPTS`).
- Solo se guarda su hash.
- Pedir uno nuevo invalida el anterior, y hay un mínimo de 60 segundos entre reenvíos.

## 2. Iniciar sesión (RF-03)

```http
POST /api/auth/sesion
Content-Type: application/json

{ "identificador": "ana@correo.com", "contrasena": "Secreta123" }
```

El `identificador` puede ser el correo o el CURP. La respuesta trae el token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 3600,
  "expiraEn": "2026-10-04T13:00:00-06:00",
  "usuario": { "id": 12, "nombre": "Ana Pérez", "correo": "ana@correo.com", "roles": ["ASISTENTE"] }
}
```

Protecciones:
- **Bloqueo temporal.** Tras 5 contraseñas incorrectas, el inicio de sesión se bloquea 15 minutos (respuesta 429).
- **Sin enumeración de usuarios.** Si la cuenta no existe, la respuesta y el tiempo de respuesta son iguales que con una contraseña incorrecta.
- **Estado de la cuenta.** Solo se informa (`CUENTA_PENDIENTE`, `CUENTA_BLOQUEADA`) después de comprobar la contraseña.

## 3. Enviar el token

```http
GET /api/boletos
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

- **Token inválido o vencido:** responde **401** con `codigo: NO_AUTENTICADO`. El cliente debe volver a iniciar sesión; no hay token de renovación en esta versión.
- **Estado y roles en vivo:** se leen de la base de datos en cada petición, no del token. Si un administrador bloquea una cuenta o cambia sus roles, el cambio aplica de inmediato.

Contenido del token (no se debe confiar en él del lado del cliente para decisiones de seguridad):

| Claim | Valor |
|---|---|
| `sub` | id del usuario |
| `iss` | `JWT_ISSUER` |
| `iat`, `exp` | emisión y vencimiento |
| `jti` | identificador único del token |
| `nombre`, `roles` | informativos, para la interfaz |

## 4. Roles

Un usuario puede acumular roles.

| Rol | Cómo se obtiene |
|---|---|
| `ASISTENTE` | Al registrarse (siempre). |
| `ORGANIZADOR` | Al registrarse con `organizador: true`, o con `POST /api/usuarios/me/roles/organizador`. |
| `VALIDADOR` | Lo concede un organizador al designar a la cuenta en uno de sus eventos (`POST /api/eventos/{id}/validadores`). |
| `ADMIN` | Cuenta inicial creada con `ADMIN_EMAIL` y `ADMIN_PASSWORD`. |

## 5. Permisos

Además del rol, varias operaciones exigen una relación con el recurso. Por ejemplo, solo el organizador **dueño** de un evento lo modifica.

| Operación | Público | Asistente | Organizador | Validador | Admin |
|---|:-:|:-:|:-:|:-:|:-:|
| Registro, verificación, sesión | ✔ | | | | |
| Ver catálogo, detalle de evento y categorías | ✔ | ✔ | ✔ | ✔ | ✔ |
| Perfil propio | | ✔ | ✔ | ✔ | ✔ |
| Comprar o confirmar, ver mis órdenes y boletos | | ✔ | ✔ | ✔ | ✔ |
| Ver mapa de asientos | | ✔ | ✔ | ✔ | ✔ |
| Crear, modificar y publicar eventos | | | ✔ (dueño) | | |
| Designar validadores, reportes, política y cancelación | | | ✔ (dueño) | | ✔ |
| Validar QR, manifiesto y sincronización | | | ✔ (dueño) | ✔ (designado) | ✔ |
| Gestión de usuarios y supervisión de eventos | | | | | ✔ |

Un recurso ajeno responde **403** (por ejemplo, editar el evento de otro organizador). Las órdenes y boletos de otra persona responden **404**, para no revelar que existen.

## 6. Webhook de la pasarela

`POST /api/pagos/webhook` no usa JWT. La pasarela firma el cuerpo con HMAC-SHA256 y `PAYMENT_WEBHOOK_SECRET`, y envía la firma en hexadecimal en la cabecera `X-Pasarela-Firma`. Si la firma no coincide, la API responde 401.

## 7. Buenas prácticas para los clientes

- Guarda el token en almacenamiento seguro: EncryptedSharedPreferences o DataStore cifrado en Android, y memoria o almacenamiento de sesión en la web. Nunca lo escribas en registros.
- Usa HTTPS siempre fuera de desarrollo.
- Genera una `Idempotency-Key` nueva (por ejemplo, un UUID) por cada intento de compra y reutilízala si reintentas ese mismo intento.
