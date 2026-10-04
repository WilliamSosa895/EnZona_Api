# Códigos de error

Todos los errores usan el formato **Problem Details** (RFC 9457, `application/problem+json`) con una propiedad adicional `codigo`. El cliente debe decidir con base en `codigo` y no en el texto de `detail`, que puede cambiar.

```json
{
  "type": "https://enzona.mx/errores/aforo-agotado",
  "title": "Conflict",
  "status": 409,
  "detail": "No hay lugares suficientes para esta compra",
  "instance": "/api/ordenes",
  "codigo": "AFORO_AGOTADO",
  "timestamp": "2026-10-04T12:00:00.000-06:00"
}
```

Los errores de validación agregan la lista `errores`:

```json
{
  "status": 400,
  "codigo": "VALIDACION",
  "detail": "Los datos enviados no son válidos",
  "errores": [
    { "campo": "curp", "mensaje": "El CURP no tiene un formato válido" },
    { "campo": "contrasena", "mensaje": "La contraseña debe tener de 8 a 72 caracteres e incluir letras y números" }
  ]
}
```

Los errores internos nunca exponen detalles técnicos (trazas, SQL): solo se registran en la bitácora del servidor.

## Códigos HTTP

| HTTP | Significado en EnZona |
|---|---|
| **200** OK | Operación correcta. También: rechazo de acceso en la puerta (es un resultado, no un error) y reintento de una orden con la misma `Idempotency-Key`. |
| **201** Created | Recurso creado (cuenta, evento, orden, designación). La cabecera `Location` apunta al recurso. |
| **202** Accepted | Solicitud recibida sin revelar su resultado (reenvío de código). |
| **204** No Content | Operación correcta sin cuerpo (cambio de contraseña, retirar validador). |
| **400** Bad Request | Datos mal formados o que no cumplen las validaciones; código de verificación incorrecto o vencido; falta `Idempotency-Key`. |
| **401** Unauthorized | Falta el token, es inválido o venció; credenciales incorrectas; firma del webhook inválida. |
| **403** Forbidden | Hay sesión, pero sin permiso: rol insuficiente, evento ajeno, cuenta pendiente o bloqueada al iniciar sesión. |
| **404** Not Found | El recurso no existe o no es visible para quien lo pide (borradores ajenos, órdenes y boletos de otra persona). |
| **409** Conflict | La operación choca con el estado actual: correo o CURP ya registrados, aforo agotado, asiento ocupado, confirmación duplicada, política de modificación, evento no cancelable. |
| **422** Unprocessable Content | La solicitud es correcta en forma, pero rompe una regla de negocio: suma de boletos mayor al aforo, faltan asientos, motivo obligatorio, método de pago no disponible. |
| **429** Too Many Requests | Demasiados intentos: inicio de sesión bloqueado temporalmente o código agotado. |
| **500** Internal Server Error | Error inesperado. Se registra con detalle en el servidor. |
| **503** Service Unavailable | Un servicio externo no respondió (reservado; hoy la orden queda pendiente en lugar de fallar). |

## Catálogo de `codigo`

| `codigo` | HTTP | Cuándo ocurre | Qué hacer en el cliente |
|---|---|---|---|
| `VALIDACION` | 400 | Uno o más campos no cumplen las reglas (ver `errores`). | Marcar los campos indicados. |
| `SOLICITUD_INVALIDA` | 400 | JSON mal formado, parámetro con tipo incorrecto, falta `Idempotency-Key`, webhook ilegible. | Corregir la petición. |
| `CODIGO_VERIFICACION_INVALIDO` | 400 | El código no coincide o no hay código activo (CP-04). | Pedir que revise el código. |
| `CODIGO_VERIFICACION_EXPIRADO` | 400 | El código venció. | Ofrecer reenvío. |
| `NO_AUTENTICADO` | 401 | Sin token, token inválido o vencido. | Enviar a inicio de sesión. |
| `CREDENCIALES_INVALIDAS` | 401 | Correo/CURP o contraseña incorrectos (CP-06), o contraseña actual incorrecta al cambiarla. | Mostrar «Credenciales inválidas». |
| `FIRMA_WEBHOOK_INVALIDA` | 401 | La firma del webhook no coincide. | (Pasarela) revisar el secreto compartido. |
| `ACCESO_DENEGADO` | 403 | Rol insuficiente o recurso ajeno. | Ocultar la acción. |
| `CUENTA_PENDIENTE` | 403 | La cuenta no terminó su verificación. | Llevar a la pantalla de código. |
| `CUENTA_BLOQUEADA` | 403 | Un administrador bloqueó la cuenta. | Informar y ofrecer contacto. |
| `NO_ENCONTRADO` | 404 | Recurso inexistente o no visible. | Mostrar «No encontrado». |
| `CORREO_REGISTRADO` | 409 | El correo ya tiene cuenta. | Ofrecer iniciar sesión. |
| `CURP_REGISTRADO` | 409 | El CURP ya tiene cuenta (CP-02). | Ofrecer iniciar sesión. |
| `AFORO_AGOTADO` | 409 | No quedan lugares suficientes (CP-11, CP-33, CP-MULTI-05). | Actualizar disponibilidad. |
| `ASIENTO_NO_DISPONIBLE` | 409 | Otro comprador tomó uno de los asientos (CP-36, CP-MULTI-08). | Recargar el mapa de asientos. |
| `CONFIRMACION_DUPLICADA` | 409 | Ya confirmó asistencia a ese evento gratuito (CP-34, CP-MULTI-09). | Mostrar su boleto. |
| `ESTADO_INVALIDO` | 409 | El evento no admite la operación en su estado (no está publicado, ya inició, ya se publicó). | Recargar el evento. |
| `POLITICA_MODIFICACION` | 409 | El cambio viola la política de RF-13: aforo o cantidad menor a lo vendido, precio de un tipo con ventas, modalidad de un evento publicado, evento ya iniciado (CP-15). | Explicar la política. |
| `EVENTO_NO_CANCELABLE` | 409 | El evento ya inició, se realizó o ya estaba cancelado. | Recargar el evento. |
| `ROL_EN_USO` | 409 | Quitar el rol de organizador con eventos vigentes, o si es su único rol. | Explicar la causa. |
| `CONFLICTO_DE_DATOS` | 409 | Una restricción de la base de datos rechazó la operación (red de seguridad). | Reintentar o recargar. |
| `REGLA_NEGOCIO` | 422 | Regla de negocio incumplida (ver `detail`). | Mostrar el mensaje. |
| `MOTIVO_REQUERIDO` | 422 | Un administrador cancela sin indicar el motivo. | Pedir el motivo. |
| `METODO_PAGO_NO_SOPORTADO` | 422 | OXXO, SPEI o método guardado (pendientes). | Ofrecer pago con tarjeta. |
| `DEMASIADOS_INTENTOS` | 429 | Sesión bloqueada temporalmente o código agotado. | Pedir que espere o solicite otro código. |
| `ERROR_INTERNO` | 500 | Error no controlado. | Mensaje genérico y reintento. |
| `SERVICIO_EXTERNO_NO_DISPONIBLE` | 503 | Reservado para fallas de servicios externos. | Reintentar más tarde. |

## Resultados que no son errores

- **Validación en puerta.** `POST /api/validaciones` responde **200** también cuando niega el acceso. El campo `motivo` explica la causa: `FORMATO_INVALIDO`, `BOLETO_INEXISTENTE`, `FIRMA_INVALIDA`, `OTRO_EVENTO`, `EVENTO_CANCELADO`, `YA_UTILIZADO`, `CANCELADO` o `EXPIRADO`.
- **Pago rechazado.** `POST /api/ordenes` responde **201** con `estadoOrden: CANCELADA` y `estadoPago: RECHAZADO`. La orden existe, pero no emitió boletos ni cobró.
