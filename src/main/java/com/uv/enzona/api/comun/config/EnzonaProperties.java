package com.uv.enzona.api.comun.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia de EnZona ({@code enzona.*}). Los secretos llegan por variables de
 * entorno y nunca se escriben en el repositorio. Si falta alguno, la aplicación no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "enzona")
public record EnzonaProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Boletos boletos,
        @Valid @NotNull Curp curp,
        @Valid @NotNull Ordenes ordenes,
        @Valid @NotNull Verificacion verificacion,
        @Valid @NotNull Sesion sesion,
        @Valid @NotNull Pasarela pasarela,
        @Valid @NotNull Notificaciones notificaciones,
        @Valid @NotNull Eventos eventos,
        @Valid @NotNull Imagenes imagenes,
        @Valid Admin admin,
        @Valid Cors cors) {
    /**
     * Portadas de los eventos. {@code proveedor=local} guarda los archivos en {@code carpetaLocal}
     * y la API los sirve en /api/imagenes; {@code proveedor=cloudinary} las sube a Cloudinary con
     * {@code cloudinaryUrl} (cloudinary://api_key:api_secret@cloud_name), que nunca sale del servidor.
     */
    public record Imagenes(@NotBlank String proveedor, String cloudinaryUrl, @NotBlank String carpetaLocal,
            @Min(1) @Max(20) int maximoMb,
            /** off | sightengine | cloudinary (add-on de moderación en la misma subida). */
            @NotBlank String moderacion, String sightengineUsuario, String sightengineSecreto,
            /** Probabilidad (0 a 1) a partir de la cual se rechaza la imagen. */
            @DecimalMin("0.1") @DecimalMax("1.0") double umbralModeracion) { }

    /** Firma de los JWT (HS256). El secreto va en Base64 y debe tener al menos 32 bytes. */
    public record Jwt(@NotBlank String secreto, @NotBlank String emisor, @Min(5) @Max(1440) long expiracionMinutos) { }

    /** Clave HMAC-SHA256 con la que se firman los boletos (RNF-02). Base64, mínimo 32 bytes. */
    public record Boletos(@NotBlank String secretoHmac) { }

    /** Cifrado AES-256-GCM del CURP y clave del índice ciego (RNF-03). Ambas en Base64, 32 bytes. */
    public record Curp(@NotBlank String claveCifrado, @NotBlank String claveIndice) { }

    /** Reserva de lugares mientras se confirma el pago y límite de boletos por orden. */
    public record Ordenes(@Min(1) @Max(120) int minutosReserva, @Min(1) @Max(50) int maximoBoletosPorOrden) { }

    /** Códigos de un solo uso (RF-02). */
    public record Verificacion(@Min(1) @Max(60) int minutosVigencia, @Min(1) @Max(10) int maximoIntentos) { }

    /** Bloqueo temporal tras intentos fallidos de inicio de sesión. */
    public record Sesion(@Min(1) @Max(20) int maximoIntentos, @Min(1) @Max(1440) int minutosBloqueo) { }

    /** Pasarela de pago externa. {@code proveedor=sandbox} usa la pasarela simulada. */
    public record Pasarela(@NotBlank String proveedor, @NotBlank String secretoWebhook) { }

    /** {@code modo=log} escribe los avisos en la bitácora (desarrollo); {@code modo=smtp} envía correos. */
    public record Notificaciones(@NotBlank String modo, @NotBlank String remitente) { }

    /**
     * Horas después del inicio en que un evento sin fecha de fin se considera realizado, y radio
     * máximo de búsqueda por cercanía.
     */
    public record Eventos(@Min(1) @Max(72) int horasDuracionPorDefecto, @Min(1) @Max(500) int radioMaximoKm) { }

    /** Cuenta de administrador inicial (opcional). Se crea solo si no existe. */
    public record Admin(String nombre, String correo, String contrasena) {
        public boolean configurado() {
            return correo != null && !correo.isBlank() && contrasena != null && !contrasena.isBlank();
        }
    }

    /** Orígenes permitidos para la aplicación web (CORS). */
    public record Cors(List<String> origenes) { }
}
