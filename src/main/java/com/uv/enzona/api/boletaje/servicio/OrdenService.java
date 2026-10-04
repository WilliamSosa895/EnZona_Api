package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.MetodoPago;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.pasarela.PasarelaNoDisponibleException;
import com.uv.enzona.api.boletaje.pasarela.PasarelaPago;
import com.uv.enzona.api.boletaje.pasarela.ResultadoCobro;
import com.uv.enzona.api.boletaje.pasarela.SolicitudCobro;
import com.uv.enzona.api.boletaje.web.dto.CrearOrdenSolicitud;
import com.uv.enzona.api.boletaje.web.dto.OrdenRespuesta;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.comun.util.Dinero;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Orquesta la compra o confirmación (CU-03, CU-04): reserva en una transacción, cobra fuera de
 * ella y confirma o libera en otra. No se mantienen bloqueos de base de datos durante la llamada
 * a la pasarela (Anexo E, riesgo E.13).
 */
@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);
    private static final Pattern CLAVE_VALIDA = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private final ReservaService reservas;
    private final ConfirmacionService confirmaciones;
    private final ConsultaBoletajeService consulta;
    private final PasarelaPago pasarela;

    public OrdenService(ReservaService reservas, ConfirmacionService confirmaciones, ConsultaBoletajeService consulta,
            PasarelaPago pasarela) {
        this.reservas = reservas;
        this.confirmaciones = confirmaciones;
        this.consulta = consulta;
        this.pasarela = pasarela;
    }

    public static String claveCobro(Long ordenId) {
        return "orden-" + ordenId;
    }

    public ResultadoOrden crear(Long asistenteId, String claveOperacion, CrearOrdenSolicitud solicitud) {
        validarClave(claveOperacion);
        validarMetodo(solicitud);

        // Idempotencia (CP-MULTI-07): la misma clave devuelve la misma orden sin volver a cobrar.
        Optional<OrdenRespuesta> previa = consulta.porClave(asistenteId, claveOperacion);
        if (previa.isPresent()) {
            return new ResultadoOrden(previa.get(), false);
        }

        Orden orden;
        try {
            orden = reservas.reservar(asistenteId, claveOperacion, solicitud);
        } catch (DataIntegrityViolationException e) {
            // Dos solicitudes simultáneas con la misma clave: gana la primera, la segunda la devuelve.
            return consulta.porClave(asistenteId, claveOperacion)
                    .map(existente -> new ResultadoOrden(existente, false))
                    .orElseThrow(() -> e);
        }

        if (orden.gratuita()) {
            confirmaciones.confirmarSinCobro(orden.getId());
        } else {
            cobrar(orden, solicitud.tokenPago());
        }
        return new ResultadoOrden(consulta.orden(orden.getId(), asistenteId, false), true);
    }

    /** Si la pasarela no responde, la orden queda pendiente y la tarea programada la resuelve. */
    private void cobrar(Orden orden, String tokenPago) {
        ResultadoCobro resultado;
        try {
            resultado = pasarela.cobrar(new SolicitudCobro(claveCobro(orden.getId()), orden.getTotal(), Dinero.MONEDA,
                    tokenPago, "EnZona · orden " + orden.getId()));
        } catch (PasarelaNoDisponibleException e) {
            log.warn("La pasarela no respondió para la orden {}; queda pendiente", orden.getId());
            return;
        }
        switch (resultado.estado()) {
            case APROBADO -> confirmaciones.confirmarPago(orden.getId(), resultado.referencia());
            case RECHAZADO -> confirmaciones.rechazarPago(orden.getId(), resultado.referencia());
            case PENDIENTE, DESCONOCIDO -> log.info("Cobro de la orden {} pendiente de confirmación", orden.getId());
        }
    }

    private static void validarClave(String clave) {
        if (clave == null || !CLAVE_VALIDA.matcher(clave).matches()) {
            throw new ApiException(ErrorCodigo.SOLICITUD_INVALIDA,
                    "La cabecera Idempotency-Key es obligatoria (8 a 64 caracteres: letras, números, - o _)");
        }
    }

    private static void validarMetodo(CrearOrdenSolicitud solicitud) {
        if (solicitud.metodo() == MetodoPago.OXXO || solicitud.metodo() == MetodoPago.SPEI) {
            throw new ApiException(ErrorCodigo.METODO_PAGO_NO_SOPORTADO,
                    "El pago en OXXO y por SPEI aún no está disponible");
        }
        if (solicitud.metodoPagoId() != null) {
            throw new ApiException(ErrorCodigo.METODO_PAGO_NO_SOPORTADO,
                    "Los métodos de pago guardados aún no están disponibles; envía tokenPago");
        }
        if (solicitud.metodo() == MetodoPago.TARJETA && (solicitud.tokenPago() == null || solicitud.tokenPago().isBlank())) {
            throw ApiException.reglaNegocio("El pago con tarjeta requiere el token de la pasarela (tokenPago)");
        }
    }
}
