package com.example.psh;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class SettingsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private savingStates state;
    private boolean is_add;
    private EditText name_box;
    private EditText des_box;
    private CheckBox sound_cb;
    private CheckBox cache_cb;
    private CheckBox active_cb;
    private boolean is_tracking;
    private boolean geofence_exist;
    private LatLng latLng = null;
    private GeofencingClient geofencingClient;

    private GoogleMap mMap;

    private float GEOFENCE_RADIUS = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        Intent intent = getIntent();
        String op = intent.getStringExtra("option");
        state = (savingStates)intent.getSerializableExtra("state");
        name_box = findViewById(R.id.name_box);
        des_box = findViewById(R.id.des_box);
        sound_cb = findViewById(R.id.sound_cb);
        cache_cb = findViewById(R.id.cache_cb);
        active_cb = findViewById(R.id.active_cb);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        if (op.equals("add"))
        {
            is_add = true;
            is_tracking = false;
        }
        else
        {
            is_add = false;
            is_tracking = false;
            name_box.setText(state.name);
            des_box.setText(state.description);
            sound_cb.setChecked(state.sound_save);
            cache_cb.setChecked(state.cache_save);
            active_cb.setChecked(state.is_active);
            is_tracking = state.is_tracking;
            geofence_exist = state.is_tracking;
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //save_btn onclick
    @SuppressLint("MissingPermission")
    public void save_state(View view) {
        Intent reply_intent = new Intent();
        state.name = name_box.getText().toString();
        state.description = des_box.getText().toString();
        state.sound_save = sound_cb.isChecked();
        state.cache_save = cache_cb.isChecked();
        state.is_active = active_cb.isChecked();
        state.is_tracking = is_tracking;
        state.geofence_exist = geofence_exist;
        state.lat = latLng.latitude;
        state.lng = latLng.longitude;

        reply_intent.putExtra("reply_state", state);
        if(is_add)
        {
            setResult(1,reply_intent);
        }
        else
        {
            setResult(2,reply_intent);
        }
        finish();
    }

    public void delete_state(View view) {
        Intent reply_intent = new Intent();
        if(is_add)
        {
            setResult(4,reply_intent);
        }
        else
        {
            reply_intent.putExtra("reply_state", state);
            setResult(3,reply_intent);
        }
        finish();
    }

    public void back_btn(View view) {
        Intent reply_intent = new Intent();
        setResult(4,reply_intent);
        finish();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        mMap.setMyLocationEnabled(true);
        if(state.is_tracking)
        {
            latLng = new LatLng(state.lat, state.lng);
            addMarker(latLng);
            addCircle(latLng, GEOFENCE_RADIUS);
        }
        else
        {
            latLng = new LatLng(36.013, 129.322);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

        mMap.setOnMapLongClickListener(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapLongClick(LatLng latLng) {
        if(is_tracking){
            mMap.clear();
        }
        else{
            this.latLng = latLng;
            addMarker(latLng);
            addCircle(latLng, GEOFENCE_RADIUS);
        }
        is_tracking = !is_tracking;
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0,0));
        circleOptions.fillColor(Color.argb(64, 255, 0,0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }
}