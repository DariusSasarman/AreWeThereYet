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
    private static LatLng initialLocation = null; // Store starting location

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
        if(state == State.START)
        {
            state = State.PICK_VEHICLE;
        }
        if(state == State.PICK_VEHICLE)
        {
            state = State.PICK_TARGET;
            if(choice.equals(Vehicle.NOT_YET))
            {
                state = State.PICK_VEHICLE;
                Intent pickVehicle = new Intent(MainActivity.this,PickVehicleActivity.class);
                startActivity(pickVehicle);
                recreate();
            }
            else {
                Toast.makeText(this, "You picked : " + choice, Toast.LENGTH_SHORT).show();
            }
        }
        if(state == State.PICK_TARGET)
        {
            state = State.REACHING_DESTINATION;
            Intent pickTargetLocation = new Intent(MainActivity.this, PickTargetLocationActivity.class);
            startActivity(pickTargetLocation);
            if(targetLocation == null || currentLocation == null)
            {
                state = State.PICK_TARGET;
                recreate();
            }
            else {
                // Store initial location when journey starts
                initialLocation = currentLocation;
            }
        }

        if(state == State.REACHING_DESTINATION)
        {
            state = State.FINISHED_EXECUTION;
            Intent awaitReachDestination = new Intent(MainActivity.this,ReachingDestinationActivity.class);
            startActivity(awaitReachDestination);
        }

        if(state == State.FINISHED_EXECUTION)
        {
            state = State.START;
            Intent callAlarm = new Intent(MainActivity.this,DestinationReachedActivity.class);
            startActivity(callAlarm);
            recreate();
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

    // Calculate distance to target in kilometers
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
        return result[0] / 1000.0; // Convert meters to km
    }

    // Calculate progress percentage
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

    // Trigger state transition when destination reached
    public static void onDestinationReached() {
        state = State.FINISHED_EXECUTION;
    }

    public static State getState() {
        return state;
    }
}