package com.uv.enzona.api.reportes;

import com.uv.enzona.api.eventos.modelo.EstadoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Panel del organizador: asistentes, ventas, aforo y accesos validados (RF-17, CP-45). */
public record ReporteRespuesta(
        Long eventoId,
        String evento,
        EstadoEvento estado,
        int aforo,
        int disponibles,
        @Schema(description = "Boletos emitidos y vigentes (válidos o usados)") long boletosEmitidos,
        @Schema(description = "Accesos registrados: boletos usados") long accesosValidados,
        @Schema(description = "Intentos de acceso rechazados en la puerta") long accesosRechazados,
        long boletosCancelados,
        @Schema(description = "Ingresos de órdenes pagadas") String ingresos,
        @Schema(description = "Importe ya devuelto a los asistentes") String reembolsado,
        @Schema(description = "Porcentaje del aforo ocupado por boletos vigentes") double ocupacion,
        List<PorTipo> porTipo) {

    public record PorTipo(Long tipoBoletoId, String nombre, String precio, int cantidadTotal, int disponibles,
            long emitidos, String ingresos) {
    }
}
