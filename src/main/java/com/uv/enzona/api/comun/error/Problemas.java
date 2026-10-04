package com.uv.enzona.api.comun.error;

import java.net.URI;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Fábrica de cuerpos de error en formato RFC 9457 con la propiedad {@code codigo}. */
public final class Problemas {

    private static final String BASE_TIPO = "https://enzona.mx/errores/";

    private Problemas() {
    }

    public static ProblemDetail de(ErrorCodigo codigo, String detalle) {
        return de(codigo.estadoHttp(), codigo.name(), detalle);
    }

    public static ProblemDetail de(HttpStatusCode estado, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(BASE_TIPO + codigo.toLowerCase().replace('_', '-')));
        problema.setProperty("codigo", codigo);
        problema.setProperty("timestamp", OffsetDateTime.now().toString());
        return problema;
    }
}
