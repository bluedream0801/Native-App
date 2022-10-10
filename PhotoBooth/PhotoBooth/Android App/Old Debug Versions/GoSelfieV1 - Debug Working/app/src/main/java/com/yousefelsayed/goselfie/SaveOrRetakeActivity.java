package com.yousefelsayed.goselfie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SaveOrRetakeActivity extends AppCompatActivity{

    private final static String TAG = "Debug";
    ImageView imgPrev ,goodIMG ,badIMG;
    String previewPath;
    GestureDetector gestureDetector;
    Bitmap imageWithWatermark;
    RelativeLayout loadingLayout;

    //Backend
    private static final String ACCESS_TOKEN = "sl.BOTnQusmFmgcGRnMY52Y8WpKReDh_0S-RGMzBG4FjYNj81vMI6u44ZK-9Rchs8ViTyMq3MAM71Ly4kbozSVJe7vrGvu5EvvKr1A3h7EGPrAFRGCk3r442ngI9a4rbYIAN_YKCZmo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_or_retake);
        init();
        setUpPrev();
        setUpListener();
    }

    private void init(){
        imgPrev = findViewById(R.id.imgPreview);
        goodIMG = findViewById(R.id.goodPicture);
        badIMG = findViewById(R.id.badPicture);
        loadingLayout = findViewById(R.id.loadingLayout);
        previewPath = getIntent().getStringExtra("path");
    }
    private void setUpListener() {
        goodIMG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
            }
        });
        badIMG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retakeImage();
            }
        });
    }
    private void setUpPrev() {
        File imgFile = new File(previewPath);
        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            Bitmap watermarkBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.watermark);
            imageWithWatermark = overlay(myBitmap,watermarkBitmap);
            imgPrev.setImageBitmap(imageWithWatermark);
            myBitmap = null;
        }else {
            finish();
        }
    }

    //Watermark Functions
    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        //Start Adding the Watermark
        bmp1 = Bitmap.createBitmap(bmp1, 0, 0, bmp1.getWidth(), bmp1.getHeight(), null, true);
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        int x = bmp1.getWidth() - bmp2.getWidth();
        int y = bmp1.getHeight() - bmp2.getHeight();
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, x, y, null);
        return bmOverlay;
    }

    //Image Location functions
    private void saveImage() {
        String imageName = previewPath.split("/")[7];
        String imagePathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+".temp"+"/";
        String finalPathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+"Good_Pictures"+"/";
        saveImageWithWaterMark(finalPathWithoutName,imageName.replace(".jpeg",".png"),imagePathWithoutName+imageName,0);
    }
    private void retakeImage() {
        String imageName = previewPath.split("/")[7];
        String imagePathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+".temp"+"/";
        String finalPathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+"Other_Pictures"+"/";
        saveImageWithWaterMark(finalPathWithoutName,imageName.replace(".jpeg",".png"),imagePathWithoutName+imageName,1);
        startMainActivity(1,"");
    }
    private void saveImageWithWaterMark(String finalPath,String imageName,String oldImagePath,int retakeMode) {
        loadingLayout.setVisibility(View.VISIBLE);
        if (imageWithWatermark == null){
            return;
        }
        File outputFolder = new File(finalPath);
        File output = new File(finalPath+imageName);
        if (!outputFolder.exists()) {
            if (!outputFolder.mkdirs()) {
                Log.d("Debug", "failed to create directory in saving with watermark");
            }
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(output);
                    imageWithWatermark.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    if (retakeMode == 0){
                        uploadImage(finalPath,imageName);
                    }else if(retakeMode == 1){
                        uploadImageForRetake(finalPath,imageName);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        deleteFile(oldImagePath);
    }
    public boolean deleteFile(String Path) {
        try {
            // delete the original file
            new File(Path).delete();
            return true;
        }
        catch (Exception e) {
            Log.d("Debug", e.getMessage());
            return false;
        }
    }
    private void startMainActivity(int retakeMode,String uploadedImageLink){
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra("retake",retakeMode);
        intent.putExtra("uploadedImageLink",uploadedImageLink);
        startActivity(intent);
        finish();
    }

    //Backend
    private void uploadImage(String filePath,String imageName){
        Log.d(TAG, "uploadImageForRetake Called: " + filePath +" ImageName: "+imageName);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("GoSelfie-Dropbox-Config").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        //Upload Image
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new FileInputStream(filePath+"/"+imageName)) {
                    FileMetadata metadata = client.files().uploadBuilder("/GoSelfie/GoodPictures/"+imageName).withMode(WriteMode.OVERWRITE).uploadAndFinish(in);
                    //Get ImageLink
                    SharedLinkSettings linkSettings = SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build();
                    SharedLinkMetadata sharedLinkMetadata = client.sharing().createSharedLinkWithSettings("/GoSelfie/GoodPictures/"+imageName, linkSettings);
                    SharedLinkMetadata sharedLinkMetadata1 = client.sharing().createSharedLinkWithSettings("/GoSelfie/GoodPictures", linkSettings);
                    startMainActivity(0,sharedLinkMetadata.getUrl());
                    Log.d(TAG, "Image Link: " + sharedLinkMetadata.getUrl()+" FolderLink: "+ sharedLinkMetadata1.getUrl());
                } catch (DbxException | IOException e) {
                    Log.d(TAG, "DropBox Upload Error: " + e.getMessage());
                    Toast.makeText(SaveOrRetakeActivity.this, "Can't connect to dropbox", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }
    private void uploadImageForRetake(String filePath,String imageName){
        Log.d(TAG, "uploadImageForRetake Called: " + filePath +" ImageName: "+imageName);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("GoSelfie-Dropbox-Config").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        //Upload Image
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new FileInputStream(filePath+"/"+imageName)) {
                    FileMetadata metadata = client.files().uploadBuilder("/GoSelfie/OtherPictures/"+imageName).withMode(WriteMode.OVERWRITE).uploadAndFinish(in);
                    //Get ImageLink
                    SharedLinkSettings linkSettings = SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build();
                    SharedLinkMetadata sharedLinkMetadata = client.sharing().createSharedLinkWithSettings("/GoSelfie/OtherPictures/"+imageName, linkSettings);
                    Log.d(TAG, "Image Link: " + sharedLinkMetadata.getUrl());
                } catch (DbxException | IOException e) {
                    Log.d(TAG, "DropBox Upload Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingLayout.setVisibility(View.GONE);
            }
        });
    }

}