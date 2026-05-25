package com.example.soa;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StockModeActivity extends AppCompatActivity implements MqttManager.MqttListener {

    private Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stock_mode);

        btnStop = findViewById(R.id.btnStopStock);

        btnStop.setOnClickListener(v -> {
            MqttManager.getInstance().publicar("soa/grupol5/comando", "STOCK_OFF");
            StateManager.setEstadoActual(StateManager.ESTADO_VIRGEN);
            finish();
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
                case "soa/grupol5/stock/sensor01":
                    // actualizar tvSensor1
                    break;
                case "soa/grupol5/stock/sensor02":
                    // actualizar tvSensor2
                    break;
                case "soa/grupol5/estado":
                    if (payload.equals(StateManager.ESTADO_VIRGEN)) finish();
                    break;
            }
        });
    }

    @Override
    public void onConexionPerdida() { }

}