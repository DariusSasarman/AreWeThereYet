package com.example.arewethereyet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

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
        if(!CurrentTripTracker.getChoice().equals(Vehicle.NOTYET))
        {
            Toast.makeText(this, "You picked : " + CurrentTripTracker.getChoice(), Toast.LENGTH_SHORT).show();
        }
        else {
            Intent pickVehicle = new Intent(MainActivity.this,PickVehicleActivity.class);
            startActivity(pickVehicle);
        }
    }
}