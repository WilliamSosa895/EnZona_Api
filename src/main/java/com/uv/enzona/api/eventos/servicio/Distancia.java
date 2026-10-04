package com.uv.enzona.api.eventos.servicio;

/** Cálculos geográficos para el descubrimiento por cercanía (RF-19). */
public final class Distancia {

    private static final double RADIO_TIERRA_KM = 6371.0088;
    private static final double KM_POR_GRADO = 111.32;

    private Distancia() {
    }

    /** Distancia en kilómetros sobre la superficie terrestre (fórmula del haversine). */
    public static double km(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Rectángulo que contiene el círculo de búsqueda; permite filtrar primero con el índice idx_evento_geo. */
    public static Caja caja(double lat, double lon, double radioKm) {
        double dLat = radioKm / KM_POR_GRADO;
        double coseno = Math.max(0.01, Math.cos(Math.toRadians(lat)));
        double dLon = radioKm / (KM_POR_GRADO * coseno);
        return new Caja(lat - dLat, lat + dLat, lon - dLon, lon + dLon);
    }

    public record Caja(double latMin, double latMax, double lonMin, double lonMax) {
    }
}
