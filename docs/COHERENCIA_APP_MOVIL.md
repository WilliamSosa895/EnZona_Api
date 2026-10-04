# Coherencia con la app Android

Se revisó el repositorio [EnZona-android](https://github.com/zywoxxx/EnZona-android), versión v4: `ApiService.kt`, `Models.kt`, las notas v2 a v4 y sus migraciones propuestas. También se revisó el Anexo E de arquitectura, que define el contrato REST del proyecto.

## Alineado

| Tema | App Android | API |
|---|---|---|
| Prefijo y recursos | `api/eventos`, `api/ordenes`, `api/boletos` | Mismas rutas (Anexo E, tabla E.4) |
| Lista de eventos | `List<EventoDto>` | Arreglo JSON; la paginación va en cabeceras, sin envolver la lista |
| Campos del evento | `ciudad`, `latitud`, `longitud`, `categoria`, `disponibles`, `esDePago`, `requiereAsiento` | Mismos nombres, más `categoriaId`, `precioDesde`, `estado`, `distanciaKm` y `tiposBoleto` |
| Fechas | ISO-8601 con zona (`FechaEvento.iso`) | ISO-8601 con el desfase de la Ciudad de México |
| Importes | Texto decimal exacto (`"360.00"`) | Igual |
| Compra múltiple | `POST api/ordenes` con `Idempotency-Key`, `CrearOrdenDto` y respuesta `OrdenCreadaDto` | Mismo contrato: todo o nada, N boletos con QR propio, idempotencia |
| Contenido del QR | `"codigo.qr_firma"` | `contenidoQr = codigo.firma`, firmado con HMAC-SHA256 |
| Roles | Acumulables; `POST/DELETE api/usuarios/me/roles/organizador` | Igual, con 409 si tiene eventos vigentes |
| Cancelación | `GET politica-cancelacion` y `POST cancelacion` con el mismo formato de respuesta | Igual (ver diferencias de la comisión) |
| Coordenadas | Ambas o ninguna; obligatorias para publicar | Igual (CHECK en la base de datos y regla al publicar) |
| Estados (enums) | `EstadoEvento`, `EstadoOrden`, `EstadoBoleto`, etc. | Mismos valores |

## Cambios que la app debe hacer al conectarse

1. **Aviso de privacidad.** El registro exige `"aceptaAvisoPrivacidad": true` (LFPDPPP, RNF-03). La app debe mostrar el aviso y enviar ese campo.
2. **Pago con tarjeta.** Debe enviar `tokenPago`, el token que genera el SDK de la pasarela en el teléfono. La API nunca recibe datos de tarjeta. En sandbox: `tok_aprobado`, `tok_rechazado`, `tok_pendiente`.
3. **Validación sin conexión con manifiesto.** El validador descarga `GET /api/eventos/{id}/manifiesto` y compara el QR leído (código y firma) contra esa lista. **La clave HMAC no debe estar en el teléfono**: si se extrajera del APK, cualquiera podría fabricar boletos (Anexo E, riesgo E.13). Al recuperar la red, sube el lote con `POST /api/validaciones/sincronizar`.
4. **Validadores por evento.** El organizador designa a su personal con `POST /api/eventos/{id}/validadores`. Un validador solo puede validar los eventos donde fue designado.
5. **Sesión.** El endpoint es `POST /api/auth/sesion`, con `identificador` (correo o CURP) y `contrasena`. Todas las rutas protegidas exigen `Authorization: Bearer`.
6. **Webhook.** Ruta `/api/pagos/webhook` (Anexo E). La usa la pasarela, no la app.

## Pendientes: la app tiene funciones fuera de los requisitos y del alcance

Se dejaron fuera de la API y quedan para decidirse después. Para incorporarlas hay que ampliar primero los requisitos.

| Función en la app | Situación | Qué haría falta |
|---|---|---|
| **Métodos de pago guardados** (pantalla «Métodos de pago», tabla `metodo_pago`) | No está en los requisitos. `metodoPagoId` responde 422 | Requisito nuevo, tabla `metodo_pago` y tokenización persistente con la pasarela elegida |
| **Pago en OXXO y SPEI** | No está en los requisitos. Responde 422 `METODO_PAGO_NO_SOPORTADO` | Requisito nuevo, pagos diferidos (ampliar `estado_pago` con PENDIENTE) y referencias de pago |
| **Comisión del 10 % al organizador al cancelar un evento con ventas** (`cargo_organizador`) | No está en los requisitos. La API reembolsa el 100 % y devuelve `comision: "0.00"`. El tipo `DE_PAGO_CON_COMISION` de la app equivale a `DE_PAGO_CON_VENTAS` | Regla de negocio aprobada, tabla de cargos y su cobro |
| **Centro de notificaciones dentro de la app y recordatorios** (`notificacion_asistente`, `tipo_notificacion`) | Los requisitos solo piden avisos por correo, y la API los envía (boletos, cancelación, cambio de fecha o lugar). Las rutas `api/usuarios/me/notificaciones` no existen | Requisito nuevo, tabla de notificaciones y tarea diaria de recordatorios |
| **Gestión de categorías por el administrador** | El Documento de Visión la menciona, pero no hay requisito (pendiente documental del Anexo E) | Requisito y endpoints CRUD de categorías |

## Otras diferencias menores

- La app define `Asiento.numero` como `Int`. En la base de datos y en la API es texto (`"5"`), según el esquema v2.
- La app calcula `disponibles = aforo − vendidos`. La API devuelve la suma de los lugares disponibles de los tipos de boleto, que es la cantidad que realmente puede venderse.
- Envío del código por SMS: la API genera y comprueba el código del teléfono, pero falta contratar un proveedor de SMS. En modo `smtp` solo se registra una advertencia.
