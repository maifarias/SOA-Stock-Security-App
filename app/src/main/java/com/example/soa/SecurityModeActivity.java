package com.example.soa;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.Locale;

public class SecurityModeActivity extends AppCompatActivity {

    private TextView tvCell1Status, tvSecDetail1, tvCell2Status, tvSecDetail2, tvConnSec;
    private MaterialCardView card1, card2;
    private Button btnStop, btnAlarmOn, btnAlarmOff;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_mode);

        tvCell1Status = findViewById(R.id.tvSecCell1Status);
        tvSecDetail1 = findViewById(R.id.tvSecDetail1);
        tvCell2Status = findViewById(R.id.tvSecCell2Status);
        tvSecDetail2 = findViewById(R.id.tvSecDetail2);
        card1 = findViewById(R.id.cardSec1);
        card2 = findViewById(R.id.cardSec2);
        tvConnSec = findViewById(R.id.tvConnSec);
        btnStop = findViewById(R.id.btnStopSecurity);
        btnAlarmOn = findViewById(R.id.btnAlarmOn);
        btnAlarmOff = findViewById(R.id.btnAlarmOff);

        btnStop.setOnClickListener(v -> {
            ApiClient.getInstance().sendSecurity(this, false, new ApiClient.OkCallback() {
                @Override
                public void onOk(JSONObject resp) { finish(); }
                @Override
                public void onError(String msg) { Toast.makeText(SecurityModeActivity.this, msg, Toast.LENGTH_SHORT).show(); }
            });
        });

        btnAlarmOn.setOnClickListener(v -> sendAlarm("ON"));
        btnAlarmOff.setOnClickListener(v -> sendAlarm("OFF"));
    }

    private void sendAlarm(String val) {
        ApiClient.getInstance().sendAlarm(this, val, new ApiClient.OkCallback() {
            @Override
            public void onOk(JSONObject resp) { Toast.makeText(SecurityModeActivity.this, "Buzzer: " + val, Toast.LENGTH_SHORT).show(); }
            @Override
            public void onError(String msg) { Toast.makeText(SecurityModeActivity.this, msg, Toast.LENGTH_SHORT).show(); }
        });
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            ApiClient.getInstance().getState(SecurityModeActivity.this, new ApiClient.StateCallback() {
                @Override
                public void onState(JSONObject state) {
                    if (isFinishing()) return;
                    tvConnSec.setText("");
                    StateManager.updateFromBackend(state);
                    actualizarInterfaz();
                    // Quedarse mientras Security siga activado.
                    if (!StateManager.isSecurityOn()) {
                        finish();
                        return;
                    }
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }
                @Override
                public void onError(String msg) {
                    if (isFinishing()) return;
                    tvConnSec.setText(msg);
                    pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
                }
                @Override
                public void onEmpty() {
                    if (isFinishing()) return;
                    tvConnSec.setText("Esperando datos…");
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
        updateSecurityUI(StateManager.getShelf01(), tvCell1Status, tvSecDetail1, card1);
        updateSecurityUI(StateManager.getShelf02(), tvCell2Status, tvSecDetail2, card2);
    }

    private void updateSecurityUI(StateManager.ShelfData data, TextView tvStat, TextView tvDet, MaterialCardView card) {
        if (data.secure) {
            tvStat.setText(R.string.status_safe);
            tvStat.setTextColor(ContextCompat.getColor(this, R.color.green));
            card.setStrokeWidth(0);
        } else {
            tvStat.setText(R.string.status_alert);
            tvStat.setTextColor(ContextCompat.getColor(this, R.color.red));
            card.setStrokeWidth(4);
            card.setStrokeColor(ContextCompat.getColor(this, R.color.red));
        }

        tvDet.setText(String.format(Locale.US, "Baseline: %.0f g\nActual: %.0f g\nΔ: %.0f g", 
                data.baseline, data.current, data.delta));
    }
}