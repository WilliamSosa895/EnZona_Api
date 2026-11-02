package com.uv.enzona.api.comun.error;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de errores de negocio. Cada código viaja en la propiedad {@code codigo} del cuerpo
 * de error (RFC 9457, application/problem+json) para que los clientes reaccionen sin depender
 * del texto del mensaje.
 */
public enum ErrorCodigo {

    // 400 — la solicitud está mal formada o no cumple las validaciones
    VALIDACION(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos"),
    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST, "La solicitud no es válida"),
    CODIGO_VERIFICACION_INVALIDO(HttpStatus.BAD_REQUEST, "El código de verificación no es válido"),
    CODIGO_VERIFICACION_EXPIRADO(HttpStatus.BAD_REQUEST, "El código de verificación expiró; solicita uno nuevo"),

    // 401 — no autenticado
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "Se requiere iniciar sesión"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
    FIRMA_WEBHOOK_INVALIDA(HttpStatus.UNAUTHORIZED, "La firma de la notificación no es válida"),

    // 403 — autenticado, pero sin permiso
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción"),
    CUENTA_PENDIENTE(HttpStatus.FORBIDDEN, "La cuenta aún no está verificada"),
    CUENTA_BLOQUEADA(HttpStatus.FORBIDDEN, "La cuenta está bloqueada"),

    // 404 — el recurso no existe o no es visible para quien lo pide
    NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El recurso no existe"),

    // 409 — la operación choca con el estado actual de los datos
    CORREO_REGISTRADO(HttpStatus.CONFLICT, "El correo ya está registrado"),
    CURP_REGISTRADO(HttpStatus.CONFLICT, "El CURP ya está registrado"),
    AFORO_AGOTADO(HttpStatus.CONFLICT, "No hay lugares suficientes para esta compra"),
    ASIENTO_NO_DISPONIBLE(HttpStatus.CONFLICT, "Uno o más asientos ya no están disponibles"),
    CONFIRMACION_DUPLICADA(HttpStatus.CONFLICT, "Ya confirmaste tu asistencia a este evento"),
    ESTADO_INVALIDO(HttpStatus.CONFLICT, "La operación no es posible en el estado actual"),
    POLITICA_MODIFICACION(HttpStatus.CONFLICT, "El cambio viola la política de modificación del evento"),
    EVENTO_NO_CANCELABLE(HttpStatus.CONFLICT, "El evento ya no puede cancelarse"),
    ROL_EN_USO(HttpStatus.CONFLICT, "No es posible quitar el rol mientras tenga eventos vigentes"),
    CONFLICTO_DE_DATOS(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes"),

    // 422 — la solicitud es correcta en forma, pero rompe una regla de negocio
    REGLA_NEGOCIO(HttpStatus.UNPROCESSABLE_ENTITY, "La solicitud no cumple una regla de negocio"),
    MOTIVO_REQUERIDO(HttpStatus.UNPROCESSABLE_ENTITY, "Se requiere el motivo de la cancelación"),
    METODO_PAGO_NO_SOPORTADO(HttpStatus.UNPROCESSABLE_ENTITY, "El método de pago aún no está disponible"),
    IMAGEN_NO_PERMITIDA(HttpStatus.UNPROCESSABLE_ENTITY, "La imagen no cumple las normas de la comunidad"),

    // 429 — demasiados intentos
    DEMASIADOS_INTENTOS(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos; espera e intenta de nuevo"),

    // 5xx
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado"),
    SERVICIO_EXTERNO_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE, "Un servicio externo no está disponible");

    private final HttpStatus estadoHttp;
    private final String mensajePorDefecto;

    ErrorCodigo(HttpStatus estadoHttp, String mensajePorDefecto) {
        this.estadoHttp = estadoHttp;
        this.mensajePorDefecto = mensajePorDefecto;
    }

    public HttpStatus estadoHttp() {
        return estadoHttp;
    }

    public String mensajePorDefecto() {
        return mensajePorDefecto;
    }
}
