package com.example.soa;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.Locale;

public class StockModeActivity extends AppCompatActivity {

    private TextView tvCell1Value, tvCell1Status, tvCell2Value, tvCell2Status, tvConnStock;
    private Button btnStop;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_mode);

        tvCell1Value = findViewById(R.id.tvCell1Value);
        tvCell1Status = findViewById(R.id.tvCell1Status);
        tvCell2Value = findViewById(R.id.tvCell2Value);
        tvCell2Status = findViewById(R.id.tvCell2Status);
        tvConnStock = findViewById(R.id.tvConnStock);
        btnStop = findViewById(R.id.btnStopStock);

        btnStop.setOnClickListener(v -> {
            ApiClient.getInstance().sendStock(this, false, new ApiClient.OkCallback() {
                @Override
                public void onOk(JSONObject resp) { finish(); }
                @Override
                public void onError(String msg) { Toast.makeText(StockModeActivity.this, msg, Toast.LENGTH_SHORT).show(); }
            });
        });
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            ApiClient.getInstance().getState(StockModeActivity.this, new ApiClient.StateCallback() {
                @Override
                public void onState(JSONObject state) {
                    if (isFinishing()) return;
                    tvConnStock.setText("");
                    StateManager.updateFromBackend(state);
                    actualizarInterfaz();
                    // Quedarse mientras Stock siga activado (aunque Security corra por prioridad).
                    if (!StateManager.isStockOn()) {
                        finish();
                        return;
                    }
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }
                @Override
                public void onError(String msg) {
                    if (isFinishing()) return;
                    tvConnStock.setText(msg);
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }
                @Override
                public void onEmpty() {
                    if (isFinishing()) return;
                    tvConnStock.setText("Esperando datos…");
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void actualizarInterfaz() {
        updateShelfUI(StateManager.getShelf01(), tvCell1Value, tvCell1Status);
        updateShelfUI(StateManager.getShelf02(), tvCell2Value, tvCell2Status);
    }

    private void updateShelfUI(StateManager.ShelfData data, TextView tvVal, TextView tvStat) {
        tvVal.setText(String.format(Locale.US, "Peso: %.1f g", data.weight));
        if (data.available) {
            tvStat.setText("DISPONIBLE");
            tvStat.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            tvStat.setText("AGOTADO");
            tvStat.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }
}