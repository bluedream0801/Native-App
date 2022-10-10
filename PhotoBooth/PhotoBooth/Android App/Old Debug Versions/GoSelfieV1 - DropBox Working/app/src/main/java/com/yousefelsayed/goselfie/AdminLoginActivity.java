package com.yousefelsayed.goselfie;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class AdminLoginActivity extends AppCompatActivity {

    ImageView startQrScan;
    Button loginButton;
    EditText passwordInput;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
                if(result.getContents() != null) {
                    checkPassword(result.getContents());
                    Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                }
            });
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);
        init();
        setupListeners();
        makeTheAppFullScreen();
    }

    private void makeTheAppFullScreen() {
        //For FullScreen
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.KEEP_SCREEN_ON;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void init(){
        startQrScan = findViewById(R.id.startQrScanActivity);
        loginButton = findViewById(R.id.loginButton);
        passwordInput = findViewById(R.id.passwordEditText);
        sp = getSharedPreferences("GoSelfieSP",0);
    }
    private void setupListeners(){
        startQrScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanOptions options = new ScanOptions();
                options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
                options.setPrompt("Scan a barcode");
                options.setCameraId(1);
                options.setBeepEnabled(true);
                options.setBarcodeImageEnabled(true);
                barcodeLauncher.launch(options);
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!passwordInput.getText().toString().equals(" ")){
                    checkPassword(passwordInput.getText().toString());
                }else {
                    Toast.makeText(AdminLoginActivity.this,"Can't leave input empty",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void checkPassword(String pass){
        if (pass.equals("1313q")){
            Intent intent = new Intent(AdminLoginActivity.this,AdminSettingsActivity.class);
            startActivity(intent);
            finish();
        }else if(pass.equals(sp.getString("ID","0"))){
            Intent intent = new Intent(AdminLoginActivity.this,SettingsActivity.class);
            startActivity(intent);
            finish();
        }else{
            Toast.makeText(this,"Sorry, Wrong Password!",Toast.LENGTH_SHORT).show();
        }
    }
}