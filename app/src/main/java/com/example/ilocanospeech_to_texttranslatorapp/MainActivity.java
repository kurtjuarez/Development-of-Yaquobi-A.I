package com.example.ilocanospeech_to_texttranslatorapp;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.ilocanospeech_to_texttranslatorapp.fragments.AboutUsPage;
import com.example.ilocanospeech_to_texttranslatorapp.fragments.HistoryPage;
import com.example.ilocanospeech_to_texttranslatorapp.fragments.HomePage;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // For the BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Set HomePage as the default fragment to show up, basically the landing page
        Fragment selectedFragment = new HomePage();

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();

        // Assume this Activity is the current activity, check record permission
        checkRecordPermission();
    }
    // Permission for Recorder
    private void checkRecordPermission() {
        int permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Record permission is granted");
        } else {
            Log.d(TAG, "Requesting record permission");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Record permission is granted");
        } else {
            Log.d(TAG, "Record permission is not granted");
        }
    }
    // For the selection from the BotNavBar which proceeds to certain fragments depending on the conditions of the if-else statement below
    private NavigationBarView.OnItemSelectedListener navListener = item -> {
        int itemId = item.getItemId();

        Fragment selectedFragment = null;

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomePage();
        } else if (itemId == R.id.nav_about_us) {
            selectedFragment = new AboutUsPage();
        } else if (itemId == R.id.nav_history) {
            selectedFragment = new HistoryPage();
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();

        return true;
    };
}