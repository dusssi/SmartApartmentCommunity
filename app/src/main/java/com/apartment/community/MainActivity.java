package com.apartment.community;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View content = findViewById(android.R.id.content);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(250);
        content.startAnimation(fade);

        setupDemoButton(R.id.btnAnnouncements, "Announcements");
        setupDemoButton(R.id.btnMaintenance, "Maintenance & Complaints");
        setupDemoButton(R.id.btnVisitors, "Visitor Management");
        setupDemoButton(R.id.btnEmergency, "Emergency Contacts");
    }

    private void setupDemoButton(int id, String label) {
        Button button = findViewById(id);
        button.setOnClickListener(v ->
            Toast.makeText(
                MainActivity.this,
                label + " module will be implemented next.",
                Toast.LENGTH_SHORT
            ).show()
        );
    }
}
