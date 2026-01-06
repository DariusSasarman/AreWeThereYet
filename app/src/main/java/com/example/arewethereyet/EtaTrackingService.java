package com.example.arewethereyet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;

public class EtaTrackingService extends Service {

    private static final String CHANNEL_ID = "eta_tracking";
    private static final int NOTIFICATION_ID = 1;

    private static final long CHECK_INTERVAL_MS = 20_000; // Check every 20 seconds
    private static final double ETA_THRESHOLD_MIN = 10.0; // Alert at 10 minutes

    private Handler handler;
    private boolean alarmPlayed = false;
    private Ringtone ringtone;

    private LatLng prevLocation = null;
    private long prevTime = 0;

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            LatLng current = MainActivity.getCurrentLocation();
            LatLng target = MainActivity.getTargetLocation();

            if (current != null && target != null && !alarmPlayed) {
                double eta = estimateEtaMinutesDynamic(current, target);

                // Update shared state so UI can read it
                MainActivity.setETA(eta);

                // Update notification with current ETA
                updateNotification(eta);

                if (eta <= ETA_THRESHOLD_MIN) {
                    alarmPlayed = true;
                    playAlarm();

                    // Trigger state transition
                    MainActivity.onDestinationReached();

                    // Launch DestinationReachedActivity where user can dismiss alarm
                    Intent alarmIntent = new Intent(EtaTrackingService.this, DestinationReachedActivity.class);
                    alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(alarmIntent);

                    // Keep service running until user dismisses from DestinationReachedActivity
                    return;
                }
            }

            // Continue checking
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(null));

        handler.post(checker);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if this is a stop command from DestinationReachedActivity
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            stopAlarm();
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checker);
        stopAlarm();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ---------------- ETA logic ---------------- */

    private double estimateEtaMinutesDynamic(LatLng current, LatLng target) {
        double distanceToTarget = distanceMeters(current, target);
        long currentTime = System.currentTimeMillis();
        double speedMps;

        if (prevLocation != null && prevTime > 0) {
            double distanceMoved = distanceMeters(prevLocation, current);
            long timeDiff = currentTime - prevTime;
            speedMps = timeDiff > 0 ? distanceMoved / (timeDiff / 1000.0) : 1.0;
            if (speedMps < 0.1) speedMps = 1.0; // Fallback for very slow/stopped movement
        } else {
            // Use default speeds based on vehicle type
            switch (MainActivity.getChoice()) {
                case CAR: speedMps = 13.8; break; // ~50 km/h
                case TRAIN: speedMps = 10.0; break; // ~36 km/h
                default: speedMps = 1.4; // Walking speed ~5 km/h
            }
        }

        prevLocation = current;
        prevTime = currentTime;

        return (distanceToTarget / speedMps) / 60.0;
    }

    private double distanceMeters(LatLng a, LatLng b) {
        float[] result = new float[1];
        android.location.Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                result
        );
        return result[0];
    }

    /* ---------------- Alarm ---------------- */

    private void playAlarm() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null)
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
        if (ringtone != null) {
            ringtone.play();
        }
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    /* ---------------- Notification ---------------- */

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ETA Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(Double eta) {
        String text = eta != null
                ? String.format("ETA: %.1f minutes", eta)
                : "Tracking destination…";

        // Create intent to open DestinationReachedActivity if alarm is playing
        Intent notificationIntent = new Intent(this,
                alarmPlayed ? DestinationReachedActivity.class : ReachingDestinationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Are We There Yet?")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(double eta) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(eta));
        }
    }
}