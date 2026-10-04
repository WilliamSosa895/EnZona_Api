package com.uv.enzona.api.comun.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce todas las excepciones a respuestas application/problem+json coherentes. Los
 * mensajes de errores internos nunca se exponen al cliente: solo se registran en la bitácora.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> manejarApi(ApiException ex) {
        ProblemDetail problema = Problemas.de(ex.getCodigo(), ex.getMessage());
        return ResponseEntity.status(ex.getCodigo().estadoHttp()).body(problema);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> manejarRestricciones(ConstraintViolationException ex) {
        List<Map<String, String>> errores = ex.getConstraintViolations().stream()
                .map(v -> Map.of("campo", v.getPropertyPath().toString(), "mensaje", v.getMessage()))
                .toList();
        ProblemDetail problema = Problemas.de(ErrorCodigo.VALIDACION, ErrorCodigo.VALIDACION.mensajePorDefecto());
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> manejarTipo(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problema = Problemas.de(ErrorCodigo.SOLICITUD_INVALIDA,
                "El parámetro '" + ex.getName() + "' tiene un valor no válido");
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Problemas.de(ErrorCodigo.ACCESO_DENEGADO, ErrorCodigo.ACCESO_DENEGADO.mensajePorDefecto()));
    }

    /** Red de seguridad: una restricción de la base de datos rechazó la operación. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Problemas.de(ErrorCodigo.CONFLICTO_DE_DATOS, ErrorCodigo.CONFLICTO_DE_DATOS.mensajePorDefecto()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Problemas.de(ErrorCodigo.ERROR_INTERNO, ErrorCodigo.ERROR_INTERNO.mensajePorDefecto()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(),
                        "mensaje", e.getDefaultMessage() == null ? "valor no válido" : e.getDefaultMessage()))
                .toList();
        List<Map<String, String>> globales = ex.getBindingResult().getGlobalErrors().stream()
                .map(e -> Map.of("campo", e.getObjectName(),
                        "mensaje", e.getDefaultMessage() == null ? "valor no válido" : e.getDefaultMessage()))
                .toList();
        ProblemDetail problema = Problemas.de(ErrorCodigo.VALIDACION, ErrorCodigo.VALIDACION.mensajePorDefecto());
        problema.setProperty("errores", errores.isEmpty() ? globales : errores);
        return ResponseEntity.badRequest().body(problema);
    }

    /** Agrega la propiedad {@code codigo} a los errores propios de Spring MVC (405, 415, etc.). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problema && problema.getProperties() == null) {
            problema.setProperty("codigo", statusCode.value() >= 500 ? "ERROR_INTERNO" : "SOLICITUD_INVALIDA");
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
