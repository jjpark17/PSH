package com.example.psh;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ACCESS_NOTIFICATION_POLICY = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static MainActivity instance;
    private ListView listView;
    private int cur_pos = -1;
    public int run_id = -1;
    private int id_cnt = 0;

    private Geofence geofence = null;
    private GeofencingClient geofencingClient;
    private GeofencingRequest.Builder builder;
    private PendingIntent geofencePendingIntent;
    private float GEOFENCE_RADIUS = 150;

    public CustomAdapter adapter = new CustomAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        listView = findViewById(R.id.show_list);
        restore_info();
        instance = this;
        geofencingClient = LocationServices.getGeofencingClient(this);
    }

    public boolean checkAccessibilityPermissions(){
        AccessibilityManager accessibilityManager =
                (AccessibilityManager)getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> list =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);

        Log.d("service_test", "size : " + list.size());
        Log.d("service", list.toString());
        for(int i = 0; i < list.size(); i++){
            AccessibilityServiceInfo info = list.get(i);
            if(info.getResolveInfo().serviceInfo.packageName.equals(getApplication().getPackageName())){
                return true;
            }
        }
        return false;
    }

    public void setAccessibilityPermissions(){
        AlertDialog.Builder permissionDialog = new AlertDialog.Builder(this);
        permissionDialog.setTitle("접근성 권한 설정");
        permissionDialog.setMessage("앱을 사용하기 위해 접근성 권한이 필요합니다.");
        permissionDialog.setPositiveButton("허용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
        }).create().show();
    }

    void restore_info()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        run_id = sharedPref.getInt("run", -1);
        id_cnt = sharedPref.getInt("id_cnt", 0);
        String json = sharedPref.getString("adap", "none");
        adapter.read_json(json);
        listView.setAdapter(adapter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView,
                                    View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                cur_pos = position;
                intent.putExtra("option", "modify");
                savingStates selected_item = (savingStates)adapterView.getItemAtPosition(position);
                intent.putExtra("state", selected_item);
                startActivityForResult(intent, 1);
            }
        });
    }

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent reply){
        super.onActivityResult(requestCode, resultCode, reply);

        if (requestCode == 1) {
            if (resultCode == 1) {
                savingStates state = (savingStates) reply.getSerializableExtra("reply_state");
                //checkbox 동적 할당? 및 정보 긁어와서 채워넣기
                state.id = id_cnt;
                id_cnt = id_cnt + 1;
                adapter.add_state(state);
                store_info();
                if (state.is_active)
                {
                    Intent intent = new Intent(MainActivity.this, StateManager.class);
                    intent.putExtra("op", "sr");
                    intent.putExtra("id", state.id);
                    startService(intent);
                }
                adapter.notifyDataSetChanged();
                manage_geofence(state);
            }
            else if(resultCode == 2){
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                adapter.set_state(cur_pos, state);
                store_info();
                if(state.is_active) {
                    if(run_id == -1) // off -> on으로
                    {
                        Intent intent = new Intent(MainActivity.this, StateManager.class);
                        intent.putExtra("op", "r");
                        intent.putExtra("id", state.id);
                        startService(intent);
                    }
                    else{ //on -> on
                        if(run_id != state.id) //active 유지하고 내용만 바꾸는 경우 제외
                        {
                            Intent intent = new Intent(MainActivity.this, StateManager.class);
                            intent.putExtra("op", "sr");
                            intent.putExtra("id", state.id);
                            startService(intent);
                        }
                    }
                }
                else{
                    if(run_id == state.id) //on -> off로
                    {
                        Intent intent = new Intent(MainActivity.this, StateManager.class);
                        intent.putExtra("op", "s");
                        startService(intent);
                    }
                }
                adapter.notifyDataSetChanged();
                manage_geofence(state);
            }
            else if(resultCode == 3){
                //delete
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                if(state.geofence_exist)
                {
                    List<String> removelist = new ArrayList<>();
                    removelist.add("geofence" + state.id);
                    geofencingClient.removeGeofences(removelist);
                }
                if(state.id == run_id)
                {
                    run_id = -1;
                }
                try {
                    Process p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("rm -rf " + this.getFilesDir()  + "/" + run_id + "\n");
                    os.flush();
                    os.writeBytes("exit\n");
                    os.flush();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                adapter.remove(cur_pos);
                store_info();
                adapter.notifyDataSetChanged();
            }
            //else는 취소하고 돌아온 거.
        }
    }

    //fab onclick
    public void add_state(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        intent.putExtra("option", "add");
        intent.putExtra("state", new savingStates());
        startActivityForResult(intent, 1);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onPause(){  //run activity 조절해줘야하지 않을까
        super.onPause();
    }

    @Override
    public void onStop(){  //저장은 계속계속 되고 있고, 좀 비효율적이긴한데, 정보 손실을 최대로 막는 중, 싫으면 onDestroy로 옮기면 되는데 스와이프 시키면 안되기도 함.
        super.onStop();
        store_info();
    }

    public void store_info()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = adapter.get_json();
        editor.putString("adap", json);
        editor.putInt("run", run_id);
        editor.putInt("id_cnt", id_cnt);
        editor.commit();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, LocationTracking.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    @SuppressLint("MissingPermission")
    private void manage_geofence(savingStates state)
    {
        if(state.geofence_exist)
        {
            List<String> removelist = new ArrayList<>();
            removelist.add("geofence" + state.id);
            geofencingClient.removeGeofences(removelist);
        }
        if(state.is_tracking){
            builder = new GeofencingRequest.Builder();
            geofence = new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId("geofence" + state.id)

                    .setCircularRegion(
                            state.lat,
                            state.lng,
                            GEOFENCE_RADIUS
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER); //제일 처음 확인할 조건
            builder.addGeofence(geofence);
            geofencingClient.addGeofences(builder.build(), getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("000000", "onSuccess: Geofence Added...");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("000000", "onFailure: ");
                            e.printStackTrace();
                        }
                    });
        }
    }
}