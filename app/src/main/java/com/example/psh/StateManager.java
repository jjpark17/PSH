package com.example.psh;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class StateManager extends Service {
    private AudioManager mAudioManager;
    private int run_id;
    private int id_cnt;
    private Context context;
    SharedPreferences sharedPref;
    public ArrayList<savingStates> profiles;

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        super.onStartCommand(intent, flags, startID);
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        run_id = sharedPref.getInt("run", -1);
        Log.d("on start", Integer.toString(run_id));
        profiles = new ArrayList<savingStates>();
        String json = sharedPref.getString("adap", "none");
        read_json(json);
        Log.d("on start", profiles.toString());

        String op;
        op = intent.getStringExtra("op");
        if(op.equals("r")){
            int id = intent.getIntExtra("id", -1);
            restore_runstate(id);
        }
        else if (op.equals("s"))
            save_runstate();
        else if (op.equals("sr")){
            save_runstate();
            int id = intent.getIntExtra("id", -1);
            restore_runstate(id);
        }
        stopSelf();
        return 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void restore_runstate(int id)  //run을 id로 복구
    {
        savingStates temp = find_by_id(id);
        if(temp == null)
            return;
        temp.is_active = true;
        run_id = id;
        try{
            MainActivity mainActivity = MainActivity.getInstance();
            mainActivity.run_id = run_id;
            mainActivity.adapter.profiles = profiles;
            mainActivity.adapter.notifyDataSetChanged();
        } catch (NullPointerException e) {
            Log.d("asdf", "working in background");
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(this.getApplicationContext().getFilesDir() + "/" + Integer.toString(id) + "log.txt"));
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = get_json();
        editor.putString("adap", json);
        editor.putInt("run", run_id);
        editor.putInt("id_cnt", id_cnt);
        editor.commit();
    }

    public void save_runstate() //기존의 run을 저장하고 run을 -1로
    {
        if(run_id == -1)
            return;
        savingStates temp = find_by_id(run_id);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.getApplicationContext().getFilesDir().getAbsolutePath() + "/" + Integer.toString(run_id) + "log.txt",false));
            if(temp.sound_save)
            {
                bw.write("volume");
                bw.write(volume_to_string());
                bw.write("volume");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        temp.is_active = false;
        run_id = -1;
        try{
            MainActivity mainActivity = MainActivity.getInstance();
            mainActivity.run_id = -1;
            mainActivity.adapter.profiles = profiles;
            mainActivity.adapter.notifyDataSetChanged();
        } catch (NullPointerException e) {
            Log.d("asdf", "working in background");
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = get_json();
        editor.putString("adap", json);
        editor.putInt("run", run_id);
        editor.putInt("id_cnt", id_cnt);
        editor.commit();
    }

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

    public String get_json()
    {
        Gson arr_gson = new Gson();
        String json = arr_gson.toJson(profiles);
        return json;
    }

    public void read_json(String json)
    {
        Gson arr_gson = new Gson();
        if(!json.equals("none"))
        {
            Type type = new TypeToken<ArrayList<savingStates>>(){}.getType();
            profiles = arr_gson.fromJson(json, type);
        }
    }

    public savingStates find_by_id(int id){
        for(savingStates cur_state : profiles)
        {
            if(cur_state.id == id)
                return cur_state;
        }
        return null;
    }
}
