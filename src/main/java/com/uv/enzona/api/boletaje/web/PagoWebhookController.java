package com.uv.enzona.api.boletaje.web;

import com.uv.enzona.api.boletaje.servicio.WebhookPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Pagos", description = "Notificaciones de la pasarela de pago")
public class PagoWebhookController {

    private final WebhookPagoService webhook;

    public PagoWebhookController(WebhookPagoService webhook) {
        this.webhook = webhook;
    }

    /** El cuerpo se recibe como texto para verificar la firma sobre los bytes exactos enviados. */
    @PostMapping("/webhook")
    @Operation(summary = "Confirma o rechaza un cobro pendiente (firma HMAC en X-Pasarela-Firma)")
    public Map<String, Boolean> recibir(@RequestBody String cuerpo,
            @RequestHeader(name = "X-Pasarela-Firma", required = false) String firma) {
        webhook.procesar(cuerpo, firma);
        return Map.of("recibido", true);
    }
}
