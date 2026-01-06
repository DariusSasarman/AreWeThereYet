package com.example.arewethereyet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

public class ReachingDestinationActivity extends AppCompatActivity {

    private TextView distanceText;
    private TextView etaText;
    private TextView percentText;

    private Button finishButton;
    private Button pauseButton;

    private boolean paused = false;
    private Handler uiUpdateHandler;
    private static final long UI_UPDATE_INTERVAL_MS = 1000; // Update UI every second

    private final Runnable uiUpdater = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                updateUiSnapshot();
            }
            // Schedule next update
            uiUpdateHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reaching_destination);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        distanceText = findViewById(R.id.distanceText);
        etaText = findViewById(R.id.etaText);
        percentText = findViewById(R.id.percentText);
        finishButton = findViewById(R.id.finishButton);
        pauseButton = findViewById(R.id.pauseButton);

        // Initialize handler for periodic UI updates
        uiUpdateHandler = new Handler(Looper.getMainLooper());

        /* ---------------- Start foreground service ---------------- */
        Intent serviceIntent = new Intent(this, EtaTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        /* ---------------- Start periodic UI updates ---------------- */
        uiUpdateHandler.post(uiUpdater);

        /* ---------------- Button handlers ---------------- */
        finishButton.setOnClickListener(v -> {
            stopService(new Intent(this, EtaTrackingService.class));
            finish(); // back to MainActivity
        });

        pauseButton.setOnClickListener(v -> {
            paused = !paused;
            pauseButton.setText(paused ? "Resume" : "Pause");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if destination has been reached
        if (MainActivity.getState() == State.FINISHED_EXECUTION) {
            finish(); // Return to MainActivity which will launch DestinationReachedActivity
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop UI updates to prevent memory leaks
        if (uiUpdateHandler != null) {
            uiUpdateHandler.removeCallbacks(uiUpdater);
        }
    }

    /**
     * Pulls the latest known values from shared state and updates UI.
     * Called every second by the uiUpdater runnable.
     */
    private void updateUiSnapshot() {
        Double eta = MainActivity.getETA();
        Double distanceKm = MainActivity.getDistanceKm();
        Double progress = MainActivity.getProgressPercent();

        if (distanceKm != null) {
            distanceText.setText(String.format("Distance: %.1f km", distanceKm));
        }

        if (eta != null) {
            etaText.setText(String.format("ETA: %.1f min", eta));
        }

        if (progress != null) {
            percentText.setText(String.format("Progress: %.0f %%", progress));
        }

        // Check if state transitioned to finished (destination reached)
        if (MainActivity.getState() == State.FINISHED_EXECUTION) {
            finish();
        }
    }
}