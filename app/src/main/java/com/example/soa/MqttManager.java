package com.example.soa;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttManager {

    private static MqttManager instance;
    private MqttAsyncClient mqttClient;
    private static final String BROKER_URL = "ws://broker.emqx.io:8083/mqtt";
    private static final String CLIENT_ID = MqttAsyncClient.generateClientId();

    private MqttManager() {
    }

    public static synchronized MqttManager getInstance() {
        if(instance == null) { instance = new MqttManager(); }
        return instance;
    }

    public interface MqttListener {
        void onMensajeRecibido(String topic, String payload);
        void onConexionPerdida();
    }

    private MqttListener listener;

    public void setListener(MqttListener listener) {
        this.listener = listener;
    }

    public void conectar(Context context) {
        if (mqttClient != null && mqttClient.isConnected()) {
            Log.d("MQTT", "Ya estaba conectado");
            return;
        }

        try {
            mqttClient = new MqttAsyncClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT", "Conexion perdida: " + cause.getMessage());
                    StateManager.resetear();
                    if (listener != null) listener.onConexionPerdida();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d("MQTT", "Mensaje recibido [" + topic + "]: " + payload);
                    if (listener != null) listener.onMensajeRecibido(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    Log.d("MQTT", "Conectado al broker.");
                    suscribir("soa/grupol5/estado");
                    suscribir("soa/grupol5/stock/sensor01");
                    suscribir("soa/grupol5/stock/sensor02");
                    suscribir("soa/grupol5/stock/alerta");
                    suscribir("soa/grupol5/security/alerta");
                }

                @Override
                public void onFailure(IMqttToken token, Throwable exception) {
                    Log.e("MQTT", "Fallo la conexion: " + exception.getMessage());
                    exception.printStackTrace();
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void suscribir(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, 0);
                Log.d("MQTT", "Suscripto a: " + topic);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publicar(String topic, String payload) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(0);
                mqttClient.publish(topic, message);
                Log.d("MQTT", "Publicado [" + topic + "]: " + payload);
            } else {
                Log.e("MQTT", "No se puede publicar: cliente no conectado");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void desconectar() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                Log.d("MQTT", "Desconectado del broker");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean estaConectado() {
        return mqttClient != null && mqttClient.isConnected();
    }

}
