package com.uv.enzona.api.comun.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.comun.error.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Respuestas 401 y 403 de la cadena de seguridad con el mismo formato problem+json del resto de la API. */
@Component
public class ProblemaSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper json;

    public ProblemaSeguridadHandler(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException excepcion) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escribir(response, Problemas.de(ErrorCodigo.NO_AUTENTICADO,
                "Se requiere un token de sesión válido y vigente"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException excepcion) throws IOException {
        escribir(response, Problemas.de(ErrorCodigo.ACCESO_DENEGADO, ErrorCodigo.ACCESO_DENEGADO.mensajePorDefecto()));
    }

    private void escribir(HttpServletResponse response, ProblemDetail problema) throws IOException {
        response.setStatus(problema.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), problema);
    }
}
