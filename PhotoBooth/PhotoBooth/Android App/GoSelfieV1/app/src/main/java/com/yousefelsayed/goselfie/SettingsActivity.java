package com.yousefelsayed.goselfie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioQrGroup, lightRadioGroup, flashRadioGroup, printRadioGroup;
    private RadioButton radioQrButton, lightRadioButton, flashRadioButton, printRadioButton;
    private SharedPreferences sharedPreferences;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeTheAppFullScreen();
        setContentView(R.layout.activity_settings);

        init();
        checkSelected();
        setupListeners();
    }
    private void makeTheAppFullScreen() {
        //For FullScreen
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.KEEP_SCREEN_ON;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void init(){
        radioQrGroup = findViewById(R.id.qrRadioGroup);
        lightRadioGroup = findViewById(R.id.lightRadioGroup);
        backButton = findViewById(R.id.userBackButton);
        flashRadioGroup = findViewById(R.id.flashRadioGroup);
        printRadioGroup = findViewById(R.id.printRadioGroup);
        sharedPreferences = getSharedPreferences("GoSelfieSP",0);
    }
    private void checkSelected(){
        if (sharedPreferences.getInt("qrcodeButton",1) == 1){
            radioQrButton = findViewById(R.id.qrEnableRadioButton);
            radioQrButton.setChecked(true);
        }else {
            radioQrButton = findViewById(R.id.qrDisableRadioButton);
            radioQrButton.setChecked(true);
        }
        if (sharedPreferences.getInt("lightSettings",1) == 1){
            radioQrButton = findViewById(R.id.lightEnableRadioButton);
            radioQrButton.setChecked(true);
        }else {
            radioQrButton = findViewById(R.id.lightDisableRadioButton);
            radioQrButton.setChecked(true);
        }
        if (sharedPreferences.getInt("flashSettings",1) == 1){
            flashRadioButton = findViewById(R.id.flashEnableRadioButton);
            flashRadioButton.setChecked(true);
        }else {
            flashRadioButton = findViewById(R.id.flashDisableRadioButton);
            flashRadioButton.setChecked(true);
        }
        if (sharedPreferences.getInt("printSettings",1) == 1){
            printRadioButton = findViewById(R.id.printEnableRadioButton);
            printRadioButton.setChecked(true);
        }else {
            printRadioButton = findViewById(R.id.printDisableRadioButton);
            printRadioButton.setChecked(true);
        }
    }
    private void setupListeners(){
        radioQrGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                radioQrButton = findViewById(i);
                if (radioQrButton.getText().equals(" Enable")){
                    sharedPreferences.edit().putInt("qrcodeButton",1).commit();
                }else {
                    sharedPreferences.edit().putInt("qrcodeButton",0).commit();
                }
            }
        });
        lightRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                lightRadioButton = findViewById(i);
                if (lightRadioButton.getText().equals(" Enable")){
                    sharedPreferences.edit().putInt("lightSettings",1).commit();
                }else {
                    sharedPreferences.edit().putInt("lightSettings",0).commit();
                }
            }
        });
        flashRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                flashRadioButton = findViewById(i);
                if (flashRadioButton.getText().equals(" Enable")){
                    sharedPreferences.edit().putInt("flashSettings",1).commit();
                }else {
                    sharedPreferences.edit().putInt("flashSettings",0).commit();
                }
            }
        });
        printRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                printRadioButton = findViewById(i);
                if (printRadioButton.getText().equals(" Enable")){
                    sharedPreferences.edit().putInt("printSettings",1).commit();
                }else {
                    sharedPreferences.edit().putInt("printSettings",0).commit();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        makeTheAppFullScreen();
        /* Workaround a probable Android bug with fullscreen activities:
         * on resume status bar hides and black margin stays,
         * reproducible half of the time when coming back from lock screen
         */
    }

}