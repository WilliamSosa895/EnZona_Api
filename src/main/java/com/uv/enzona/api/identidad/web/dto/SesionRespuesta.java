package com.uv.enzona.api.identidad.web.dto;

import java.time.OffsetDateTime;

public record SesionRespuesta(String token, String tipo, long expiraEnSegundos, OffsetDateTime expiraEn,
        UsuarioResumen usuario) {
}
