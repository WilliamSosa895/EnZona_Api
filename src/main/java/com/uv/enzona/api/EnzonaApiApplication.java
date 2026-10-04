package com.uv.enzona.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la API REST de EnZona.
 *
 * <p>Arquitectura: monolito modular por capas (Anexo E, DA-01). Cada paquete de primer nivel
 * es un módulo de dominio (identidad, eventos, boletaje, validacion, cancelacion, reportes,
 * administracion, notificaciones) y {@code comun} agrupa lo transversal.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class EnzonaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnzonaApiApplication.class, args);
    }
}
