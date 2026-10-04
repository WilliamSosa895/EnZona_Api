package com.uv.enzona.api.eventos.web;

import com.uv.enzona.api.eventos.repositorio.CategoriaRepository;
import com.uv.enzona.api.eventos.web.dto.CategoriaRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Eventos")
public class CategoriaController {

    private final CategoriaRepository categorias;

    public CategoriaController(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    @Operation(summary = "Catálogo de categorías (público)")
    public List<CategoriaRespuesta> listar() {
        return categorias.findAll(Sort.by("id")).stream()
                .map(categoria -> new CategoriaRespuesta(categoria.getId(), categoria.getNombre()))
                .toList();
    }
}
