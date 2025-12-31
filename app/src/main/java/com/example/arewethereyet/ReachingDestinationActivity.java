package com.example.arewethereyet;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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

import com.google.android.gms.maps.model.LatLng;

public class ReachingDestinationActivity extends AppCompatActivity {

    private static final long CHECK_INTERVAL_MS = 2000; // 2 seconds
    private static final double ETA_THRESHOLD_MIN = 10.0;

    private Handler handler;
    private TextView distanceText, etaText, percentText;
    private Button finishButton, pauseButton;

    private boolean alarmPlayed = false;
    private boolean paused = false;
    private Ringtone ringtone;

    private LatLng prevLocation = null;
    private long prevTime = 0;

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            if (paused) {
                handler.postDelayed(this, CHECK_INTERVAL_MS);
                return;
            }

            LatLng current = MainActivity.getCurrentLocation();
            LatLng target = MainActivity.getTargetLocation();

            if (current != null && target != null) {

                double distance = distanceMeters(current, target);
                double eta = estimateEtaMinutesDynamic(current, target);
                double totalDistance = distanceMeters(prevLocation != null ? prevLocation : current, target);
                double percent = totalDistance > 0 ? (1.0 - distance / totalDistance) * 100.0 : 0.0;

                MainActivity.setETA(eta);

                distanceText.setText(String.format("Distance: %.1f km", distance / 1000.0));
                etaText.setText(String.format("ETA: %.1f min", eta));
                percentText.setText(String.format("Progress: %.0f %%", percent));

                if (eta <= ETA_THRESHOLD_MIN && !alarmPlayed) {
                    alarmPlayed = true;
                    playAlarm();
                    finishButton.setEnabled(true);
                    return; // stop checking until user finishes
                }
            }

            handler.postDelayed(this, CHECK_INTERVAL_MS);
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

        finishButton.setEnabled(false);
        finishButton.setOnClickListener(v -> {
            stopAlarm();
            finish(); // return to MainActivity
        });

        pauseButton.setOnClickListener(v -> {
            paused = !paused;
            pauseButton.setText(paused ? "Resume" : "Pause");
        });

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(checker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(checker);
    }

    /* ------------------ helpers ------------------ */

    private double distanceMeters(LatLng a, LatLng b) {
        float[] result = new float[1];
        android.location.Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                result
        );
        return result[0];
    }

    private double estimateEtaMinutesDynamic(LatLng current, LatLng target) {
        double distanceToTarget = distanceMeters(current, target);
        long currentTime = System.currentTimeMillis();
        double speedMps;

        if (prevLocation != null && prevTime > 0) {
            double distanceMoved = distanceMeters(prevLocation, current);
            long timeDiff = currentTime - prevTime; // milliseconds
            speedMps = timeDiff > 0 ? (distanceMoved / (timeDiff / 1000.0)) : 1.0;
            if (speedMps < 0.1) speedMps = 1.0; // avoid zero
        } else {
            // fallback: fixed speeds based on vehicle
            switch (MainActivity.getChoice()) {
                case CAR: speedMps = 13.8; break;      // ~50 km/h
                case TRAIN: speedMps = 10.0; break;    // ~36 km/h realistic slower train
                case NONE:
                case NOT_YET:
                default: speedMps = 1.4; break;       // walking
            }
        }

        prevLocation = current;
        prevTime = currentTime;

        return (distanceToTarget / speedMps) / 60.0; // minutes
    }

    private void playAlarm() {
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alert == null) alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }
}
