package com.example.arewethereyet;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

public class EtaTrackingService extends Service {

    private static final String CHANNEL_ID = "eta_tracking";
    private static final int NOTIFICATION_ID = 1;

    private static final long LOCATION_UPDATE_INTERVAL_MS = 5_000; // Request location every 5 seconds
    private static final long FASTEST_INTERVAL_MS = 3_000; // Fastest update rate
    private static final double ETA_THRESHOLD_MIN = 10.0; // Alert at 10 minutes

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler handler;
    private boolean alarmPlayed = false;
    private MediaPlayer mediaPlayer;

    private LatLng prevLocation = null;
    private long prevTime = 0;
    private LatLng currentTrackedLocation = null;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(null));

        setupLocationTracking();
    }

    private void setupLocationTracking() {
        // Create location request
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .build();

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    onLocationUpdate(location);
                }
            }
        };

        // Start location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void onLocationUpdate(Location location) {
        currentTrackedLocation = new LatLng(location.getLatitude(), location.getLongitude());

        // Update MainActivity's current location
        MainActivity.setCurrentLocation(currentTrackedLocation);

        // Check if we should trigger the alarm
        checkDestinationProximity();
    }

    private void checkDestinationProximity() {
        LatLng target = MainActivity.getTargetLocation();

        if (currentTrackedLocation != null && target != null && !alarmPlayed) {
            double eta = estimateEtaMinutesDynamic(currentTrackedLocation, target);

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
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if ("STOP_ALARM".equals(action)) {
                // Stop the alarm and service
                stopAlarm();
                stopSelf();
                return START_NOT_STICKY;
            } else if ("TRIGGER_ALARM".equals(action)) {
                // Manually trigger the alarm (from Finish button)
                if (!alarmPlayed) {
                    alarmPlayed = true;
                    playAlarm();
                }
                return START_STICKY;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Stop location updates
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
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
        try {
            // Get the alarm URI
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            // Release any existing MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            // Create and configure MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), alarmUri);

            // Use ALARM audio stream
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            // Set to loop continuously
            mediaPlayer.setLooping(true);

            // Prepare and start
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Also set the volume to max for alarm stream
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

                // If volume is too low, notify but don't change it (user preference)
                if (currentVolume < maxVolume / 3) {
                    // Volume is quite low, but we respect user settings
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: try using Ringtone instead
            try {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.setLooping(true);
                    }
                    ringtone.play();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
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