package com.uv.enzona.api.eventos.imagenes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Sirve las portadas guardadas por {@link AlmacenLocal} (solo con el proveedor local). */
@RestController
@RequestMapping(AlmacenLocal.RUTA_PUBLICA)
@ConditionalOnBean(AlmacenLocal.class)
@Tag(name = "Eventos")
public class ImagenesController {

    private final AlmacenLocal almacen;

    public ImagenesController(AlmacenLocal almacen) {
        this.almacen = almacen;
    }

    @GetMapping("/{archivo:[A-Za-z0-9]+\\.(?:jpg|png|webp)}")
    @Operation(summary = "Portada de un evento guardada en el servidor (público)")
    public ResponseEntity<Resource> imagen(@PathVariable String archivo) {
        Path ruta = almacen.resolver(archivo);
        if (ruta == null || !Files.isRegularFile(ruta)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(AlmacenLocal.tipoMimeDe(archivo)))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(new PathResource(ruta));
    }
}
