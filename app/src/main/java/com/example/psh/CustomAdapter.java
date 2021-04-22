package com.example.psh;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
}
