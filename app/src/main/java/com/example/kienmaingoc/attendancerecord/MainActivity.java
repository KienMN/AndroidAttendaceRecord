package com.example.kienmaingoc.attendancerecord;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button addNewUserButton;
    Button checkInButton;
    Button settingButton;
    public static final Float THRESHOLD = 70F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if the app could access the camera
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
        }

        // Check if the app could connect to the Internet
        if (checkSelfPermission(android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Can not access the Internet", Toast.LENGTH_LONG).show();
        }

        // Check if the app could access to read and write files
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("ko chay", "true");
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 500);
        }

        addNewUserButton = (Button) findViewById(R.id.addNewUserButton);
        checkInButton = (Button) findViewById(R.id.checkInButton);
        settingButton = (Button) findViewById(R.id.settingButton);

    }

    public void addNewUser(View view) {
        Intent i = new Intent(getApplicationContext(), AddNewUserActivity.class);
        startActivity(i);
    }

    public void setting(View view) {
        Intent i = new Intent(getApplicationContext(), SettingActivity.class);
        startActivity(i);
    }

    public void checkIn(View view) {
        Intent i = new Intent(getApplicationContext(), CheckInActivity.class);
        startActivity(i);
    }
}
