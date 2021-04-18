package com.example.psh;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {
    private savingStates state;
    private Button save_button;
    private boolean is_add;
    private EditText name_box;
    private EditText des_box;
    private CheckBox sound_cb;
    private CheckBox cache_cb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        save_button = findViewById(R.id.save_btn);
        Intent intent = getIntent();
        String op = intent.getStringExtra("option");
        state = (savingStates)intent.getSerializableExtra("state");
        name_box = findViewById(R.id.name_box);
        des_box = findViewById(R.id.des_box);
        sound_cb = findViewById(R.id.sound_cb);
        cache_cb = findViewById(R.id.cache_cb);

        if (op.equals("add"))
        {
            is_add = true;
        }
        else
        {
            is_add = false;
            name_box.setText(state.name);
            des_box.setText(state.description);
            sound_cb.setChecked(state.sound_save);
            cache_cb.setChecked(state.cache_save);
        }
    }

    //save_btn onclick
    public void save_state(View view) {
        Intent reply_intent = new Intent();
        state.name = name_box.getText().toString();
        state.description = des_box.getText().toString();
        state.sound_save = sound_cb.isChecked();
        state.cache_save = cache_cb.isChecked();
        reply_intent.putExtra("reply_state", state);
        if(is_add)
        {
            setResult(1,reply_intent);
        }
        else
        {
            setResult(2,reply_intent);
        }
        finish();
    }


    public void delete_state(View view) {
        Intent reply_intent = new Intent();
        if(is_add)
        {
            setResult(4,reply_intent);
        }
        else
        {
            reply_intent.putExtra("reply_state", state);
            setResult(3,reply_intent);
        }
        finish();
    }

    public void back_btn(View view) {
        Intent reply_intent = new Intent();
        setResult(4,reply_intent);
        finish();
    }
}