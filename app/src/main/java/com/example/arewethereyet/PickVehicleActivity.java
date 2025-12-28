package com.example.arewethereyet;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PickVehicleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pick_vehicle);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button car = findViewById(R.id.PickCarVehicle);
        Button train = findViewById(R.id.pickTrainVehicle);
        Button none = findViewById(R.id.pickNoVehicle);

        car.setOnClickListener(v -> {
            MainActivity.setChoice(Vehicle.CAR);
            finish();
        });

        train.setOnClickListener(v-> {
            MainActivity.setChoice(Vehicle.TRAIN);
            finish();
        });

        none.setOnClickListener( v-> {
            MainActivity.setChoice(Vehicle.PLANE);
            finish();
        });
    }
}