package com.yousefelsayed.goselfie;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import com.dropbox.core.v2.users.FullAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminSettingsActivity extends AppCompatActivity {

    private RadioGroup radioWaterMarkGroup;
    private RadioButton radioWaterMarkButton;
    private SharedPreferences sharedPreferences;
    private EditText idEditText;
    private Button updateIdButton;
    //private String DROPBOX_ACCOUNT_KEY = "qe2tm2jd0vzm231";
    private String DROPBOX_ACCOUNT_KEY = "37yrhuixb5vzrtf";
    private String DROPBOX_AUTH_KEY;
    DbxRequestConfig config;
    //Firebase
    FirebaseStorage storage;
    StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);
        makeTheAppFullScreen();

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
        radioWaterMarkGroup = findViewById(R.id.radioGroup);
        sharedPreferences = getSharedPreferences("GoSelfieSP",0);
        updateIdButton = findViewById(R.id.updateIdButton);
        idEditText = findViewById(R.id.idEditText);
        config = DbxRequestConfig.newBuilder("GoSelfie").build();
        //Backend
        storage = FirebaseStorage.getInstance();
    }
    private void checkSelected(){
        if (sharedPreferences.getInt("watermark",1) == 1){
            radioWaterMarkButton = findViewById(R.id.watermarkEnableRadioButton);
            radioWaterMarkButton.setChecked(true);
        }else {
            radioWaterMarkButton = findViewById(R.id.watermarkDisableRadioButton);
            radioWaterMarkButton.setChecked(true);
        }
        idEditText.setText(sharedPreferences.getString("ID","0"));
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void DropboxLogin(){
        if (isNetworkAvailable()){
            if (sharedPreferences.getString("dropBoxHelp",null) != null){
                try {
                    DbxCredential credential = DbxCredential.Reader.readFully(sharedPreferences.getString("dropBoxHelp",null));
                    Log.d("Debug","Test22: "+credential.getAccessToken());
                    DROPBOX_AUTH_KEY = credential.getAccessToken();
                } catch (JsonReadException e) {
                    Log.d("Debug","Test222Error: "+e.getMessage());
                    e.printStackTrace();
                }
            }else {
                Auth.startOAuth2PKCE(this, DROPBOX_ACCOUNT_KEY, config);
            }
        }else {
            Toast.makeText(this,"There's no internet",Toast.LENGTH_SHORT).show();
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
        updateIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!idEditText.getText().toString().equals(" ")){
                    sharedPreferences.edit().putString("ID",idEditText.getText().toString()).apply();
                    StartUploadToFirebase(sharedPreferences.getString("ID","0"));
                    //uploadToDropBox(sharedPreferences.getString("ID","0"));
                }
            }
        });
    }

    //Backend
    private void StartUploadToFirebase(String customerID){
        File mainFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/" +sharedPreferences.getString("ID","0")+"/Good_Pictures" + "/");
        File[] list = mainFolder.listFiles();
        if (isNetworkAvailable()){
            if(list != null){
                Toast.makeText(AdminSettingsActivity.this,"Customer ID changed to: "+customerID+" And Started uploading "+list.length+" images to firebase.",Toast.LENGTH_SHORT).show();
                updateIdButton.setClickable(false);
                String timeStamp = new SimpleDateFormat("HH.mm.ss", Locale.getDefault()).format(new Date());
                for (int i = 0;i < list.length;i++){
                    if (i != list.length - 1){
                        uploadToFirebase(list[i].getPath(),"CUSTOMER_"+sharedPreferences.getString("ID","0")+"_"+i+"_"+timeStamp+".png",0,mainFolder.getAbsolutePath(),customerID+"/");
                    }else {
                        uploadToFirebase(list[i].getPath(),"CUSTOMER_"+sharedPreferences.getString("ID","0")+"_"+i+"_"+timeStamp+".png",1,mainFolder.getAbsolutePath(),customerID+"/");
                    }
                }
            }else {
                Toast.makeText(AdminSettingsActivity.this,"Customer ID changed to: "+customerID+" but there's no pictures to upload.",Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(AdminSettingsActivity.this,"There's no internet can't upload images",Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadToFirebase(String filePath,String imageName,int isLast,String folderPath,String customerID){
        storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child(customerID+imageName);
        try {
            InputStream stream = new FileInputStream(new File(filePath));
            UploadTask uploadTask = imagesRef.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("Debug","Firebase Can't Upload: "+exception.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    Log.d("Debug","Firebase Uploaded image successfully");
                    if (isLast == 1){
                        if (deleteFile(folderPath)){
                            Toast.makeText(AdminSettingsActivity.this,"All Images are uploaded and deleted from the device successfully!",Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(AdminSettingsActivity.this,"All Images are uploaded but couldn't delete from the device!",Toast.LENGTH_SHORT).show();
                        }
                        updateIdButton.setClickable(true);
                    }
                }
            });
        } catch (FileNotFoundException e) {
            Log.d("Debug","Input File Stream Error: "+e.getMessage());
            e.printStackTrace();
        }
    }
    private void uploadToDropBox(String customerID){
        DropboxLogin();
        // Get current account info
        Log.d("Debug","Auth Key: "+DROPBOX_AUTH_KEY);
        File mainFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/" +sharedPreferences.getString("ID","0")+"/Good_Pictures" + "/");
        File[] list = mainFolder.listFiles();
        Log.d("Debug","ImageName: "+list[1]);
        if (isNetworkAvailable()){
            Toast.makeText(AdminSettingsActivity.this,"Customer ID changed to: "+customerID+" And Started uploading "+list.length+" images to dropbox.",Toast.LENGTH_SHORT).show();
            for (int i = 0;i < list.length;i++){
                if (i != list.length - 1){
                    uploadImages(list[i].getPath(),"CUSTOMER_"+sharedPreferences.getString("ID","0")+"_"+i,0,mainFolder.getAbsolutePath());
                }else {
                    uploadImages(list[i].getPath(),"CUSTOMER_"+sharedPreferences.getString("ID","0")+"_"+i,1,mainFolder.getAbsolutePath());
                }
            }
        }else {
            Toast.makeText(AdminSettingsActivity.this,"There's no internet can't upload images",Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadImages(String filePath,String imageName,int isLast,String folderPath){
        DbxClientV2 client = new DbxClientV2(config,DROPBOX_AUTH_KEY);
        //Upload Image
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new FileInputStream(filePath)) {
                    FileMetadata metadata = client.files().uploadBuilder("/GoSelfie/"+sharedPreferences.getString("ID","0")+"/GoodPictures/"+imageName+".png").withMode(WriteMode.OVERWRITE).uploadAndFinish(in);
                    Log.d("Debug","ImagePath: "+"/GoSelfie/"+sharedPreferences.getString("ID","0")+"/GoodPictures/"+imageName+".png");

                    if (isLast == 1){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (deleteFile(folderPath)){
                                    Toast.makeText(AdminSettingsActivity.this,"All Images are uploaded to dropBox and deleted from the device",Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(AdminSettingsActivity.this,"All Images are uploaded to dropBox but couldn't delete from the device",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    //Get ImageLink
                    //SharedLinkSettings linkSettings = SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build();
                    //SharedLinkMetadata sharedLinkMetadata = client.sharing().createSharedLinkWithSettings("/GoSelfie/"+sharedPreferences.getString("ID","0")+"/GoodPictures", linkSettings);
                    //Log.d("Debug", "FolderLink: "+ sharedLinkMetadata.getUrl());
                } catch (DbxException | IOException e) {
                    Log.d("Debug", "DropBox Upload Error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdminSettingsActivity.this, "Can't connect to dropbox: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    public boolean deleteFile(String Path) {
        try {
            Log.d("Debug","Deleting Started: "+Path);
            // delete the original file
            File dir = new File(Path);
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
            }
            dir.delete();
            return true;
        }
        catch (Exception e) {
            Log.d("Debug", e.getMessage());
            return false;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        makeTheAppFullScreen();
        if (Auth.getDbxCredential() != null){
            DROPBOX_AUTH_KEY = Auth.getOAuth2Token();
            //DROPBOX_AUTH_KEY = Auth.getDbxCredential().getRefreshToken();
            Log.d("Debug","ResultAuth Key: "+Auth.getOAuth2Token());
            Log.d("Debug","ResultAuth Refresh Key: "+Auth.getDbxCredential().getRefreshToken());
            sharedPreferences.edit().putString("dropBoxToken",Auth.getDbxCredential().getRefreshToken().toString()).apply();
            sharedPreferences.edit().putString("dropBoxHelp",Auth.getDbxCredential().toString()).apply();
        }
    }

}