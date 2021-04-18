package com.example.psh;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements StateHoldingActivity.OnTaskCompleted{
    private static final String TRACKING_LOCATION_KEY = "Trrdeds";

    private ListView listView;
    private int cur_pos;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        listView = findViewById(R.id.show_list);
        adapter = new CustomAdapter();
               // new ArrayAdapter<savingStates>(this, android.R.layout.simple_list_item_1, profiles);
        listView.setAdapter(adapter);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent reply){
        super.onActivityResult(requestCode, resultCode, reply);
        if (requestCode == 1) {
            if (resultCode == 1) {
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                //checkbox 동적 할당? 및 정보 긁어와서 채워넣기
                adapter.add_state(state);
                adapter.notifyDataSetChanged();
            }
            else if(resultCode == 2){
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                adapter.set_state(cur_pos, state);
                adapter.notifyDataSetChanged();
            }
            else if(resultCode == 3){
                //delete
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
        Log.d("Aaaaa", "onDestroy() called");
    }

    @Override
    public void onTaskCompleted(String result) {

    }

    //나가면서 state 저장하고 있는거
    //켜져 있을 때 background에서 동작하는 거 activity 하나 만들고 profile 넘겨주면 do it background를 슈슉슈슉 할 수 있도록
    //background에서 바뀔 때 alarm 띄워주는 거
    //위치 추적하는 거
}
