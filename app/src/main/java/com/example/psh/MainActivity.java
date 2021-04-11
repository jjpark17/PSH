package com.example.psh;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<savingStates> profiles;
    private ListView listView;
    private int cnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        profiles = new ArrayList<savingStates>();
        listView = findViewById(R.id.show_list);
        cnt = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                savingStates state = new savingStates();
                //checkbox 동적 할당? 및 정보 긁어와서 채워넣기
                state.id = cnt;
                profiles.add(state);
                cnt++;
            }
        }
    }

    //fab onclick
    public void add_state(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, 1);
    }

    //state 지우기 & cnt 다 끌어당기기?
    //update view? // https://recipes4dev.tistory.com/48 listview 참고

    //클릭해서 설정 변경
    //켜져 있을 때 background에서 동작하는 거
    //켜져있는 거 어떻게 관리할 건지, cnt로 몇번이 켜져있는지?
    //background에서 바뀔 때 alarm 띄워주는 거
    //설정 계속 유지하고 있기
}
