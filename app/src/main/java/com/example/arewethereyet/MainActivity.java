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
    private static Vehicle choice = Vehicle.NOTYET;
    private static LatLng targetLocation = null;
    private static LatLng currentLocation = null;

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
        switch (state)
        {
            case START:
                state = State.PICK_VEHICLE;
                /// Intended fall-through
            case PICK_VEHICLE:
                state = State.PICK_TARGET;
                if(choice.equals(Vehicle.NOTYET))
                {
                    state = State.PICK_VEHICLE;
                    Intent pickVehicle = new Intent(MainActivity.this,PickVehicleActivity.class);
                    startActivity(pickVehicle);
                }
                else {
                    Toast.makeText(this, "You picked : " + choice, Toast.LENGTH_SHORT).show();
                }
                break;
            case PICK_TARGET:
                state = State.REACHING_DESTINATION;
                Intent pickTargetLocation = new Intent(MainActivity.this, PickTargetLocationActivity.class);
                startActivity(pickTargetLocation);
                if(targetLocation.equals(null) || currentLocation.equals(null))
                {
                    state = State.PICK_TARGET;
                }
                break;
            case REACHING_DESTINATION:

                break;
            case FINISHED_EXECUTION:

                break;
            default:
                state = State.START;
                break;
        }

    }
    public static void setChoice(Vehicle choice) {
        MainActivity.choice = choice;
    }

    public static void setTargetLocation(LatLng targetLocation)
    {
        MainActivity.targetLocation = targetLocation;
    }

    public static void setCurrentLocation(LatLng currentLocation) {
        MainActivity.currentLocation = currentLocation;
    }

}