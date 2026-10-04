package com.uv.enzona.api.comun.error;

/**
 * Excepción de negocio con un código del catálogo. El manejador global la traduce al
 * código HTTP correspondiente; los servicios nunca construyen respuestas HTTP.
 */
public class ApiException extends RuntimeException {

    private final ErrorCodigo codigo;

    public ApiException(ErrorCodigo codigo) {
        this(codigo, codigo.mensajePorDefecto());
    }

    public ApiException(ErrorCodigo codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public ErrorCodigo getCodigo() {
        return codigo;
    }

    public static ApiException noEncontrado(String recurso, Object id) {
        return new ApiException(ErrorCodigo.NO_ENCONTRADO, recurso + " " + id + " no existe");
    }

    public static ApiException reglaNegocio(String mensaje) {
        return new ApiException(ErrorCodigo.REGLA_NEGOCIO, mensaje);
    }

    public static ApiException accesoDenegado() {
        return new ApiException(ErrorCodigo.ACCESO_DENEGADO);
    }
}
