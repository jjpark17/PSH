package com.example.psh;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

    private boolean previous_tracking;
    private Geofence geofence = null;
    private GeofencingClient geofencingClient;
    private GeofencingRequest.Builder builder;
    private PendingIntent geofencePendingIntent;

    private float GEOFENCE_RADIUS = 150;


    public CustomAdapter adapter = new CustomAdapter();
    AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        listView = findViewById(R.id.show_list);
        restore_info();
        instance = this;
        geofencingClient = LocationServices.getGeofencingClient(this);
    }

    void restore_info()
    {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
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
/*
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent); 전체적인 설정을 키는 부분인 듯
 */
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
                if (state.is_active)
                {
                    if (run_id != -1) {
                        savingStates run_state = adapter.find_by_id(run_id);
                        run_state.is_active = false;   //로그 갈아 끼우고?
                        save_runstate();
                    }
                    run_id = state.id;
                }
                adapter.notifyDataSetChanged();
                manage_geofence(state);
            }
            else if(resultCode == 2){
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                adapter.set_state(cur_pos, state);
                if(state.is_active) {
                    if(run_id != -1)
                    {
                        savingStates run_state = adapter.find_by_id(run_id);
                        run_state.is_active = false;
                        state.is_active = true;
                        save_runstate();
                    }
                    run_id = state.id;
                    execute_runstate(run_id);
                }
                else{
                    if(run_id == state.id) //on -> off로
                    {
                        save_runstate();
                        run_id = -1;
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
                //관련 로그 파일 다 지우고
                adapter.remove(cur_pos);
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
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = adapter.get_json();
        editor.putString("adap", json);
        editor.putInt("run", run_id);
        editor.putInt("id_cnt", id_cnt);
        editor.commit();
    }

    public void execute_runstate(int id)
    {
        try {
            BufferedReader br = new BufferedReader(new FileReader(getFilesDir() + Integer.toString(run_id) + "log.txt"));
            String readStr = "";
            String str = null;
            while((str = br.readLine()) != null)
                readStr += str + "\n";
            br.close();
            Log.d("whole txt", readStr);
            String[] arr = readStr.split("volume");
            if(arr.length > 1)
            {
                Log.d("volume txt", arr[1]);
                string_to_volume(arr[1]);
                readStr = arr[2];
            }

            //(readStr);
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save_runstate()
    {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(getFilesDir() + Integer.toString(run_id) + "log.txt",false));
            savingStates run_state = adapter.find_by_id(run_id);
            if(run_state.sound_save)
            {
                bw.write("volume");
                bw.write(volume_to_string());
                bw.write("volume");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //location option 켜 놓은 애들 배열로 만들어 놓고 locationtracking에서 background로 위치 추적하도록 만들고
    //위치는 아예 안겹치게 받거나 또는 그냥 맨 위에거 부터 먼저 겹치는 곳으로 가도록? 나중에 우선순위를 설정하고 비교하는 식으로.
    //main으로 위치 넘겨줄 때 마다 추적하고 있는 위치랑 비교해서 alarm 띄워주고 설정 건드리도록 변경

    //권한은 그냥 한번에 받자

    public String volume_to_string()
    {
       int alarm = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
       int dtmf = mAudioManager.getStreamVolume(AudioManager.STREAM_DTMF);
       int media = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
       int notice = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
       int ring = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
       int system = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
       int vcall = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
       int ringmode = mAudioManager.getRingerMode();
       int mode = mAudioManager.getMode();
       return alarm + ";" + dtmf + ";" + media + ";" + notice + ";" + ring + ";"  + system + ";" + vcall + ";" + ringmode + ";" + mode;
    }

    public void string_to_volume(String log)
    {
        String[] arr = log.split(";");
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, Integer.parseInt(arr[0]),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_DTMF, Integer.parseInt(arr[1]),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Integer.parseInt(arr[2]),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, Integer.parseInt(arr[3]),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, Integer.parseInt(arr[4]),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, Integer.parseInt(arr[5]),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, Integer.parseInt(arr[6]),0);
        mAudioManager.setRingerMode(Integer.parseInt(arr[7]));
        mAudioManager.setMode(Integer.parseInt(arr[8]));
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
                        }
                    });
        }
    }
}