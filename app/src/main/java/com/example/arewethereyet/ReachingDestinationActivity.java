package com.example.arewethereyet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ReachingDestinationActivity extends AppCompatActivity {

    private TextView distanceText;
    private TextView etaText;
    private TextView percentText;
    private ProgressBar progressBar;

    private Button finishButton;
    private Button pauseButton;

    private boolean paused = false;
    private Handler uiUpdateHandler;
    private static final long UI_UPDATE_INTERVAL_MS = 1000; // Update UI every second
    private static final int PERMISSION_REQUEST_CODE = 2001;

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
        progressBar = findViewById(R.id.progressBar);
        finishButton = findViewById(R.id.finishButton);
        pauseButton = findViewById(R.id.pauseButton);

        // Enable the finish button
        finishButton.setEnabled(true);

        // Initialize handler for periodic UI updates
        uiUpdateHandler = new Handler(Looper.getMainLooper());

        /* ---------------- Button handlers ---------------- */
        finishButton.setOnClickListener(v -> {
            // Trigger the alarm manually
            Intent serviceIntent = new Intent(this, EtaTrackingService.class);
            serviceIntent.setAction("TRIGGER_ALARM");
            startService(serviceIntent);

            // Update state to finished
            MainActivity.onDestinationReached();
            finish(); // Close this activity
        });

        pauseButton.setOnClickListener(v -> {
            paused = !paused;
            pauseButton.setText(paused ? "Resume" : "Pause");
        });

        // Check and request all necessary permissions
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        // List of all required permissions
        String[] requiredPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            requiredPermissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            // Android 12 and below
            requiredPermissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        // Check which permissions are missing
        boolean allGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            // Request all permissions
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
        } else {
            // All permissions granted, start service and UI updates
            startTrackingService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
                startTrackingService();
            } else {
                Toast.makeText(this, "Some permissions were denied. The app may not work correctly.", Toast.LENGTH_LONG).show();
                // Still try to start the service even if some permissions are denied
                startTrackingService();
            }
        }
    }

    private void startTrackingService() {
        /* ---------------- Start foreground service ---------------- */
        Intent serviceIntent = new Intent(this, EtaTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        /* ---------------- Start periodic UI updates ---------------- */
        uiUpdateHandler.post(uiUpdater);
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
        } else {
            distanceText.setText("Distance: -- km");
        }

        if (eta != null) {
            etaText.setText(String.format("ETA: %.1f min", eta));
        } else {
            etaText.setText("ETA: -- min");
        }

        if (progress != null) {
            percentText.setText(String.format("Progress: %.0f %%", progress));
            progressBar.setProgress(progress.intValue());
        } else {
            percentText.setText("Progress: -- %");
            progressBar.setProgress(0);
        }

        // Check if state transitioned to finished (destination reached)
        if (MainActivity.getState() == State.FINISHED_EXECUTION) {
            finish(); // This will return to MainActivity, which will launch DestinationReachedActivity
        }
    }
}