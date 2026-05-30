package com.example.soa;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private Button btnStock, btnSecurity, btnStop;
    private TextView tvState, tvLastUpdate;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvState = findViewById(R.id.tvState);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        btnStock = findViewById(R.id.btnStock);
        btnSecurity = findViewById(R.id.btnSecurity);
        btnStop = findViewById(R.id.btnStop);

        // Cada modo es un toggle independiente: si ya está activado, el botón ENTRA a su
        // pantalla; si no, lo ACTIVA. Ambos pueden estar activados a la vez.
        btnStock.setOnClickListener(v -> {
            if (StateManager.isStockOn()) {
                startActivity(new Intent(this, StockModeActivity.class));
            } else {
                sendStock(true);
            }
        });

        btnSecurity.setOnClickListener(v -> {
            if (StateManager.isSecurityOn()) {
                startActivity(new Intent(this, SecurityModeActivity.class));
            } else {
                sendSecurity(true);
            }
        });

        btnStop.setOnClickListener(v -> {
            sendStock(false);
            sendSecurity(false);
        });
    }

    private void sendStock(boolean on) {
        ApiClient.getInstance().sendStock(this, on, cmdCallback(on ? "Stock activado" : "Stock desactivado"));
    }

    private void sendSecurity(boolean on) {
        ApiClient.getInstance().sendSecurity(this, on, cmdCallback(on ? "Security activado" : "Security desactivado"));
    }

    private ApiClient.OkCallback cmdCallback(String okMsg) {
        return new ApiClient.OkCallback() {
            @Override
            public void onOk(JSONObject resp) {
                Toast.makeText(MainActivity.this, okMsg, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String msg) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            ApiClient.getInstance().getState(MainActivity.this, new ApiClient.StateCallback() {
                @Override
                public void onState(JSONObject state) {
                    StateManager.updateFromBackend(state);
                    actualizarInterfaz();
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }

                @Override
                public void onError(String msg) {
                    tvState.setText(msg);
                    tvState.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }

                @Override
                public void onEmpty() {
                    tvState.setText("Esperando datos…");
                    tvState.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.primary_light));
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
        String availability = StateManager.getAvailability();
        long lastUpd = StateManager.getLastUpdated();

        if (lastUpd > 0) {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(lastUpd));
            tvLastUpdate.setText(getString(R.string.last_updated_format, time));
        }

        if (availability.equals("offline")) {
            tvState.setText("SERVIDOR OFFLINE");
            tvState.setTextColor(ContextCompat.getColor(this, R.color.red));
            return;
        }

        boolean stockOn = StateManager.isStockOn();
        boolean securityOn = StateManager.isSecurityOn();
        String active = StateManager.getEstadoActual();   // modo que corre por prioridad

        // Texto de estado: modo efectivo (Security gana sobre Stock).
        switch (active) {
            case StateManager.ESTADO_SEGURIDAD:
                tvState.setText(R.string.state_security);
                tvState.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
                break;
            case StateManager.ESTADO_STOCK:
                tvState.setText(R.string.state_stock);
                tvState.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
                break;
            default:
                tvState.setText(R.string.state_off);
                tvState.setTextColor(ContextCompat.getColor(this, R.color.primary_light));
                break;
        }

        // Cada botón refleja su propio toggle (ambos pueden estar ON a la vez):
        // ON -> "Ver ...", OFF -> "Activar ...".
        btnStock.setText(stockOn ? R.string.btn_view_stock : R.string.btn_stock);
        btnSecurity.setText(securityOn ? R.string.btn_view_security : R.string.btn_security);
        btnStop.setEnabled(stockOn || securityOn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Configuración");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        showConfigDialog();
        return true;
    }

    private void showConfigDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etUrl = new EditText(this);
        etUrl.setHint("URL (http://IP:1880)");
        etUrl.setText(ApiClient.getInstance().getBaseUrl(this));
        layout.addView(etUrl);

        final EditText etId = new EditText(this);
        etId.setHint("Device ID");
        etId.setText(ApiClient.getInstance().getDeviceId(this));
        layout.addView(etId);

        new AlertDialog.Builder(this)
                .setTitle("Ajustes de API")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    ApiClient.getInstance().saveSettings(this, etUrl.getText().toString(), etId.getText().toString());
                    Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}