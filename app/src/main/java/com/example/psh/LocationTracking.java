package com.example.psh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class LocationTracking extends BroadcastReceiver {
    // ...
    private static final String TAG = "GeofenceBroadcastReceiv";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
//        Toast.makeText(context, "Geofence triggered...", Toast.LENGTH_SHORT).show();

        NotificationHelper notificationHelper = new NotificationHelper(context);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event...");
            return;
        }

        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
        for (Geofence geofence: geofenceList) {
            Log.d(TAG, "onReceive: " + geofence.getRequestId());
        }
//        Location location = geofencingEvent.getTriggeringLocation();
        int transitionType = geofencingEvent.getGeofenceTransition();
        List<Geofence> geofences = geofencingEvent.getTriggeringGeofences();
        Geofence temp = geofences.get(0);
        int id = Integer.parseInt(temp.getRequestId().substring(8));

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        int run_id = sharedPref.getInt("run", -1);
        String json = sharedPref.getString("adap", "none");
        CustomAdapter adapter = new CustomAdapter();
        adapter.read_json(json);
        Intent state_intent;

        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                if(run_id != -1){ // 바꿀 꺼가 있는지
                    if(run_id == id) //이미 실행하고 있던 거
                        return;
                    state_intent = new Intent(context.getApplicationContext(), StateManager.class);
                    state_intent.putExtra("op", "sr");
                    state_intent.putExtra("id", id);
                    context.getApplicationContext().startService(state_intent);
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_ENTER", adapter.find_by_id(run_id).name + " saved & " + adapter.find_by_id(id).name + " restored" , SettingsActivity.class);
                }
                else // 그냥 복구
                {
                    state_intent = new Intent(context.getApplicationContext(), StateManager.class);
                    state_intent.putExtra("op", "r");
                    state_intent.putExtra("id", id);
                    context.getApplicationContext().startService(state_intent);
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_ENTER", adapter.find_by_id(id).name + " restored" , SettingsActivity.class);
                }
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                if(run_id != -1){
                    if(id != run_id)
                    {
                        Log.d("adsf", "location error or changed manually in range of geofence");
                        return;
                    }
                    state_intent = new Intent(context.getApplicationContext(), StateManager.class);
                    state_intent.putExtra("op", "s");
                    context.getApplicationContext().startService(state_intent);
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_EXIT", adapter.find_by_id(id).name + " saved", SettingsActivity.class);
                }
                break;
        }

    }
}