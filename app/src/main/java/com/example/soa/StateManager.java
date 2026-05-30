package com.example.soa;

import org.json.JSONObject;

public class StateManager {
    public static final String ESTADO_VIRGEN = "IDLE";
    public static final String ESTADO_STOCK = "STOCK";
    public static final String ESTADO_SEGURIDAD = "SECURITY";

    private static String estadoActual = ESTADO_VIRGEN;   // = status.active (modo efectivo, con prioridad)
    private static boolean stockOn = false;               // = status.stock  (toggle activado por el usuario)
    private static boolean securityOn = false;            // = status.security
    private static String availability = "offline";
    private static long lastUpdated = 0;
    
    public static class ShelfData {
        public double weight = 0;
        public int stock = 0;
        public int min = 0;
        public boolean available = false;
        public boolean secure = true;
        public double baseline = 0;
        public double current = 0;
        public double delta = 0;
    }

    private static ShelfData shelf01 = new ShelfData();
    private static ShelfData shelf02 = new ShelfData();

    public static String getEstadoActual() { return estadoActual; }   // modo que corre por prioridad
    public static boolean isStockOn() { return stockOn; }
    public static boolean isSecurityOn() { return securityOn; }
    public static String getAvailability() { return availability; }
    public static long getLastUpdated() { return lastUpdated; }
    public static ShelfData getShelf01() { return shelf01; }
    public static ShelfData getShelf02() { return shelf02; }

    public static void updateFromBackend(JSONObject state) {
        // Parseo defensivo: el ESP32 publica on-change, asi que los estados parciales
        // (ej. stock sin security todavia) son normales. Si falta una seccion se
        // conserva el valor previo en vez de abortar toda la actualizacion.
        availability = state.optString("availability", availability);
        lastUpdated = state.optLong("updatedAt", lastUpdated);

        JSONObject status = state.optJSONObject("status");
        if (status != null) {
            estadoActual = status.optString("active", estadoActual);   // modo efectivo (prioridad)
            stockOn = status.optBoolean("stock", stockOn);             // toggles independientes
            securityOn = status.optBoolean("security", securityOn);
        }

        JSONObject shelf = state.optJSONObject("shelf");
        if (shelf != null) {
            if (shelf.has("01")) updateShelf(shelf01, shelf.optJSONObject("01"));
            if (shelf.has("02")) updateShelf(shelf02, shelf.optJSONObject("02"));
        }
    }

    private static void updateShelf(ShelfData data, JSONObject json) {
        if (json == null) return;

        JSONObject stock = json.optJSONObject("stock");
        if (stock != null) {
            data.weight = stock.optDouble("weight", data.weight);
            data.stock = stock.optInt("stock", data.stock);
            data.min = stock.optInt("min", data.min);
            data.available = stock.optBoolean("available", data.available);
        }

        JSONObject security = json.optJSONObject("security");
        if (security != null) {
            data.secure = security.optBoolean("secure", data.secure);
            data.baseline = security.optDouble("baseline", data.baseline);
            data.current = security.optDouble("current", data.current);
            data.delta = security.optDouble("delta", data.delta);
        }
    }
    
    // Legacy support
    public static void setEstadoActual(String estado) { estadoActual = estado; }
}