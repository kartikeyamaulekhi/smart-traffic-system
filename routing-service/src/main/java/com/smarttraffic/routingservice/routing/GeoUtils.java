package com.smarttraffic.routingservice.routing;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Great-circle distance between two lat/lng points, in kilometers.
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Rounds coordinates to ~1 meter precision and joins them into a stable
     * node key, so two segments that share an endpoint (even with tiny
     * floating-point differences) are recognized as the same graph node.
     */
    public static String nodeKey(double lat, double lng) {
        return String.format("%.5f,%.5f", lat, lng);
    }

}