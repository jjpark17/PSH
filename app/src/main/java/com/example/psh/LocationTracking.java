package com.example.psh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

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
        MainActivity mainActivity = MainActivity.getInstance();
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                String body = "";
                if(mainActivity.run_id != -1){
                    mainActivity.adapter.find_by_id(mainActivity.run_id).is_active = false;
                    mainActivity.save_runstate();
                    body = mainActivity.adapter.find_by_id(mainActivity.run_id).name + " saved & ";
                }
                notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_ENTER" + temp.getRequestId() + "test", body + mainActivity.adapter.find_by_id(id).name + " restored" , SettingsActivity.class);
                mainActivity.run_id = id;
                mainActivity.execute_runstate(id);
                mainActivity.adapter.find_by_id(id).is_active = true;
                mainActivity.adapter.notifyDataSetChanged();
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_DWELL", "", SettingsActivity.class);
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                if(mainActivity.run_id != -1)
                {
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_EXIT" + geofences.get(0).getRequestId() + "test", mainActivity.adapter.find_by_id(mainActivity.run_id).name + " saved", SettingsActivity.class);
                    mainActivity.save_runstate();
                    mainActivity.run_id = -1;
                    mainActivity.adapter.find_by_id(id).is_active = false;
                    mainActivity.adapter.notifyDataSetChanged();
                }
                break;
        }

    }
}