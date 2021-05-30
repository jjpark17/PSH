package com.example.psh;

import android.annotation.SuppressLint;
import android.app.Service;
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
import java.io.DataOutputStream;
import java.io.File;
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
    SharedPreferences sharedPref;
    public ArrayList<savingStates> profiles;

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        super.onStartCommand(intent, flags, startID);
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        run_id = sharedPref.getInt("run", -1);
        id_cnt = sharedPref.getInt("id_cnt", -1);
        profiles = new ArrayList<savingStates>();
        String json = sharedPref.getString("adap", "none");
        read_json(json);

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
            if(temp.sound_save)
            {
                BufferedReader br = new BufferedReader(new FileReader(this.getFilesDir() + "/" + id + "/" + "volume.txt"));
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
                }
            }
            if(temp.cache_save)
                restore_app("com.android.chrome");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        File folder = new File(this.getFilesDir().getAbsolutePath() + "/" + run_id + "/");
        folder.mkdir();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(folder.getAbsolutePath() + "/volume.txt",false));
            if(temp.sound_save)
            {
                bw.write("volume");
                bw.write(volume_to_string());
                bw.write("volume");
            }
            bw.close();
            if(temp.cache_save)
                save_app("com.android.chrome");
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

    public void restore_app(String app_name) {
        try {
            //ApplicationInfo app = this.getPackageManager().getApplicationInfo(app_name, 0);
            //Context packageContext = createPackageContext(app.packageName, 0);
           // File cache = new File(packageContext.getCacheDir().getAbsolutePath());
            //File app = new File("/data/user/0/" + app_name + "/");
            File app = new File("/data/user/0/" + app_name);

            /*
            File cookie = new File("/data/user/0/com.android.chrome/app_chrome/Default/Cookies");
            //File cookie_j = new File("/data/user/0/com.android.chrome/app_chrome/Default/Cookies-journal");
            File history = new File("/data/user/0/com.android.chrome/app_chrome/Default/History");
            //File history_j = new File("/data/user/0/com.android.chrome/app_chrome/Default/History-journal");
            */
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            String temp = "[[ -e " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/" + app_name + " ]] && rm -rf " + app.getAbsolutePath() + "/* " + "\n";
            os.writeBytes(temp);
            os.flush();
            os.writeBytes("cp -a " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/" + app_name + "/. " + app.getAbsolutePath() + "\n");
            os.flush();

            /*
            os.writeBytes("FILE=\"" + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/" + app_name + "\"\n");
            os.flush();
            os.writeBytes("if[ -e ${FILE} ] ; then" + "\n");
            os.flush();
            os.writeBytes("a = 1" + "\n");
            //os.writeBytes("rm -rf " + app.getAbsolutePath() + "/*\n");
            os.flush();

            os.writeBytes("else" + "\n");
            os.flush();
            os.writeBytes("mkdir" + " " + app.getAbsolutePath() + "aa_false" + "\n");
            //os.writeBytes("rm -rf " + app.getAbsolutePath() + "/*\n");
            os.flush();
            os.writeBytes("fi\n");
            os.flush();
*/

            /*os.writeBytes("if[ -f \"" + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/Default\" ]; then" + "\n");
            os.flush();
            os.writeBytes("rm -rf " + others.getAbsolutePath() + "/*\n");
            os.flush();
            os.writeBytes("cp -a " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/Default/. " + others.getAbsolutePath() + "/ " + "\n");
            os.flush();*/
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return;
    }

    public void save_app(String app_name)
    {
        try {
            /*
            ApplicationInfo app = this.getPackageManager().getApplicationInfo("com.android.chrome", 0);
            Context packageContext = createPackageContext(app.packageName, 0);
            //File cache = new File("/data/user/0/com.android.chrome/cache/Cache/");
            //File cookie = new File("/data/user/0/com.android.chrome/app_chrome/Default/Cookies");
            File cache = new File(packageContext.getCacheDir().getAbsolutePath());
            File others = new File("/data/user/0/com.android.chrome/app_chrome/Default");*/

            //File app = new File("/data/user/0/" + app_name + "/");
            File app = new File("/data/user/0/" + app_name);

            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("mkdir" + " " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/" + app_name + "\n");
            os.flush();
            os.writeBytes("cp -a " + app.getAbsolutePath() + "/. " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/" + app_name + "/" + "\n");
            os.flush();
            /*
            os.writeBytes("mkdir" + " " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/Default" + "\n");
            os.flush();
            os.writeBytes("cp -a " + app.getAbsolutePath() + "/. " + this.getFilesDir().getAbsolutePath() + "/" + run_id + "/Default/" + "\n");
            os.flush();*/
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        /*
        final PackageManager pm = getPackageManager();
//get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        //Log.d(TAG, "Installed package :" + packageInfo.packageName); //com.android.chrome  com.samsung.android.app.sbrowseredge
        //Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
        //Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));

        String TAG = "cache_error";
        for (ApplicationInfo packageInfo : packages) {

            if (packageInfo.packageName.equals("com.android.chrome")) {
                try {
                    Context packageContext = createPackageContext(packageInfo.packageName, 0);
                    File cache = packageContext.getCacheDir();
                    File files = packageContext.getFilesDir();
                    File dir = new File("/data/data/com.android.chrome");
                    if(dir.isFile())
                        ;
                    if(dir.isDirectory())
                        ;
                    if(dir.isHidden())
                        ;
                    boolean a = dir.mkdir();
                    boolean b = cache.mkdir();
                    Log.d(TAG, dir.list()[0]);
                    File file_list[] = dir.listFiles();
                    Log.d(TAG, file_list.toString());
                    if(file_list != null)
                    {
                        for (int i=0; i<file_list.length; ++i)
                        {
                            Log.d("FILE:", file_list[i].getName());
                            BufferedReader br = new BufferedReader(new FileReader(file_list[i].getAbsolutePath()));
                            String readStr = "";
                            String str = null;
                            while ((str = br.readLine()) != null)
                                readStr += str + "\n";
                            br.close();
                            Log.d(TAG, readStr);
                        }
                    }

                    BufferedReader br = new BufferedReader(new FileReader(packageContext.getAbsolutePath()));
                    String readStr = "";
                    String str = null;
                    while ((str = br.readLine()) != null)
                        readStr += str + "\n";
                    br.close();
                    Log.d(TAG, readStr);

                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "NameNotFoundException");
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "FileNotFoundException");
                } catch (IOException e) {
                    Log.d(TAG, "IOException");
                }
            }
        }*/
        return;
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
