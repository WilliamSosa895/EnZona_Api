package com.uv.enzona.api.validacion.servicio;

import com.uv.enzona.api.boletaje.modelo.Boleto;
import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import com.uv.enzona.api.boletaje.repositorio.BoletoRepository;
import com.uv.enzona.api.boletaje.servicio.FirmaBoletos;
import com.uv.enzona.api.boletaje.servicio.QrBoleto;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.eventos.modelo.Asiento;
import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import com.uv.enzona.api.eventos.repositorio.AsientoRepository;
import com.uv.enzona.api.eventos.repositorio.EventoRepository;
import com.uv.enzona.api.eventos.repositorio.EventoValidadorRepository;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.validacion.modelo.MotivoRechazo;
import com.uv.enzona.api.validacion.modelo.ResultadoAcceso;
import com.uv.enzona.api.validacion.modelo.ValidacionAcceso;
import com.uv.enzona.api.validacion.repositorio.ValidacionAccesoRepository;
import com.uv.enzona.api.validacion.web.dto.ManifiestoRespuesta;
import com.uv.enzona.api.validacion.web.dto.RegistroOffline;
import com.uv.enzona.api.validacion.web.dto.ResultadoSincronizado;
import com.uv.enzona.api.validacion.web.dto.SincronizacionRespuesta;
import com.uv.enzona.api.validacion.web.dto.SincronizacionSolicitud;
import com.uv.enzona.api.validacion.web.dto.ValidacionRespuesta;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validación de acceso en la puerta (RF-15, CU-06; CP-12, CP-13, CP-14, CP-40 a CP-43).
 *
 * <p>Separa dos comprobaciones (Anexo E, E.7.2): la autenticidad del boleto, con su firma HMAC,
 * y su uso único, con una actualización condicional en la base de datos. Solo el primer escaneo
 * consigue pasar el boleto de VALIDO a USADO.</p>
 */
@Service
public class ValidacionService {

    private final BoletoRepository boletos;
    private final EventoRepository eventos;
    private final EventoValidadorRepository designaciones;
    private final TipoBoletoRepository tipos;
    private final AsientoRepository asientos;
    private final ValidacionAccesoRepository bitacora;
    private final FirmaBoletos firma;
    private final Clock reloj;

    public ValidacionService(BoletoRepository boletos, EventoRepository eventos,
            EventoValidadorRepository designaciones, TipoBoletoRepository tipos, AsientoRepository asientos,
            ValidacionAccesoRepository bitacora, FirmaBoletos firma, Clock reloj) {
        this.boletos = boletos;
        this.eventos = eventos;
        this.designaciones = designaciones;
        this.tipos = tipos;
        this.asientos = asientos;
        this.bitacora = bitacora;
        this.firma = firma;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ en línea

    @Transactional
    public ValidacionRespuesta validar(Long validadorId, boolean esAdmin, Long eventoId, String contenidoQr) {
        Evento evento = autorizar(eventoId, validadorId, esAdmin);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        Decision decision = decidir(evento, contenidoQr, ahora);
        if (decision.boleto() != null) {
            bitacora.save(ValidacionAcceso.registrar(decision.boleto().getId(), validadorId, ahora,
                    decision.resultado(), decision.motivo(), false));
        }
        return respuesta(decision);
    }

    // ------------------------------------------------------------------ sin conexión

    /** Manifiesto con los boletos vigentes y usados del evento para validar sin red. */
    @Transactional(readOnly = true)
    public ManifiestoRespuesta manifiesto(Long validadorId, boolean esAdmin, Long eventoId) {
        Evento evento = autorizar(eventoId, validadorId, esAdmin);
        List<Boleto> delEvento = boletos.delEventoEnEstados(eventoId, List.of(EstadoBoleto.VALIDO, EstadoBoleto.USADO));
        Map<Long, String> nombresTipo = tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId).stream()
                .collect(Collectors.toMap(TipoBoleto::getId, TipoBoleto::getNombre));
        Map<Long, Asiento> porAsiento = asientos.findByEventoId(eventoId).stream()
                .collect(Collectors.toMap(Asiento::getId, Function.identity()));
        List<ManifiestoRespuesta.Entrada> entradas = delEvento.stream()
                .map(b -> new ManifiestoRespuesta.Entrada(b.getCodigo(), b.getQrFirma(), b.getEstado(),
                        nombresTipo.get(b.getTipoBoletoId()),
                        b.getAsientoId() == null ? null : porAsiento.get(b.getAsientoId()).etiqueta()))
                .toList();
        return new ManifiestoRespuesta(eventoId, evento.getNombre(), Fechas.enZonaMx(OffsetDateTime.now(reloj)),
                entradas.size(), entradas);
    }

    /**
     * Sube los escaneos hechos sin conexión. Se procesan del más antiguo al más reciente: si dos
     * dispositivos admitieron el mismo boleto, prevalece el primero y el segundo queda en la
     * bitácora como rechazado por conflicto (CP-14, CP-43). Reenviar el mismo lote no duplica nada.
     */
    @Transactional
    public SincronizacionRespuesta sincronizar(Long validadorId, boolean esAdmin, SincronizacionSolicitud solicitud) {
        Evento evento = autorizar(solicitud.eventoId(), validadorId, esAdmin);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        List<RegistroOffline> ordenados = solicitud.registros().stream()
                .sorted(Comparator.comparing(RegistroOffline::fechaHora))
                .toList();

        List<ResultadoSincronizado> resultados = new ArrayList<>();
        int permitidos = 0;
        int conflictos = 0;
        for (RegistroOffline registro : ordenados) {
            OffsetDateTime fechaEscaneo = registro.fechaHora().isAfter(ahora) ? ahora : registro.fechaHora();
            Decision decision = decidir(evento, registro.qr(), fechaEscaneo);
            boolean duplicado = decision.boleto() != null && bitacora.existsByBoletoIdAndValidadorIdAndFechaHora(
                    decision.boleto().getId(), validadorId, fechaEscaneo);
            if (decision.boleto() != null && !duplicado) {
                bitacora.save(ValidacionAcceso.registrar(decision.boleto().getId(), validadorId, fechaEscaneo,
                        decision.resultado(), decision.motivo(), true));
            }
            boolean conflicto = !duplicado && decision.motivo() == MotivoRechazo.YA_UTILIZADO;
            if (decision.resultado() == ResultadoAcceso.PERMITIDO) {
                permitidos++;
            }
            if (conflicto) {
                conflictos++;
            }
            resultados.add(new ResultadoSincronizado(registro.qr(), Fechas.enZonaMx(fechaEscaneo), decision.resultado(),
                    decision.motivo(), conflicto, duplicado));
        }
        return new SincronizacionRespuesta(resultados.size(), permitidos, resultados.size() - permitidos, conflictos,
                resultados);
    }

    // ------------------------------------------------------------------ decisión

    private Decision decidir(Evento evento, String contenidoQr, OffsetDateTime momento) {
        Optional<QrBoleto> qr = QrBoleto.leer(contenidoQr);
        if (qr.isEmpty()) {
            return Decision.rechazo(null, MotivoRechazo.FORMATO_INVALIDO);
        }
        Optional<Boleto> encontrado = boletos.findByCodigo(qr.get().codigo());
        if (encontrado.isEmpty()) {
            return Decision.rechazo(null, MotivoRechazo.BOLETO_INEXISTENTE);
        }
        Boleto boleto = encontrado.get();
        Long eventoDelBoleto = boletos.eventoDelBoleto(boleto.getId()).orElseThrow();
        if (!firma.verificar(boleto.getCodigo(), eventoDelBoleto, qr.get().firma())) {
            return Decision.rechazo(boleto, MotivoRechazo.FIRMA_INVALIDA);
        }
        if (!eventoDelBoleto.equals(evento.getId())) {
            return Decision.rechazo(boleto, MotivoRechazo.OTRO_EVENTO);
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            return Decision.rechazo(boleto, MotivoRechazo.EVENTO_CANCELADO);
        }
        switch (boleto.getEstado()) {
            case USADO:
                return Decision.rechazo(boleto, MotivoRechazo.YA_UTILIZADO);
            case CANCELADO:
                return Decision.rechazo(boleto, MotivoRechazo.CANCELADO);
            case EXPIRADO:
                return Decision.rechazo(boleto, MotivoRechazo.EXPIRADO);
            case VALIDO:
            default:
                break;
        }
        boolean primero = boletos.marcarUsado(boleto.getId(), momento, EstadoBoleto.VALIDO, EstadoBoleto.USADO) == 1;
        return primero
                ? new Decision(boleto, ResultadoAcceso.PERMITIDO, null, momento)
                : Decision.rechazo(boleto, MotivoRechazo.YA_UTILIZADO);
    }

    private ValidacionRespuesta respuesta(Decision decision) {
        Boleto boleto = decision.boleto();
        String mensaje = decision.resultado() == ResultadoAcceso.PERMITIDO ? "Acceso permitido" : decision.motivo().mensaje();
        if (boleto == null) {
            return new ValidacionRespuesta(decision.resultado(), decision.motivo(), mensaje, null, null, null, null);
        }
        String tipo = tipos.findById(boleto.getTipoBoletoId()).map(TipoBoleto::getNombre).orElse(null);
        String asiento = boleto.getAsientoId() == null ? null
                : asientos.findById(boleto.getAsientoId()).map(Asiento::etiqueta).orElse(null);
        OffsetDateTime fechaUso = decision.resultado() == ResultadoAcceso.PERMITIDO ? decision.momento()
                : boletos.findById(boleto.getId()).map(Boleto::getFechaUso).orElse(null);
        return new ValidacionRespuesta(decision.resultado(), decision.motivo(), mensaje, boleto.getCodigo(), tipo,
                asiento, Fechas.enZonaMx(fechaUso));
    }

    /** El organizador del evento, un administrador o un validador designado para ese evento. */
    private Evento autorizar(Long eventoId, Long usuarioId, boolean esAdmin) {
        Evento evento = eventos.findById(eventoId).orElseThrow(() -> ApiException.noEncontrado("Evento", eventoId));
        if (!esAdmin && !evento.perteneceA(usuarioId) && !designaciones.estaDesignado(eventoId, usuarioId)) {
            throw ApiException.accesoDenegado();
        }
        return evento;
    }

    private record Decision(Boleto boleto, ResultadoAcceso resultado, MotivoRechazo motivo, OffsetDateTime momento) {

        static Decision rechazo(Boleto boleto, MotivoRechazo motivo) {
            return new Decision(boleto, ResultadoAcceso.RECHAZADO, motivo, null);
        }
    }
}
