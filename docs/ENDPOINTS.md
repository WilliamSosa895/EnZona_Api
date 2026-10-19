# Endpoints

- **URL base:** `http://localhost:8080` en desarrollo.
- **Formato:** JSON en UTF-8.
- **Fechas:** ISO-8601 con desfase, expresadas en la hora del centro de México (`2026-11-20T19:00:00-06:00`).
- **Importes:** texto con dos decimales (`"360.00"`, MXN).
- **Errores:** ver [CODIGOS_DE_ERROR.md](CODIGOS_DE_ERROR.md).
- **Ejemplos completos:** ver [EJEMPLOS.md](EJEMPLOS.md).

**Autenticación:** 🔓 público · 🔐 requiere `Authorization: Bearer <token>`. Los roles se indican entre corchetes.

**Listas paginadas:** devuelven un arreglo JSON. Los datos de paginación van en las cabeceras `X-Total-Count`, `X-Page`, `X-Page-Size` y `X-Total-Pages`. Parámetros `page` (desde 0, por defecto 0) y `size` (por defecto 20, máximo 100).

---

## 1. Autenticación (`/api/auth`)

### POST `/api/auth/registro` 🔓
Crea la cuenta en estado `PENDIENTE` y envía los códigos de verificación (RF-01).

| Campo | Tipo | Reglas |
|---|---|---|
| `nombre` | string | obligatorio, ≤ 120 |
| `correo` | string | obligatorio, correo válido, ≤ 180 |
| `telefono` | string | opcional, 10 a 15 dígitos, `+` opcional. Si se envía, también se verifica |
| `curp` | string | obligatorio, formato oficial (18 caracteres) |
| `contrasena` | string | obligatorio, 8 a 72 caracteres, con letras y números |
| `organizador` | boolean | opcional; `true` activa también el rol ORGANIZADOR |
| `aceptaAvisoPrivacidad` | boolean | obligatorio, debe ser `true` (LFPDPPP) |

- **201:** `{ usuarioId, correo, estado, verificacionesPendientes[] }`
- **Errores:** 400 `VALIDACION` · 409 `CORREO_REGISTRADO` · 409 `CURP_REGISTRADO`

### POST `/api/auth/verificacion` 🔓
Confirma un código de 6 dígitos (RF-02). Cuerpo: `correo`, `canal` (`CORREO` por defecto o `TELEFONO`), `codigo`.

- **200:** `{ estado, correoVerificado, telefonoVerificado, verificacionesPendientes[] }`. La cuenta pasa a `ACTIVO` al completar las verificaciones.
- **Errores:** 400 `CODIGO_VERIFICACION_INVALIDO` · 400 `CODIGO_VERIFICACION_EXPIRADO` · 429 `DEMASIADOS_INTENTOS`

### POST `/api/auth/verificacion/reenvio` 🔓
Cuerpo: `correo`, `canal`. Envía un código nuevo e invalida el anterior (mínimo 60 s entre envíos).

- **202** siempre, exista o no el correo.

### POST `/api/auth/sesion` 🔓
Cuerpo: `identificador` (correo o CURP) y `contrasena` (RF-03).

- **200:** `{ token, tipo, expiraEnSegundos, expiraEn, usuario{ id, nombre, correo, roles[] } }`
- **Errores:** 401 `CREDENCIALES_INVALIDAS` · 403 `CUENTA_PENDIENTE` · 403 `CUENTA_BLOQUEADA` · 429 `DEMASIADOS_INTENTOS`

## 2. Perfil (`/api/usuarios/me`) 🔐

| Método y ruta | Descripción | Respuesta | Errores |
|---|---|---|---|
| GET `/api/usuarios/me` | Perfil propio (RF-04). Nunca incluye el CURP. | 200 `{ id, nombre, correo, telefono, correoVerificado, telefonoVerificado, estado, roles[], fechaRegistro }` | 401 |
| PUT `/api/usuarios/me` | Cuerpo `{ nombre, telefono }`. Un teléfono nuevo queda sin verificar y recibe un código. | 200 perfil | 400, 401 |
| PUT `/api/usuarios/me/contrasena` | Cuerpo `{ actual, nueva }`. | 204 | 400, 401 `CREDENCIALES_INVALIDAS` |
| POST `/api/usuarios/me/roles/organizador` | Activa el rol ORGANIZADOR. | 200 `{ id, nombre, correo, roles[] }` | 401 |
| DELETE `/api/usuarios/me/roles/organizador` | Quita el rol ORGANIZADOR. | 200 `{ id, nombre, correo, roles[] }` | 409 `ROL_EN_USO` |

## 3. Eventos

### GET `/api/eventos` 🔓
Eventos publicados que no han terminado (RF-05, RF-06, RF-07, RF-19). Paginado.

| Parámetro | Tipo | Descripción |
|---|---|---|
| `q` | string | Texto en nombre, descripción o lugar |
| `categoriaId` | int | Categoría |
| `ciudad` | string | Ciudad, alternativa sin ubicación (RNF-11) |
| `desde`, `hasta` | fecha ISO | Rango de la fecha de inicio |
| `esDePago` | boolean | `true` de pago, `false` gratuitos |
| `lat`, `lng` | double | Ubicación del usuario: ordena por cercanía y agrega `distanciaKm` |
| `radioKm` | double | Radio de búsqueda (por defecto 15, máximo `EVENT_MAX_RADIUS_KM`) |
| `page`, `size` | int | Paginación |

- **Orden:** sin ubicación, por fecha de inicio. Con ubicación, por distancia y luego por fecha.
- **200:** `EventoRespuesta[]`
- **Errores:** 400 (parámetros) · 422 (`lat` sin `lng`)

### GET `/api/eventos/{id}` 🔓
Detalle con `tiposBoleto[]` y disponibilidad.

- **200:** `EventoRespuesta`
- **Errores:** 404. Un borrador solo lo ve su organizador o un administrador.

### POST `/api/eventos` 🔐 [ORGANIZADOR]
Crea un evento en borrador o publicado (RF-08). Cuerpo `EventoSolicitud`:

| Campo | Reglas |
|---|---|
| `nombre`, `lugar` | obligatorios (≤ 160) |
| `descripcion` | ≤ 5000 |
| `direccion` | ≤ 240 |
| `ciudad` | por defecto `Orizaba` |
| `latitud`, `longitud` | juntas o ninguna. Obligatorias para publicar |
| `categoriaId` | debe existir |
| `fechaHoraInicio` | obligatoria y futura |
| `fechaHoraFin` | opcional, no anterior al inicio |
| `aforo` | 1 a 100 000 |
| `esDePago` | gratuito: sin `tiposBoleto`, se crea el tipo «General» a precio 0 con el aforo. De pago: `tiposBoleto[]` obligatorio, cada uno `{ nombre, precio > 0, cantidad }`, y la suma de cantidades no puede superar el aforo |
| `requiereAsiento` | `secciones[]` obligatorio `{ nombre, filas ≤ 26, asientosPorFila ≤ 200 }`; el total de asientos debe ser igual al aforo |
| `publicar` | `true` lo publica de inmediato |

- **201:** `EventoRespuesta`, con cabecera `Location`
- **Errores:** 400 · 403 · 422 `REGLA_NEGOCIO`

### PUT `/api/eventos/{id}` 🔐 [ORGANIZADOR dueño]
Modifica el evento con la política de RF-13. El cuerpo es igual al de la creación.

- **Tipos de boleto:** se envían con `id` para modificarlos o sin `id` para agregar uno nuevo. Un tipo omitido se elimina, solo si no tiene ventas.
- **Secciones:** en un evento publicado son opcionales (`null` = sin cambios).
- **Política de modificación:**
  - No se modifica un evento cancelado, realizado o ya iniciado.
  - El aforo y las cantidades no bajan de lo vendido o reservado.
  - En un evento publicado no cambia la modalidad (de pago o con asiento).
  - El precio de un tipo con ventas no cambia.
  - El mapa de asientos solo cambia si ninguno está vendido o reservado.
- **Avisos:** si cambian la fecha, la hora o el lugar de un evento publicado, se avisa por correo a quienes tienen boleto.
- **200:** `EventoRespuesta`
- **Errores:** 403 · 404 · 409 `ESTADO_INVALIDO` / `POLITICA_MODIFICACION` · 422

### POST `/api/eventos/{id}/publicacion` 🔐 [ORGANIZADOR dueño]
Publica un borrador. Exige coordenadas y fecha futura.

- **200:** `EventoRespuesta`
- **Errores:** 409 `ESTADO_INVALIDO` · 422

### POST `/api/eventos/{id}/imagen` 🔐 [ORGANIZADOR dueño o ADMIN]
Sube o reemplaza la portada del evento. Cuerpo `multipart/form-data` con el campo `imagen` (JPEG, PNG o WebP, hasta `IMAGE_MAX_MB`). La API valida el tipo por su contenido, guarda el archivo en el almacén configurado (`IMAGE_PROVIDER=local` o `cloudinary`) y borra la portada anterior. Antes de guardarla la pasa por el servicio de moderación configurado (`IMAGE_MODERATION`): una imagen rechazada nunca se almacena ni se publica. El cliente nunca recibe credenciales del almacén ni del moderador.

- **200:** `EventoRespuesta` con `imagenUrl` (absoluta en Cloudinary; relativa `/api/imagenes/…` con el almacén local)
- **Errores:** 403 · 404 · 409 `ESTADO_INVALIDO` (evento cancelado o realizado) · 422 `REGLA_NEGOCIO` (tipo o tamaño) · 422 `IMAGEN_NO_PERMITIDA` (desnudez, contenido sexual, violencia gráfica o autolesiones según `IMAGE_MODERATION`) · 503 `SERVICIO_EXTERNO_NO_DISPONIBLE`

### DELETE `/api/eventos/{id}/imagen` 🔐 [ORGANIZADOR dueño o ADMIN]
Quita la portada. **200:** `EventoRespuesta` con `imagenUrl: null`.

### GET `/api/imagenes/{archivo}` 🔓
Solo con `IMAGE_PROVIDER=local`: sirve la portada guardada en el servidor (caché de 7 días).

### GET `/api/eventos/{id}/asientos` 🔐
Mapa de asientos (RF-09): `[{ id, seccion, fila, numero, estado, etiqueta }]`, con `estado` DISPONIBLE, RESERVADO u OCUPADO.

### Validadores del evento 🔐 [ORGANIZADOR dueño o ADMIN]

| Método y ruta | Descripción | Respuesta |
|---|---|---|
| GET `/api/eventos/{id}/validadores` | Personal de acceso designado | 200 `[{ usuarioId, nombre, correo }]` |
| POST `/api/eventos/{id}/validadores` | Cuerpo `{ correo }` de una cuenta activa; le agrega el rol VALIDADOR | 201; errores 404 si no existe la cuenta, 422 si no está activa |
| DELETE `/api/eventos/{id}/validadores/{usuarioId}` | Retira la designación | 204 |

### Cancelación 🔐 [ORGANIZADOR dueño o ADMIN] (RF-14)

**GET `/api/eventos/{id}/politica-cancelacion`**

- **200:** `{ permitida, tipo, importeCobrado, comision, reembolso, boletosAfectados, motivoBloqueo }`
- `tipo`: `BORRADOR`, `GRATUITO`, `DE_PAGO_SIN_VENTAS`, `DE_PAGO_CON_VENTAS` o `ADMINISTRATIVA`
- `comision` siempre vale `"0.00"` (comisión pendiente; ver COHERENCIA_APP_MOVIL)

**POST `/api/eventos/{id}/cancelacion`**

Cuerpo opcional `{ motivo, aceptaComision }`. El motivo es obligatorio si cancela un administrador sobre un evento ajeno. Al cancelar:
- Se anulan los boletos.
- Se cancelan las órdenes pendientes y gratuitas.
- Se reembolsa el 100 % de las pagadas, de forma asíncrona.
- Se avisa por correo a los asistentes.

Respuesta y errores:
- **200:** `{ eventoId, importeReembolsado, comision, boletosAnulados, asistentesNotificados, canceladoPor, fechaCancelacion }`
- **Errores:** 403 · 409 `EVENTO_NO_CANCELABLE` · 422 `MOTIVO_REQUERIDO`

### GET `/api/eventos/{id}/reportes` 🔐 [ORGANIZADOR dueño o ADMIN]
Panel del organizador (RF-17).

- **200:** `{ eventoId, evento, estado, aforo, disponibles, boletosEmitidos, accesosValidados, accesosRechazados, boletosCancelados, ingresos, reembolsado, ocupacion, porTipo[] }`

### GET `/api/eventos/{id}/manifiesto` 🔐 [VALIDADOR designado, ORGANIZADOR dueño o ADMIN]
Lista de boletos para validar sin conexión (RNF-08).

- **200:** `{ eventoId, evento, generadoEn, total, boletos[{ codigo, firma, estado, tipoBoleto, asiento }] }`, con boletos en estado VALIDO y USADO.

### GET `/api/categorias` 🔓
`[{ id, nombre }]`

### GET `/api/organizador/eventos` 🔐 [ORGANIZADOR]
Todos mis eventos, en cualquier estado.

## 4. Órdenes y boletos

### POST `/api/ordenes` 🔐
Compra o confirma N boletos en una sola orden, todo o nada (RF-10, RF-11, RF-12, RF-16).

**Cabecera obligatoria:** `Idempotency-Key`, de 8 a 64 caracteres (`A-Z a-z 0-9 - _`). Repetir la misma clave devuelve la misma orden **sin volver a cobrar**.

| Campo | Reglas |
|---|---|
| `eventoId` | obligatorio; el evento debe estar publicado y no haber iniciado |
| `tipoBoletoId` | opcional si el evento tiene un solo tipo |
| `cantidad` | 1 a `ORDER_MAX_TICKETS`. En eventos gratuitos, 1 (una confirmación por persona) |
| `asientosIds[]` | un asiento distinto por boleto si el evento lo requiere; vacío si no |
| `metodo` | `GRATIS` (gratuitos) o `TARJETA` (de pago). `OXXO` y `SPEI` responden 422 (pendientes) |
| `tokenPago` | token de la pasarela, obligatorio con `TARJETA`. En sandbox: `tok_aprobado`, `tok_rechazado`, `tok_pendiente`, `tok_error` |
| `metodoPagoId` | métodos guardados: pendiente; si se envía responde 422 |

Reglas de cobro:
- El precio y el total se calculan en el servidor a partir del tipo de boleto (RNF-09).
- La reserva de lugares y asientos dura `ORDER_RESERVATION_MINUTES`; si el pago no se confirma en ese tiempo, se libera.

| Respuesta | Cuándo |
|---|---|
| **201** `OrdenRespuesta` | Orden nueva: `PAGADA` con boletos, `CANCELADA` con pago `RECHAZADO`, o `PENDIENTE` esperando a la pasarela |
| **200** `OrdenRespuesta` | Reintento con la misma `Idempotency-Key` |
| 400 | Falta la clave o datos inválidos |
| 404 | El evento no existe |
| 409 | `AFORO_AGOTADO`, `ASIENTO_NO_DISPONIBLE`, `CONFIRMACION_DUPLICADA`, `ESTADO_INVALIDO` |
| 422 | `REGLA_NEGOCIO`, `METODO_PAGO_NO_SOPORTADO` |

`OrdenRespuesta`: `{ ordenId, eventoId, estadoOrden, estadoPago, total, moneda, referenciaPago, expiraEn, mensaje, boletos[] }`

### GET `/api/ordenes/{id}` 🔐
Orden propia (o cualquiera, si eres administrador) con sus boletos. Una orden ajena responde 404.

### GET `/api/boletos` 🔐
Mis boletos, del más reciente al más antiguo:

`[{ id, ordenId, eventoId, tipoBoletoId, asientoId, codigo, firma, contenidoQr, estado, fechaEmision, fechaUso, eventoNombre, fechaHoraInicio, lugar, tipoNombre, asientoEtiqueta }]`

`contenidoQr` (`codigo.firma`) es el texto que se dibuja en el QR.

### GET `/api/boletos/{id}` 🔐
Un boleto propio. Un boleto ajeno responde 404.

### POST `/api/pagos/webhook` 🔓 (firmado)
Notificación de la pasarela para cobros pendientes.

- **Cabecera:** `X-Pasarela-Firma`, el HMAC-SHA256 en hexadecimal del cuerpo con `PAYMENT_WEBHOOK_SECRET`.
- **Cuerpo:** `{ claveIdempotencia: "orden-{id}", referencia, estado: APROBADO | RECHAZADO | PENDIENTE }`
- **200:** `{ "recibido": true }`. Es idempotente.
- **Errores:** 400 · 401 `FIRMA_WEBHOOK_INVALIDA`

## 5. Validación de acceso 🔐 [VALIDADOR designado, ORGANIZADOR dueño o ADMIN]

### POST `/api/validaciones`
Cuerpo: `{ eventoId, qr }`, donde `qr` es el texto leído, `codigo.firma`.

- Comprueba la firma, el evento y el estado del boleto, y lo marca USADO de forma atómica (RF-15).
- **200:** `{ resultado: PERMITIDO | RECHAZADO, motivo, mensaje, codigo, tipoBoleto, asiento, fechaUso }`
- `motivo` puede ser `FORMATO_INVALIDO`, `BOLETO_INEXISTENTE`, `FIRMA_INVALIDA`, `OTRO_EVENTO`, `EVENTO_CANCELADO`, `YA_UTILIZADO`, `CANCELADO` o `EXPIRADO`.

### POST `/api/validaciones/sincronizar`
Cuerpo: `{ eventoId, registros[{ qr, fechaHora }] }`, hasta 2000 escaneos hechos sin conexión.

- Se procesan del más antiguo al más reciente. Si dos dispositivos admitieron el mismo boleto, prevalece el primero y el segundo queda como conflicto.
- Reenviar el mismo lote no duplica registros.
- **200:** `{ procesados, permitidos, rechazados, conflictos, resultados[{ qr, fechaHora, resultado, motivo, conflicto, duplicado }] }`

## 6. Administración 🔐 [ADMIN] (RF-18)

| Método y ruta | Descripción |
|---|---|
| GET `/api/admin/usuarios?estado=&q=&page=&size=` | Cuentas, filtradas por estado o por texto en nombre o correo. Paginado: `[{ id, nombre, correo, telefono, estado, correoVerificado, roles[], fechaRegistro }]` |
| PATCH `/api/admin/usuarios/{id}/estado` | Cuerpo `{ estado: ACTIVO | BLOQUEADO }`. El bloqueo aplica de inmediato, incluso con tokens vigentes. No se puede cambiar la propia cuenta (422) |
| GET `/api/admin/eventos?estado=&page=&size=` | Eventos en cualquier estado. Paginado |

## 7. Operación

| Ruta | Descripción |
|---|---|
| GET `/actuator/health` 🔓 | Estado de la API y de la base de datos |
| GET `/swagger-ui.html` 🔓 | Exploración interactiva (en `prod`, solo con `SWAGGER_ENABLED=true`) |
| GET `/v3/api-docs` 🔓 | Especificación OpenAPI generada |
