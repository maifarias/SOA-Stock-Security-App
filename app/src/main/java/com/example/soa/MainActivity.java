package com.example.soa;


import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;


public class MainActivity extends AppCompatActivity implements MqttManager.MqttListener {

    private Button btnStock;
    private Button btnSecurity;
    private Button btnStop;
    private TextView tvState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvState = findViewById(R.id.tvState);
        btnStock = findViewById(R.id.btnStock);
        btnSecurity = findViewById(R.id.btnSecurity);
        btnStop = findViewById(R.id.btnStop);

        MqttManager.getInstance().conectar(this);

        btnStock.setOnClickListener(v -> {
            if (StateManager.getEstadoActual().equals(StateManager.ESTADO_STOCK)) {
                startActivity(new Intent(MainActivity.this, StockModeActivity.class));
            } else {
                MqttManager.getInstance().publicar("soa/grupol5/comando", "STOCK_ON");
            }
        });

        btnSecurity.setOnClickListener(v -> {
            if (StateManager.getEstadoActual().equals(StateManager.ESTADO_SEGURIDAD)) {
                startActivity(new Intent(MainActivity.this, SecurityModeActivity.class));
            } else {
                MqttManager.getInstance().publicar("soa/grupol5/comando", "SECURITY_ON");
            }
        });

        btnStop.setOnClickListener(v -> {
            MqttManager.getInstance().publicar("soa/grupol5/comando", "OFF");
        });

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
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            MqttManager.getInstance().desconectar();
        }
    }

    @Override
    public void onConexionPerdida() {
        runOnUiThread(() -> {
            actualizarInterfaz();
            Toast.makeText(this, "Conexion perdida con el broker", Toast.LENGTH_LONG).show();
        });
    }
    private void navegarSegunEstado(String estado) {
        switch (estado) {
            case StateManager.ESTADO_STOCK:
                startActivity(new Intent(this, StockModeActivity.class));
                break;
            case StateManager.ESTADO_SEGURIDAD:
                startActivity(new Intent(this, SecurityModeActivity.class));
                break;
        }
    }

    @Override
    public void onMensajeRecibido(String topic, String payload) {
        runOnUiThread(() -> {
            switch (topic) {
                case "soa/grupol5/estado":
                    StateManager.setEstadoActual(payload);
                    actualizarInterfaz();
                    navegarSegunEstado(payload);
                    break;

                case "soa/grupol5/stock/alerta":
                    if (!payload.equals("0"))
                        mostrarAlerta("Falta de Stock", "Sin stock en: " + payload);
                    break;

                case "soa/grupol5/security/alerta":
                    if (!payload.equals("0"))
                        mostrarAlerta("Alerta de Seguridad", "Variacion detectada en: " + payload);
                    break;
            }
        });
    }

    private void actualizarInterfaz() {
        String estado = StateManager.getEstadoActual();

        switch (estado) {
            case StateManager.ESTADO_VIRGEN:
                tvState.setText(R.string.state_off);
                tvState.setTextColor(ContextCompat.getColor(this, R.color.primary_light));
                btnStock.setText(R.string.btn_stock);
                btnStock.setEnabled(true);
                btnSecurity.setText(R.string.btn_security);
                btnSecurity.setEnabled(true);
                btnStop.setEnabled(false);
                break;

            case StateManager.ESTADO_STOCK:
                tvState.setText(R.string.state_stock);
                tvState.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
                btnStock.setText(R.string.btn_view_stock);
                btnStock.setEnabled(true);
                btnSecurity.setText(R.string.btn_security);
                btnSecurity.setEnabled(true);
                btnStop.setEnabled(true);
                break;

            case StateManager.ESTADO_SEGURIDAD:
                tvState.setText(R.string.state_security);
                tvState.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
                btnStock.setText(R.string.btn_stock);
                btnStock.setEnabled(false);
                btnSecurity.setText(R.string.btn_view_security);
                btnSecurity.setEnabled(true);
                btnStop.setEnabled(true);
                break;
        }

    }

    private void mostrarAlerta(String titulo, String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Ver", (d, w) -> navegarSegunEstado(StateManager.getEstadoActual()))
                .setNegativeButton("Cerrar", null)
                .show();
    }

}