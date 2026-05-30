package com.example.soa;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StockModeActivity extends AppCompatActivity implements MqttManager.MqttListener {

    private TextView tvCell1Value, tvCell1Status, tvCell2Value, tvCell2Status;
    private Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_mode);

        tvCell1Value = findViewById(R.id.tvCell1Value);
        tvCell1Status = findViewById(R.id.tvCell1Status);
        tvCell2Value = findViewById(R.id.tvCell2Value);
        tvCell2Status = findViewById(R.id.tvCell2Status);
        btnStop = findViewById(R.id.btnStopStock);

        btnStop.setOnClickListener(v -> MqttManager.getInstance().publicar("soa/grupol5/comando", "STOCK_OFF"));

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
                    actualizarSensor(tvCell1Value, tvCell1Status, payload);
                    break;
                case "soa/grupol5/stock/sensor02":
                    actualizarSensor(tvCell2Value, tvCell2Status, payload);
                    break;
                case "soa/grupol5/estado":
                    StateManager.setEstadoActual(payload);
                    if (payload.equals(StateManager.ESTADO_VIRGEN)) finish();
                    break;
            }
        });
    }

    private void actualizarSensor(TextView tvValue, TextView tvStatus, String pesoStr) {
        try {
            float peso = Float.parseFloat(pesoStr);
            tvValue.setText(String.format(Locale.US, "%.1f g", peso));
            
            if (peso < 100) { // Umbral de stock bajo
                tvStatus.setText(R.string.status_low);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
            } else {
                tvStatus.setText(R.string.status_ok);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
            }
        } catch (NumberFormatException e) {
            tvValue.setText(pesoStr);
        }
    }

    @Override
    public void onConexionPerdida() {
        runOnUiThread(() ->
            Toast.makeText(this, "Conexión perdida", Toast.LENGTH_SHORT).show()
        );
    }
}