package com.example.psh;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {
    private ArrayList<savingStates> profiles;

    public CustomAdapter(){
        profiles = new ArrayList<savingStates>();
    }

    @Override
    public int getCount() {
        return profiles.size();
    }

    @Override
    public Object getItem(int i) {
        return profiles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Context context = viewGroup.getContext();
        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.listview_item, viewGroup, false);
        }
        savingStates cur_item = profiles.get(i);
        if(cur_item.is_active)
        {
            ((LinearLayout)view.findViewById(R.id.content_layout)).setBackgroundColor(0xFF85C6A0);
        }
        else
        {
            ((LinearLayout)view.findViewById(R.id.content_layout)).setBackgroundColor(0xFFFFFFFF);
        }
        ((TextView)view.findViewById(R.id.name)).setText(cur_item.name);
        ((TextView)view.findViewById(R.id.desc)).setText(cur_item.description);

        return view;
    }

    public void add_state(savingStates state)
    {
        profiles.add(state);
    }

    public void set_state(int pos, savingStates state)
    {
        profiles.set(pos, state);
    }

    public void remove(int pos)
    {
        profiles.remove(pos);
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
