package com.uv.enzona.api.identidad.servicio;

/**
 * Puerto que el módulo de eventos implementa. Permite a identidad consultar si un organizador
 * tiene eventos vigentes sin depender directamente del módulo de eventos (inversión de dependencias).
 */
public interface ConsultaEventosVigentes {

    boolean tieneEventosVigentes(Long organizadorId);
}
