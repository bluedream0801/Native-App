package com.yousefelsayed.goselfie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioWaterMarkGroup;
    private RadioButton radioWaterMarkButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        init();
        checkSelected();
        setupListeners();
    }

    private void init(){
        radioWaterMarkGroup = findViewById(R.id.radioGroup);
        sharedPreferences = getSharedPreferences("GoSelfieSP",0);
    }
    private void checkSelected(){
        if (sharedPreferences.getInt("watermark",1) == 1){
            radioWaterMarkButton = findViewById(R.id.watermarkEnableRadioButton);
            radioWaterMarkButton.setChecked(true);
        }else {
            radioWaterMarkButton = findViewById(R.id.watermarkDisableRadioButton);
            radioWaterMarkButton.setChecked(true);
        }
    }
    private void setupListeners(){
        radioWaterMarkGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                radioWaterMarkButton = findViewById(i);
                if (radioWaterMarkButton.getText().equals(" Enable")){
                    sharedPreferences.edit().putInt("watermark",1).commit();
                }else {
                    sharedPreferences.edit().putInt("watermark",0).commit();
                }
            }
        });
    }

}