# Ejemplos de solicitudes y respuestas

Valores ilustrativos. Los tokens, códigos y firmas reales son distintos en cada ejecución.

## 1. Registro, verificación y sesión

**Registro**

```http
POST /api/auth/registro
Content-Type: application/json

{
  "nombre": "Ana Pérez",
  "correo": "ana@correo.com",
  "telefono": null,
  "curp": "PERA000229MDFRRSA1",
  "contrasena": "Secreta123",
  "organizador": false,
  "aceptaAvisoPrivacidad": true
}
```

```json
HTTP/1.1 201 Created
{ "usuarioId": 12, "correo": "ana@correo.com", "estado": "PENDIENTE", "verificacionesPendientes": ["CORREO"] }
```

**Verificación**

```http
POST /api/auth/verificacion
Content-Type: application/json

{ "correo": "ana@correo.com", "canal": "CORREO", "codigo": "482913" }
```

```json
HTTP/1.1 200 OK
{ "estado": "ACTIVO", "correoVerificado": true, "telefonoVerificado": false, "verificacionesPendientes": [] }
```

**Sesión**

```http
POST /api/auth/sesion
Content-Type: application/json

{ "identificador": "PERA000229MDFRRSA1", "contrasena": "Secreta123" }
```

```json
HTTP/1.1 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlbnpvbmEtYXBpIiwic3ViIjoiMTIi...",
  "tipo": "Bearer",
  "expiraEnSegundos": 3600,
  "expiraEn": "2026-10-04T13:00:00-06:00",
  "usuario": { "id": 12, "nombre": "Ana Pérez", "correo": "ana@correo.com", "roles": ["ASISTENTE"] }
}
```

## 2. Crear un evento de pago con asientos (organizador)

```http
POST /api/eventos
Authorization: Bearer <token del organizador>
Content-Type: application/json

{
  "nombre": "Noche de jazz en el Foro",
  "descripcion": "Cuarteto local, entrada con asiento numerado.",
  "lugar": "Foro Cultural Orizaba",
  "direccion": "Sur 5 120, Centro",
  "ciudad": "Orizaba",
  "latitud": 18.850600,
  "longitud": -97.100300,
  "categoriaId": 1,
  "fechaHoraInicio": "2026-11-20T20:00:00-06:00",
  "fechaHoraFin": "2026-11-20T23:00:00-06:00",
  "aforo": 50,
  "esDePago": true,
  "requiereAsiento": true,
  "tiposBoleto": [
    { "nombre": "Platea", "precio": 180.00, "cantidad": 30 },
    { "nombre": "Preferente", "precio": 250.00, "cantidad": 20 }
  ],
  "secciones": [
    { "nombre": "Platea", "filas": 5, "asientosPorFila": 10 }
  ],
  "publicar": true
}
```

```json
HTTP/1.1 201 Created
Location: /api/eventos/7
{
  "id": 7, "organizadorId": 3, "nombre": "Noche de jazz en el Foro",
  "descripcion": "Cuarteto local, entrada con asiento numerado.",
  "lugar": "Foro Cultural Orizaba", "direccion": "Sur 5 120, Centro", "ciudad": "Orizaba",
  "latitud": 18.8506, "longitud": -97.1003, "categoriaId": 1, "categoria": "Música",
  "fechaHoraInicio": "2026-11-20T20:00:00-06:00", "fechaHoraFin": "2026-11-20T23:00:00-06:00",
  "esDePago": true, "requiereAsiento": true, "aforo": 50, "disponibles": 50, "precioDesde": "180.00",
  "estado": "PUBLICADO", "distanciaKm": null, "fechaCancelacion": null, "motivoCancelacion": null,
  "tiposBoleto": [
    { "id": 15, "nombre": "Platea", "precio": "180.00", "cantidadTotal": 30, "disponibles": 30 },
    { "id": 16, "nombre": "Preferente", "precio": "250.00", "cantidadTotal": 20, "disponibles": 20 }
  ]
}
```

## 3. Descubrir eventos cercanos (RF-19)

```http
GET /api/eventos?lat=18.8510&lng=-97.0990&radioKm=5&esDePago=true&page=0&size=10
```

```json
HTTP/1.1 200 OK
X-Total-Count: 1
X-Page: 0
X-Page-Size: 10
X-Total-Pages: 1

[
  {
    "id": 7, "nombre": "Noche de jazz en el Foro", "lugar": "Foro Cultural Orizaba", "ciudad": "Orizaba",
    "latitud": 18.8506, "longitud": -97.1003, "categoria": "Música",
    "fechaHoraInicio": "2026-11-20T20:00:00-06:00", "esDePago": true, "requiereAsiento": true,
    "aforo": 50, "disponibles": 50, "precioDesde": "180.00", "estado": "PUBLICADO",
    "distanciaKm": 0.14, "tiposBoleto": null
  }
]
```

Sin permiso de ubicación (RNF-11): `GET /api/eventos?ciudad=Orizaba`.

## 4. Comprar dos boletos con asiento

```http
POST /api/ordenes
Authorization: Bearer <token del asistente>
Idempotency-Key: 6b1f7c1e-3a9d-4c55-9a2f-1f0e2d6c9b10
Content-Type: application/json

{
  "eventoId": 7,
  "tipoBoletoId": 15,
  "cantidad": 2,
  "asientosIds": [101, 102],
  "metodoPagoId": null,
  "metodo": "TARJETA",
  "tokenPago": "tok_aprobado"
}
```

```json
HTTP/1.1 201 Created
Location: /api/ordenes/31
{
  "ordenId": 31, "eventoId": 7, "estadoOrden": "PAGADA", "estadoPago": "APROBADO",
  "total": "360.00", "moneda": "MXN", "referenciaPago": "sbx_ch_4f0c8e2b6a1d9e7f3c21",
  "expiraEn": null, "mensaje": "Compra confirmada",
  "boletos": [
    {
      "id": 88, "ordenId": 31, "eventoId": 7, "tipoBoletoId": 15, "asientoId": 101,
      "codigo": "EZq3Vt0bX9kLm2Pz", "firma": "Yp3c1M8...k2Q",
      "contenidoQr": "EZq3Vt0bX9kLm2Pz.Yp3c1M8...k2Q", "estado": "VALIDO",
      "fechaEmision": "2026-10-04T12:05:10-06:00", "fechaUso": null,
      "eventoNombre": "Noche de jazz en el Foro", "fechaHoraInicio": "2026-11-20T20:00:00-06:00",
      "lugar": "Foro Cultural Orizaba", "tipoNombre": "Platea", "asientoEtiqueta": "Platea · Fila A · Asiento 1"
    },
    {
      "id": 89, "ordenId": 31, "eventoId": 7, "tipoBoletoId": 15, "asientoId": 102,
      "codigo": "EZ7Hn2sQw4Lc8Rt1", "firma": "b9Qe0...Zx",
      "contenidoQr": "EZ7Hn2sQw4Lc8Rt1.b9Qe0...Zx", "estado": "VALIDO",
      "fechaEmision": "2026-10-04T12:05:10-06:00", "fechaUso": null,
      "eventoNombre": "Noche de jazz en el Foro", "fechaHoraInicio": "2026-11-20T20:00:00-06:00",
      "lugar": "Foro Cultural Orizaba", "tipoNombre": "Platea", "asientoEtiqueta": "Platea · Fila A · Asiento 2"
    }
  ]
}
```

**Reintento con la misma `Idempotency-Key`:** `HTTP/1.1 200 OK` con la misma orden 31, sin un segundo cobro.

**Pago rechazado** (`"tokenPago": "tok_rechazado"`):

```json
HTTP/1.1 201 Created
{
  "ordenId": 32, "eventoId": 7, "estadoOrden": "CANCELADA", "estadoPago": "RECHAZADO",
  "total": "180.00", "moneda": "MXN", "referenciaPago": "sbx_ch_91aa...", "expiraEn": null,
  "mensaje": "El pago no se aprobó; no se realizó ningún cargo", "boletos": []
}
```

**Asiento tomado por otra persona:**

```json
HTTP/1.1 409 Conflict
{ "status": 409, "codigo": "ASIENTO_NO_DISPONIBLE", "detail": "Uno o más asientos ya no están disponibles" }
```

## 5. Confirmar asistencia a un evento gratuito

```http
POST /api/ordenes
Authorization: Bearer <token>
Idempotency-Key: rsvp-2c8f1d0a
Content-Type: application/json

{ "eventoId": 9, "cantidad": 1, "asientosIds": [], "metodo": "GRATIS" }
```

```json
HTTP/1.1 201 Created
{ "ordenId": 40, "eventoId": 9, "estadoOrden": "PAGADA", "estadoPago": null, "total": "0.00",
  "moneda": "MXN", "mensaje": "Asistencia confirmada", "boletos": [ { "codigo": "EZ...", "contenidoQr": "EZ....", "estado": "VALIDO" } ] }
```

Una segunda confirmación de la misma persona responde `409 CONFIRMACION_DUPLICADA`.

## 6. Validar en la puerta

```http
POST /api/validaciones
Authorization: Bearer <token del validador>
Content-Type: application/json

{ "eventoId": 7, "qr": "EZq3Vt0bX9kLm2Pz.Yp3c1M8...k2Q" }
```

```json
HTTP/1.1 200 OK
{ "resultado": "PERMITIDO", "motivo": null, "mensaje": "Acceso permitido", "codigo": "EZq3Vt0bX9kLm2Pz",
  "tipoBoleto": "Platea", "asiento": "Platea · Fila A · Asiento 1", "fechaUso": "2026-11-20T19:42:10-06:00" }
```

Segundo escaneo del mismo QR:

```json
HTTP/1.1 200 OK
{ "resultado": "RECHAZADO", "motivo": "YA_UTILIZADO", "mensaje": "El boleto ya fue utilizado",
  "codigo": "EZq3Vt0bX9kLm2Pz", "tipoBoleto": "Platea", "asiento": "Platea · Fila A · Asiento 1",
  "fechaUso": "2026-11-20T19:42:10-06:00" }
```

## 7. Modo sin conexión

**Antes del evento**, con red, se descarga el manifiesto:

```http
GET /api/eventos/7/manifiesto
Authorization: Bearer <token del validador>
```

```json
{ "eventoId": 7, "evento": "Noche de jazz en el Foro", "generadoEn": "2026-11-20T18:00:00-06:00", "total": 2,
  "boletos": [
    { "codigo": "EZq3Vt0bX9kLm2Pz", "firma": "Yp3c1M8...k2Q", "estado": "VALIDO", "tipoBoleto": "Platea", "asiento": "Platea · Fila A · Asiento 1" },
    { "codigo": "EZ7Hn2sQw4Lc8Rt1", "firma": "b9Qe0...Zx", "estado": "VALIDO", "tipoBoleto": "Platea", "asiento": "Platea · Fila A · Asiento 2" }
  ] }
```

**Al recuperar la red**, se sube el lote de escaneos:

```http
POST /api/validaciones/sincronizar
Authorization: Bearer <token del validador>
Content-Type: application/json

{ "eventoId": 7, "registros": [
  { "qr": "EZ7Hn2sQw4Lc8Rt1.b9Qe0...Zx", "fechaHora": "2026-11-20T19:50:00-06:00" }
] }
```

```json
{ "procesados": 1, "permitidos": 1, "rechazados": 0, "conflictos": 0,
  "resultados": [ { "qr": "EZ7Hn2sQw4Lc8Rt1.b9Qe0...Zx", "fechaHora": "2026-11-20T19:50:00-06:00",
                    "resultado": "PERMITIDO", "motivo": null, "conflicto": false, "duplicado": false } ] }
```

## 8. Cancelar un evento

```http
POST /api/eventos/7/cancelacion
Authorization: Bearer <token del organizador>
Content-Type: application/json

{ "motivo": "Lluvia intensa en la zona", "aceptaComision": false }
```

```json
HTTP/1.1 200 OK
{ "eventoId": 7, "importeReembolsado": "360.00", "comision": "0.00", "boletosAnulados": 2,
  "asistentesNotificados": 1, "canceladoPor": 3, "fechaCancelacion": "2026-11-18T10:00:00-06:00" }
```

## 9. Panel del organizador

```http
GET /api/eventos/7/reportes
Authorization: Bearer <token del organizador>
```

```json
{ "eventoId": 7, "evento": "Noche de jazz en el Foro", "estado": "PUBLICADO", "aforo": 50, "disponibles": 48,
  "boletosEmitidos": 2, "accesosValidados": 1, "accesosRechazados": 1, "boletosCancelados": 0,
  "ingresos": "360.00", "reembolsado": "0.00", "ocupacion": 4.0,
  "porTipo": [
    { "tipoBoletoId": 15, "nombre": "Platea", "precio": "180.00", "cantidadTotal": 30, "disponibles": 28, "emitidos": 2, "ingresos": "360.00" },
    { "tipoBoletoId": 16, "nombre": "Preferente", "precio": "250.00", "cantidadTotal": 20, "disponibles": 20, "emitidos": 0, "ingresos": "0.00" }
  ] }
```

## 10. Webhook de la pasarela (cobro pendiente)

```http
POST /api/pagos/webhook
X-Pasarela-Firma: 5f2b0c... (HMAC-SHA256 en hexadecimal del cuerpo)
Content-Type: application/json

{ "claveIdempotencia": "orden-33", "referencia": "ch_123", "estado": "APROBADO" }
```

```json
{ "recibido": true }
```

Para firmar el cuerpo en pruebas:

```bash
echo -n '{"claveIdempotencia":"orden-33","referencia":"ch_123","estado":"APROBADO"}' \
  | openssl dgst -sha256 -hmac "$PAYMENT_WEBHOOK_SECRET" | awk '{print $2}'
```
