package com.uv.enzona.api.eventos.imagenes;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.uv.enzona.api.comun.config.EnzonaProperties;
import com.uv.enzona.api.comun.error.ApiException;
import com.uv.enzona.api.comun.error.ErrorCodigo;
import com.uv.enzona.api.eventos.modelo.Evento;
import com.uv.enzona.api.eventos.repositorio.TipoBoletoRepository;
import com.uv.enzona.api.eventos.servicio.AccesoEventos;
import com.uv.enzona.api.eventos.servicio.EventoMapper;
import com.uv.enzona.api.eventos.web.dto.EventoRespuesta;

/**
 * Portada de un evento. Solo el organizador dueño (o un administrador) la cambia;
 * el archivo se valida por tipo y tamaño antes de tocar el almacén, y la imagen
 * anterior se borra al reemplazarla.
 */
@Service
public class ImagenEventoService {

    private static final Logger log = LoggerFactory.getLogger(ImagenEventoService.class);
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final AlmacenImagenes almacen;
    private final AccesoEventos acceso;
    private final TipoBoletoRepository tipos;
    private final EventoMapper mapper;
    private final EnzonaProperties propiedades;

    public ImagenEventoService(AlmacenImagenes almacen, AccesoEventos acceso, TipoBoletoRepository tipos,
            EventoMapper mapper, EnzonaProperties propiedades) {
        this.almacen = almacen;
        this.acceso = acceso;
        this.tipos = tipos;
        this.mapper = mapper;
        this.propiedades = propiedades;
    }

    @Transactional
    public EventoRespuesta asignar(Long eventoId, Long usuarioId, boolean esAdmin, MultipartFile archivo) {
        byte[] datos = validar(archivo);
        Evento evento = acceso.bloquearGestionable(eventoId, usuarioId, esAdmin, true);
        if (!evento.esModificable()) {
            throw new ApiException(ErrorCodigo.ESTADO_INVALIDO, "Un evento " + evento.getEstado() + " no se modifica");
        }
        String anterior = evento.getImagenId();
        AlmacenImagenes.ImagenGuardada guardada = almacen.guardar(datos, archivo.getContentType(), "eventos/" + eventoId);
        evento.asignarImagen(guardada.url(), guardada.id());
        borrarSinFallar(anterior);
        return mapper.detalle(evento, tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId));
    }

    @Transactional
    public EventoRespuesta quitar(Long eventoId, Long usuarioId, boolean esAdmin) {
        Evento evento = acceso.bloquearGestionable(eventoId, usuarioId, esAdmin, true);
        String anterior = evento.getImagenId();
        evento.quitarImagen();
        borrarSinFallar(anterior);
        return mapper.detalle(evento, tipos.findByEventoIdOrderByPrecioAscIdAsc(eventoId));
    }

    private byte[] validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.reglaNegocio("Adjunta una imagen (campo \"imagen\")");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !TIPOS_PERMITIDOS.contains(tipo.toLowerCase())) {
            throw ApiException.reglaNegocio("La imagen debe ser JPEG, PNG o WebP");
        }
        long maximo = propiedades.imagenes().maximoMb() * 1024L * 1024L;
        if (archivo.getSize() > maximo) {
            throw ApiException.reglaNegocio("La imagen no puede pesar más de " + propiedades.imagenes().maximoMb() + " MB");
        }
        try {
            byte[] datos = archivo.getBytes();
            if (!esImagen(datos)) {
                throw ApiException.reglaNegocio("El archivo no es una imagen válida");
            }
            return datos;
        } catch (IOException e) {
            throw ApiException.reglaNegocio("No se pudo leer la imagen");
        }
    }

    /** Comprueba la firma del archivo (JPEG, PNG o WebP), no solo el tipo que declara el cliente. */
    static boolean esImagen(byte[] datos) {
        if (datos.length < 12) {
            return false;
        }
        boolean jpeg = (datos[0] & 0xFF) == 0xFF && (datos[1] & 0xFF) == 0xD8;
        boolean png = (datos[0] & 0xFF) == 0x89 && datos[1] == 'P' && datos[2] == 'N' && datos[3] == 'G';
        boolean webp = datos[0] == 'R' && datos[1] == 'I' && datos[2] == 'F' && datos[3] == 'F'
                && datos[8] == 'W' && datos[9] == 'E' && datos[10] == 'B' && datos[11] == 'P';
        return jpeg || png || webp;
    }

    private void borrarSinFallar(String id) {
        if (id == null) {
            return;
        }
        try {
            almacen.eliminar(id);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar la imagen anterior {}: {}", id, e.getMessage());
        }
    }
}
