package com.uv.enzona.api.boletaje.servicio;

import com.uv.enzona.api.boletaje.modelo.Boleto;
import com.uv.enzona.api.boletaje.modelo.Orden;
import com.uv.enzona.api.boletaje.modelo.Pago;
import com.uv.enzona.api.boletaje.repositorio.BoletoRepository;
import com.uv.enzona.api.boletaje.repositorio.OrdenRepository;
import com.uv.enzona.api.boletaje.repositorio.PagoRepository;
import com.uv.enzona.api.boletaje.web.dto.BoletoRespuesta;
import com.uv.enzona.api.boletaje.web.dto.OrdenRespuesta;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.util.Dinero;
import com.uv.enzona.api.comun.util.Fechas;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura de órdenes y boletos del asistente (CU-03, CU-04). */
@Service
public class ConsultaBoletajeService {

    private final OrdenRepository ordenes;
    private final PagoRepository pagos;
    private final BoletoRepository boletos;
    private final BoletoMapper mapper;

    public ConsultaBoletajeService(OrdenRepository ordenes, PagoRepository pagos, BoletoRepository boletos,
            BoletoMapper mapper) {
        this.ordenes = ordenes;
        this.pagos = pagos;
        this.boletos = boletos;
        this.mapper = mapper;
    }

    /** Las órdenes ajenas responden 404 para no revelar su existencia. */
    @Transactional(readOnly = true)
    public OrdenRespuesta orden(Long ordenId, Long solicitanteId, boolean esAdmin) {
        Orden orden = ordenes.findById(ordenId).orElseThrow(() -> ApiException.noEncontrado("Orden", ordenId));
        if (!orden.perteneceA(solicitanteId) && !esAdmin) {
            throw ApiException.noEncontrado("Orden", ordenId);
        }
        return respuesta(orden);
    }

    @Transactional(readOnly = true)
    public Optional<OrdenRespuesta> porClave(Long asistenteId, String clave) {
        return ordenes.findByAsistenteIdAndClaveOperacion(asistenteId, clave).map(this::respuesta);
    }

    @Transactional(readOnly = true)
    public List<BoletoRespuesta> boletosDe(Long asistenteId) {
        return mapper.respuestas(boletos.delAsistente(asistenteId));
    }

    @Transactional(readOnly = true)
    public BoletoRespuesta boleto(Long boletoId, Long solicitanteId) {
        Boleto boleto = boletos.findById(boletoId).orElseThrow(() -> ApiException.noEncontrado("Boleto", boletoId));
        boolean propio = ordenes.findById(boleto.getOrdenId()).map(orden -> orden.perteneceA(solicitanteId)).orElse(false);
        if (!propio) {
            throw ApiException.noEncontrado("Boleto", boletoId);
        }
        return mapper.respuestas(List.of(boleto)).get(0);
    }

    private OrdenRespuesta respuesta(Orden orden) {
        Optional<Pago> pago = pagos.findFirstByOrdenIdOrderByFechaDesc(orden.getId());
        List<BoletoRespuesta> emitidos = mapper.respuestas(boletos.findByOrdenIdOrderByIdAsc(orden.getId()));
        return new OrdenRespuesta(
                orden.getId(),
                orden.getEventoId(),
                orden.getEstado(),
                pago.map(Pago::getEstado).orElse(null),
                Dinero.texto(orden.getTotal()),
                Dinero.MONEDA,
                pago.map(Pago::getReferenciaPasarela).orElse(null),
                Fechas.enZonaMx(orden.getExpiraEn()),
                mensaje(orden, pago),
                emitidos);
    }

    private static String mensaje(Orden orden, Optional<Pago> pago) {
        return switch (orden.getEstado()) {
            case PAGADA -> orden.gratuita() ? "Asistencia confirmada" : "Compra confirmada";
            case PENDIENTE -> "Esperando la confirmación del pago; tus lugares están reservados";
            case CANCELADA -> pago.map(Pago::getEstado).map(estado -> switch (estado) {
                case RECHAZADO -> "El pago no se aprobó; no se realizó ningún cargo";
                case APROBADO -> "La reserva venció antes de confirmar el pago; el cargo se reembolsará";
                case REEMBOLSADO -> "La reserva venció antes de confirmar el pago; el cargo fue reembolsado";
            }).orElse("Orden cancelada");
            case REEMBOLSADA -> "Orden reembolsada";
        };
    }
}
