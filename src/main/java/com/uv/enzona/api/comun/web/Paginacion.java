package com.uv.enzona.api.comun.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Las listas se devuelven como arreglo JSON (contrato de la app Android) y los datos de
 * paginación viajan en cabeceras: X-Total-Count, X-Page, X-Page-Size y X-Total-Pages.
 */
public final class Paginacion {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    public static int tamano(Integer solicitado) {
        if (solicitado == null || solicitado < 1) {
            return TAMANO_POR_DEFECTO;
        }
        return Math.min(solicitado, TAMANO_MAXIMO);
    }

    public static int pagina(Integer solicitada) {
        return solicitada == null || solicitada < 0 ? 0 : solicitada;
    }

    public static <T> ResponseEntity<List<T>> respuesta(Page<T> pagina) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.add("X-Total-Count", String.valueOf(pagina.getTotalElements()));
        cabeceras.add("X-Page", String.valueOf(pagina.getNumber()));
        cabeceras.add("X-Page-Size", String.valueOf(pagina.getSize()));
        cabeceras.add("X-Total-Pages", String.valueOf(pagina.getTotalPages()));
        return ResponseEntity.ok().headers(cabeceras).body(pagina.getContent());
    }
}
