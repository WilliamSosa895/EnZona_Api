package com.uv.enzona.api.eventos.repositorio;

import com.uv.enzona.api.eventos.modelo.TipoBoleto;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Las operaciones de inventario son actualizaciones condicionales: devuelven 1 si se aplicaron
 * y 0 si la condición ya no se cumplía (por ejemplo, otro comprador tomó el último lugar).
 */
public interface TipoBoletoRepository extends JpaRepository<TipoBoleto, Long> {

    List<TipoBoleto> findByEventoIdOrderByPrecioAscIdAsc(Long eventoId);

    /** Descuenta N lugares solo si quedan al menos N (Anexo E, escenario E.7.1). */
    @Modifying(flushAutomatically = true)
    @Query("update TipoBoleto t set t.cantidadDisponible = t.cantidadDisponible - :cantidad "
            + "where t.id = :id and t.cantidadDisponible >= :cantidad")
    int descontar(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Devuelve N lugares (pago rechazado, orden expirada o cancelada). */
    @Modifying(flushAutomatically = true)
    @Query("update TipoBoleto t set t.cantidadDisponible = t.cantidadDisponible + :cantidad "
            + "where t.id = :id and t.cantidadDisponible + :cantidad <= t.cantidadTotal")
    int reponer(@Param("id") Long id, @Param("cantidad") int cantidad);

    /**
     * Cambia el total conservando lo ya comprometido. No se aplica si el nuevo total es menor
     * que lo vendido o reservado (RF-13, CP-15).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TipoBoleto t set t.cantidadDisponible = t.cantidadDisponible + (:total - t.cantidadTotal), "
            + "t.cantidadTotal = :total "
            + "where t.id = :id and t.cantidadDisponible + (:total - t.cantidadTotal) >= 0")
    int ajustarTotal(@Param("id") Long id, @Param("total") int total);

    /** El precio solo cambia si el tipo no tiene ventas (transparencia de precios, RNF-09). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TipoBoleto t set t.precio = :precio where t.id = :id and t.cantidadDisponible = t.cantidadTotal")
    int cambiarPrecioSinVentas(@Param("id") Long id, @Param("precio") BigDecimal precio);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TipoBoleto t set t.nombre = :nombre where t.id = :id")
    int renombrar(@Param("id") Long id, @Param("nombre") String nombre);

    @Query("select t.eventoId as eventoId, sum(t.cantidadDisponible) as disponibles, "
            + "sum(t.cantidadTotal) as total, min(t.precio) as precioDesde "
            + "from TipoBoleto t where t.eventoId in :eventos group by t.eventoId")
    List<ResumenInventario> resumir(@Param("eventos") Collection<Long> eventos);

    /** Proyección con el inventario agregado de cada evento. */
    interface ResumenInventario {
        Long getEventoId();

        Long getDisponibles();

        Long getTotal();

        BigDecimal getPrecioDesde();
    }
}
