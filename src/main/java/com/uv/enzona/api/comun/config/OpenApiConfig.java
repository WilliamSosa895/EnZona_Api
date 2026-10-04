package com.uv.enzona.api.comun.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de la especificación OpenAPI que publica springdoc en /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI enzonaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EnZona API")
                        .version("0.1.0")
                        .description("API REST de EnZona: descubrimiento de eventos por ubicación, boletaje "
                                + "gratuito y de pago, boletos firmados con QR de un solo uso y validación en puerta "
                                + "con modo sin conexión. Área de estudio: Orizaba, Veracruz.")
                        .contact(new Contact().name("Equipo EnZona · Universidad Veracruzana")))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/auth/sesion")));
    }
}
