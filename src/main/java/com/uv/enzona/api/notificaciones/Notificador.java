package com.uv.enzona.api.notificaciones;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;

/**
 * Puerto de salida de las notificaciones. Hay dos implementaciones: bitácora (desarrollo) y
 * correo SMTP. El envío por SMS del código de teléfono queda pendiente de contratar proveedor.
 */
public interface Notificador {

    void enviarCodigo(String destino, CanalVerificacion canal, String nombre, String codigo, int minutosVigencia);

    void enviarBoletos(MensajeBoletos mensaje);

    void avisarCancelacion(String destinatario, String nombre, String evento, String motivo);

    void avisarCambio(String destinatario, String nombre, String evento, String cambios);
}
