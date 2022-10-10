package com.yousefelsayed.goselfie;


import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class AdminSettingsActivity extends AppCompatActivity {

    private RadioGroup radioWaterMarkGroup;
    private RadioButton radioWaterMarkButton;
    private SharedPreferences sharedPreferences;
    private EditText idEditText, printerId;
    private Button updateIdButton, uploadImageButton, updateIdUsingQr, sendUsingBluetooth, updatePrinterId;
    private TextView backButton;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            sharedPreferences.edit().putString("ID", result.getContents().toString().replaceAll("\\s+","")).apply();
            Toast.makeText(AdminSettingsActivity.this, "Customer ID successfully changed to: " + sharedPreferences.getString("ID", "0"), Toast.LENGTH_LONG).show();
            checkSelected();
        }
    });
    //Firebase
    FirebaseStorage storage;
    StorageReference storageRef;
    private DatabaseReference mDatabase;



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
    private void init() {
        radioWaterMarkGroup = findViewById(R.id.radioGroup);
        sharedPreferences = getSharedPreferences("GoSelfieSP", 0);
        updateIdButton = findViewById(R.id.updateIdButton);
        idEditText = findViewById(R.id.idEditText);
        uploadImageButton = findViewById(R.id.uploadImagesButton);
        backButton = findViewById(R.id.backButton);
        updateIdUsingQr = findViewById(R.id.changeIdWithQrCode);
        sendUsingBluetooth = findViewById(R.id.sendFilesUsingB);
        printerId = findViewById(R.id.printerIdEditText);
        updatePrinterId = findViewById(R.id.updatePrinterIdButton);
        //Backend
        storage = FirebaseStorage.getInstance();
    }
    private void checkSelected() {
        if (sharedPreferences.getInt("watermark", 1) == 1) {
            radioWaterMarkButton = findViewById(R.id.watermarkEnableRadioButton);
            radioWaterMarkButton.setChecked(true);
        } else {
            radioWaterMarkButton = findViewById(R.id.watermarkDisableRadioButton);
            radioWaterMarkButton.setChecked(true);
        }
        idEditText.setText(sharedPreferences.getString("ID", "0"));
        printerId.setText(sharedPreferences.getString("PrinterID", "1"));
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void setupListeners() {
        radioWaterMarkGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                radioWaterMarkButton = findViewById(i);
                if (radioWaterMarkButton.getText().equals(" Enable")) {
                    sharedPreferences.edit().putInt("watermark", 1).commit();
                } else {
                    sharedPreferences.edit().putInt("watermark", 0).commit();
                }
            }
        });
        updateIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!idEditText.getText().toString().equals(" ")) {
                    updateIdButton.setClickable(false);
                    uploadImageButton.setClickable(false);
                    StartUploadToFirebase(sharedPreferences.getString("ID", "0"));
                    sharedPreferences.edit().putString("ID", idEditText.getText().toString().replaceAll("\\s+","")).apply();
                }
            }
        });
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateIdButton.setClickable(false);
                uploadImageButton.setClickable(false);
                StartUploadToFirebase(sharedPreferences.getString("ID", "0"));
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        updateIdUsingQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQrScanFunction();
            }
        });
        sendUsingBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendFilesUsingBluetooth();
            }
        });
        updatePrinterId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!printerId.getText().toString().equals(" ")) {
                    updatePrinterId.setClickable(false);
                    sharedPreferences.edit().putString("PrinterID", printerId.getText().toString().replaceAll("\\s+","")).commit();
                    createPrinterFirebaseFields(printerId.getText().toString());
                    Toast.makeText(AdminSettingsActivity.this, "Printer ID changed to: "+sharedPreferences.getString("PrinterID","1"), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void startQrScanFunction() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan a Code");
        options.setCameraId(1);
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CustomQrCodeScannerActivity.class);
        barcodeLauncher.launch(options);
    }
    private void sendFilesUsingBluetooth(){
        Log.d("Debug","Selected2");
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        sharingIntent.setType("image/*");
        sharingIntent.setComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));
        ArrayList<Uri> files = new ArrayList<Uri>();
        File mainFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie" + "/" + sharedPreferences.getString("ID", "0") + "/"+"Good_Pictures/");
        String[] children = mainFolder.list();
        for (int i = 0;i < children.length;i++){
            Log.d("Debug",mainFolder.getAbsolutePath().toString()+"/"+children[i]);
            Uri uri1 = Uri.fromFile(new File(mainFolder.getAbsolutePath().toString()+"/"+children[i]));
            files.add(uri1);
        }
        Log.d("Debug","FilesL: "+files.size());
        sharingIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        sharingIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        //sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie" + "/" + "1" + "/"+"Good_Pictures/", "zz.png")));
        startActivity(sharingIntent);
    }


    //Backend
    private void StartUploadToFirebase(String customerID){
        File goodFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/" +sharedPreferences.getString("ID","0")+"/Good_Pictures" + "/");
        File badFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/" +sharedPreferences.getString("ID","0")+"/Other_Pictures" + "/");
        File[] list = goodFolder.listFiles();
        File[] list1 = badFolder.listFiles();
        if (isNetworkAvailable()){
            if(list != null){
                Toast.makeText(AdminSettingsActivity.this,"Started uploading "+list.length+" images to firebase.",Toast.LENGTH_SHORT).show();
                for (int i = 0;i < list.length;i++){
                    if (i != list.length - 1){
                        uploadToFirebase(list[i].getPath(),list[i].getName(),0,goodFolder.getAbsolutePath(),customerID,"Good_Pictures");
                    }else {
                        uploadToFirebase(list[i].getPath(),list[i].getName(),1,goodFolder.getAbsolutePath(),customerID,"Good_Pictures");
                    }
                }
            }else if(list1 != null){
                for (int i = 0;i < list1.length;i++){
                    if (i != list1.length - 1){
                        uploadToFirebase(list1[i].getPath(),list1[i].getName(),0,badFolder.getAbsolutePath(),customerID,"Other_Pictures");
                    }else {
                        uploadToFirebase(list1[i].getPath(),list1[i].getName(),1,badFolder.getAbsolutePath(),customerID,"Other_Pictures");
                    }
                }
            }else {
                Toast.makeText(AdminSettingsActivity.this,"Customer ID changed to: "+customerID+" but there's no pictures to upload.",Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(AdminSettingsActivity.this,"There's no internet can't upload images",Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadToFirebase(String filePath,String imageName,int isLast,String folderPath,String customerID,String folderName){
        storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child(customerID+"/"+folderName+"/"+imageName);
        try {
            InputStream stream = new FileInputStream(new File(filePath));
            UploadTask uploadTask = imagesRef.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("Debug","Firebase Can't Upload: "+exception.getMessage());
                    updateIdButton.setClickable(true);
                    uploadImageButton.setClickable(true);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    Log.d("Debug","Firebase Uploaded image successfully");
                    if (isLast == 1){
                        if (deleteFile(folderPath)){
                            if(deleteFile(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/"+customerID+"/Other_Pictures") && deleteFile(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/"+customerID+"/.temp")){
                                deleteFile(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/"+customerID);
                            }
                            Toast.makeText(AdminSettingsActivity.this,"All "+folderName+" Images are uploaded and deleted from the device successfully!",Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(AdminSettingsActivity.this,"All "+folderName+" Images are uploaded but couldn't delete from the device!",Toast.LENGTH_SHORT).show();
                        }
                        updateIdButton.setClickable(true);
                        uploadImageButton.setClickable(true);
                    }
                }
            });
        } catch (FileNotFoundException e) {
            updateIdButton.setClickable(true);
            uploadImageButton.setClickable(true);
            Log.d("Debug","Input File Stream Error: "+e.getMessage());
            e.printStackTrace();
        }
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
            return dir.delete();
        }
        catch (Exception e) {
            Log.d("Debug", e.getMessage());
            return false;
        }
    }
    private void createPrinterFirebaseFields(String printerID){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("printer"+printerID+"Data").setValue(" ");
        mDatabase.child("printer"+printerID+"Requests").setValue("done");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("xxx", "onActivityResult "+requestCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeTheAppFullScreen();
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