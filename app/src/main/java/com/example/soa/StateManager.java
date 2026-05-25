package com.example.soa;

public class StateManager {
    public static final String ESTADO_VIRGEN = "VIRGIN_EMBEDDED";
    public static final String ESTADO_STOCK = "STOCK_MODE";
    public static final String ESTADO_SEGURIDAD = "SECURITY_MODE";

    private static String estadoActual = ESTADO_VIRGEN;

    private static String alertaStock    = "0";
    private static String alertaSecurity = "0";
    private static String pesoSensor1    = "--";
    private static String pesoSensor2    = "--";

    public static String getEstadoActual() {
        return estadoActual;
    }

    public static void setEstadoActual(String estado) {
        if (estado.equals(ESTADO_VIRGEN) ||
                estado.equals(ESTADO_STOCK)  ||
                estado.equals(ESTADO_SEGURIDAD)) {
            estadoActual = estado;
        } else {
            android.util.Log.w("StateManager", "Estado desconocido: " + estado);
        }
    }

    public static void resetear() {
        estadoActual = ESTADO_VIRGEN;
        alertaStock    = "0";
        alertaSecurity = "0";
        pesoSensor1    = "--";
        pesoSensor2    = "--";
    }

    // alertas
    public static String getAlertaStock()            { return alertaStock; }
    public static void   setAlertaStock(String v)    { alertaStock = v; }

    public static String getAlertaSecurity()         { return alertaSecurity; }
    public static void   setAlertaSecurity(String v) { alertaSecurity = v; }

    // pesos de sensores
    public static String getPesoSensor1()            { return pesoSensor1; }
    public static void   setPesoSensor1(String v)    { pesoSensor1 = v; }

    public static String getPesoSensor2()            { return pesoSensor2; }
    public static void   setPesoSensor2(String v)    { pesoSensor2 = v; }
}
