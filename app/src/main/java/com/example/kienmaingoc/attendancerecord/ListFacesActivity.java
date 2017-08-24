package com.example.kienmaingoc.attendancerecord;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ListFacesActivity extends AppCompatActivity {

    ListView listFacesListView;
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_faces);
        listFacesListView = (ListView) findViewById(R.id.listFacesListView);
        if (SettingActivity.facesInCollection.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No face available", Toast.LENGTH_LONG).show();
        } else {
            arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, SettingActivity.facesInCollection);
            listFacesListView.setAdapter(arrayAdapter);
        }
    }
}
