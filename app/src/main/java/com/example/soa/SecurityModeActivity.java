package com.example.soa;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class SecurityModeActivity extends AppCompatActivity implements MqttManager.MqttListener {

    private TextView tvCell1Status;
    private TextView tvCell2Status;
    private Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_mode);

        tvCell1Status = findViewById(R.id.tvSecCell1Status);
        tvCell2Status = findViewById(R.id.tvSecCell2Status);
        btnStop = findViewById(R.id.btnStopSecurity);

        btnStop.setOnClickListener(v -> MqttManager.getInstance().publicar("soa/grupol5/comando", "SECURITY_OFF"));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MqttManager.getInstance().setListener(this);
        actualizarInterfaz();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MqttManager.getInstance().setListener(null);
    }

    @Override
    public void onMensajeRecibido(String topic, String payload) {
        runOnUiThread(() -> {
            switch (topic) {

                case "soa/grupol5/estado":
                    StateManager.setEstadoActual(payload);
                    if (payload.equals(StateManager.ESTADO_VIRGEN)) {
                        finish();
                    }
                    break;

                case "soa/grupol5/security/alerta":
                    StateManager.setAlertaSecurity(payload);
                    actualizarInterfaz();
                    if (!payload.equals("0")) {
                        mostrarAlertaSeguridad(payload);
                    }
                    break;
            }
        });
    }

    private void actualizarInterfaz() {
        String alerta = StateManager.getAlertaSecurity();

        switch (alerta) {
            case "0":
                setSensorStatus(tvCell1Status, "Estado: SEGURO", true);
                setSensorStatus(tvCell2Status, "Estado: SEGURO", true);
                break;

            case "sensor01":
                setSensorStatus(tvCell1Status, "Estado: ALERTA", false);
                setSensorStatus(tvCell2Status, "Estado: SEGURO",  true);
                break;

            case "sensor02":
                setSensorStatus(tvCell1Status, "Estado: SEGURO",   true);
                setSensorStatus(tvCell2Status, "Estado: ALERTA", false);
                break;

            case "ambos":
                setSensorStatus(tvCell1Status, "Estado: ALERTA", false);
                setSensorStatus(tvCell2Status, "Estado: ALERTA", false);
                break;
        }
    }

    private void setSensorStatus(TextView tv, String texto, boolean seguro) {
        tv.setText(texto);
        tv.setTextColor(seguro? ContextCompat.getColor(this, R.color.green): ContextCompat.getColor(this, R.color.red));
    }

    @Override
    public void onConexionPerdida() {
        runOnUiThread(() ->
                Toast.makeText(this, "Conexion perdida con el broker", Toast.LENGTH_LONG).show()
        );
    }

    private void mostrarAlertaSeguridad(String sensor) {
        new AlertDialog.Builder(this)
                .setTitle("Alerta de Seguridad")
                .setMessage("Variacion de peso detectada en: " + sensor)
                .setPositiveButton("Reconocer", (d, w) -> d.dismiss())
                .setCancelable(false)
                .show();
    }
}