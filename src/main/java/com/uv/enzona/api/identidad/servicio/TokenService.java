package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.util.Fechas;
import com.uv.enzona.api.identidad.modelo.Usuario;
import com.uv.enzona.api.identidad.web.dto.SesionRespuesta;
import com.uv.enzona.api.identidad.web.dto.UsuarioResumen;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite el JWT de sesión (HS256, expiración corta). El sujeto es el id del usuario. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final EnzonaProperties propiedades;
    private final Clock reloj;

    public TokenService(JwtEncoder encoder, EnzonaProperties propiedades, Clock reloj) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public SesionRespuesta emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Duration vigencia = Duration.ofMinutes(propiedades.jwt().expiracionMinutos());
        Instant expira = ahora.plus(vigencia);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.jwt().emisor())
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(String.valueOf(usuario.getId()))
                .id(UUID.randomUUID().toString())
                .claim("nombre", usuario.getNombre())
                .claim("roles", usuario.nombresRoles())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new SesionRespuesta(token, "Bearer", vigencia.toSeconds(),
                Fechas.enZonaMx(OffsetDateTime.ofInstant(expira, ZoneOffset.UTC)), UsuarioResumen.de(usuario));
    }
}
