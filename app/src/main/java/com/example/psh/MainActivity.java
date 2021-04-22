package com.example.psh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements StateHoldingActivity.OnTaskCompleted{

    private ListView listView;
    private int cur_pos = -1;
    private int run_id = -1;
    private int id_cnt = 0;

    private CustomAdapter adapter = new CustomAdapter();

    void restore_info()
    {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        run_id = sharedPref.getInt("run", -1);
        id_cnt = sharedPref.getInt("id_cnt", 0);
        String json = sharedPref.getString("adap", "none");
        adapter.read_json(json);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        listView = findViewById(R.id.show_list);
        restore_info();
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
                    }
                    run_id = state.id;
                }
                adapter.notifyDataSetChanged();
            }
            else if(resultCode == 2){
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                adapter.set_state(cur_pos, state);
                if(state.is_active) {
                    if(run_id != -1)
                    {
                        savingStates run_state = adapter.find_by_id(run_id);
                        run_state.is_active = false;   //로그 갈아 끼우고? // equal은 생각 안해도 될듯?
                    }
                    run_id = state.id;
                }
                adapter.notifyDataSetChanged();
            }
            else if(resultCode == 3){
                //delete
                savingStates state = (savingStates)reply.getSerializableExtra("reply_state");
                if(state.equals(run_id))
                {
                    run_id = -1;
                }
                //관련 로그 다 지우고
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

    @Override
    public void onTaskCompleted(String result) {

    }

    //켜져 있을 때 background에서 동작하는 거 activity 하나 만들고 profile 넘겨주면 do it background를 슈슉슈슉 할 수 있도록
    //background에서 바뀔 때 alarm 띄워주는 거
    //위치 추적하는 거
    //권한은 그냥 한번에 받자
}
