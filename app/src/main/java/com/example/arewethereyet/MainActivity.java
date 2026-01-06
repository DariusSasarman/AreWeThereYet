package com.example.arewethereyet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity {

    private static State state = State.START;
    private static Vehicle choice = Vehicle.NOT_YET;
    private static LatLng targetLocation = null;
    private static LatLng initialLocation = null;

    private static long lastTimeCurrentLocation = System.currentTimeMillis();
    private static LatLng currentLocation = null;
    private static double ETA = 100.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Handle state transitions one at a time
        if(state == State.START) {
            state = State.PICK_VEHICLE;
            Intent pickVehicle = new Intent(MainActivity.this, PickVehicleActivity.class);
            startActivity(pickVehicle);
            return; // Exit early to prevent multiple transitions
        }

        if(state == State.PICK_VEHICLE) {
            if(choice.equals(Vehicle.NOT_YET)) {
                // Still waiting for vehicle selection
                Intent pickVehicle = new Intent(MainActivity.this, PickVehicleActivity.class);
                startActivity(pickVehicle);
                return;
            } else {
                // Vehicle selected, move to next state
                Toast.makeText(this, "You picked: " + choice, Toast.LENGTH_SHORT).show();
                state = State.PICK_TARGET;
                Intent pickTargetLocation = new Intent(MainActivity.this, PickTargetLocationActivity.class);
                startActivity(pickTargetLocation);
                return;
            }
        }

        if(state == State.PICK_TARGET) {
            if(targetLocation == null || currentLocation == null) {
                // Still waiting for location selection
                Intent pickTargetLocation = new Intent(MainActivity.this, PickTargetLocationActivity.class);
                startActivity(pickTargetLocation);
                return;
            } else {
                // Location selected, store initial location and start tracking
                initialLocation = currentLocation;
                state = State.REACHING_DESTINATION;
                Intent awaitReachDestination = new Intent(MainActivity.this, ReachingDestinationActivity.class);
                startActivity(awaitReachDestination);
                return;
            }
        }

        if(state == State.REACHING_DESTINATION) {
            // We're in tracking mode - only launch the activity if it's not already running
            // This prevents re-launching when coming back from other activities
            return;
        }

        if(state == State.FINISHED_EXECUTION) {
            // Reset state immediately to prevent loop
            state = State.START;
            choice = Vehicle.NOT_YET;
            targetLocation = null;
            initialLocation = null;
            currentLocation = null;
            ETA = 100.0;

            // Then show completion screen
            Intent callAlarm = new Intent(MainActivity.this, DestinationReachedActivity.class);
            startActivity(callAlarm);
            return;
        }
    }

    public static void setChoice(Vehicle choice) {
        MainActivity.choice = choice;
    }

    public static void setTargetLocation(LatLng targetLocation) {
        MainActivity.targetLocation = targetLocation;
    }

    public static void setCurrentLocation(LatLng currentLocation) {
        MainActivity.currentLocation = currentLocation;
        updateLastTimeCurrentLocation();
    }

    public static LatLng getTargetLocation() {
        return MainActivity.targetLocation;
    }

    public static LatLng getCurrentLocation() {
        return MainActivity.currentLocation;
    }

    public static void setETA(double ETA) {
        MainActivity.ETA = ETA;
    }

    public static Double getETA() {
        return ETA;
    }

    public static Vehicle getChoice() {
        return choice;
    }

    public static long getLastTimeCurrentLocation() {
        return lastTimeCurrentLocation;
    }

    public static void updateLastTimeCurrentLocation() {
        lastTimeCurrentLocation = System.currentTimeMillis();
    }

    public static Double getDistanceKm() {
        if (currentLocation == null || targetLocation == null) {
            return null;
        }
        float[] result = new float[1];
        android.location.Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                targetLocation.latitude, targetLocation.longitude,
                result
        );
        return result[0] / 1000.0;
    }

    public static Double getProgressPercent() {
        if (currentLocation == null || targetLocation == null || initialLocation == null) {
            return null;
        }

        float[] totalDistance = new float[1];
        android.location.Location.distanceBetween(
                initialLocation.latitude, initialLocation.longitude,
                targetLocation.latitude, targetLocation.longitude,
                totalDistance
        );

        float[] remainingDistance = new float[1];
        android.location.Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                targetLocation.latitude, targetLocation.longitude,
                remainingDistance
        );

        if (totalDistance[0] == 0) {
            return 100.0;
        }

        double traveled = totalDistance[0] - remainingDistance[0];
        return Math.max(0, Math.min(100, (traveled / totalDistance[0]) * 100.0));
    }

    public static void onDestinationReached() {
        state = State.FINISHED_EXECUTION;
    }

    public static State getState() {
        return state;
    }
}