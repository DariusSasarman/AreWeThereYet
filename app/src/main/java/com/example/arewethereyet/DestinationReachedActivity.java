package com.example.arewethereyet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DestinationReachedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_destination_reached);

        // Handle window insets for proper padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the button and set its click listener
        Button buttonThankYou = findViewById(R.id.buttonThankYou);
        buttonThankYou.setOnClickListener(v -> {
            // CRITICAL: Stop the alarm by sending stop command to service
            Intent stopIntent = new Intent(this, EtaTrackingService.class);
            stopIntent.setAction("STOP_ALARM");
            startService(stopIntent);

            // Close this activity
            finish();
        });
    }
}