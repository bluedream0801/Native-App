package com.yousefelsayed.goselfie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SaveOrRetakeActivity extends AppCompatActivity{

    private final static String TAG = "Debug";
    RelativeLayout printingLayout;
    Button closeQrCode,closePrintingScreen;
    ImageView goodIMG ,badIMG, imgPrev ,qrcodeImage,startPrint,showQrCode;
    String previewPath,CustomerID;
    int watermarkMode;
    GestureDetector gestureDetector;
    Bitmap imageWithWatermark;
    RelativeLayout loadingLayout, qrCodeLayout;
    SharedPreferences sp;

    //Backend
    private RequestQueue mQueue;
    //Firebase
    FirebaseStorage storage;
    StorageReference storageRef;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeTheAppFullScreen();
        setContentView(R.layout.activity_save_or_retake);
        init();
        setUpPrev();
        setUpListener();
    }

    private void makeTheAppFullScreen() {
        //For FullScreen
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.KEEP_SCREEN_ON;
        decorView.setSystemUiVisibility(uiOptions);
    }
    private void init(){
        sp = getSharedPreferences("GoSelfieSP",0);
        showQrCode = findViewById(R.id.showQrCode);
        imgPrev = findViewById(R.id.imgPreview);
        goodIMG = findViewById(R.id.goodPicture);
        badIMG = findViewById(R.id.badPicture);
        loadingLayout = findViewById(R.id.loadingLayout);
        qrCodeLayout = findViewById(R.id.qrCodeLayout);
        qrcodeImage = findViewById(R.id.QrcodeImage);
        closeQrCode = findViewById(R.id.closeQrCode);
        previewPath = getIntent().getStringExtra("path");
        CustomerID = getIntent().getStringExtra("ID");
        watermarkMode = getIntent().getIntExtra("watermark",1);
        startPrint = findViewById(R.id.startPrint);
        printingLayout = findViewById(R.id.printingLayout);
        closePrintingScreen = findViewById(R.id.closePrintingScreen);
        //Backend
        storage = FirebaseStorage.getInstance();
        mQueue = Volley.newRequestQueue(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (sp.getInt("printSettings",1) == 1){
            startPrint.setVisibility(View.VISIBLE);
        }else {
            startPrint.setVisibility(View.GONE);
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void setUpListener() {
        goodIMG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (watermarkMode == 1){
                    saveImage();
                }else {
                    saveImageWithoutWatermark(0);
                }
            }
        });
        badIMG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (watermarkMode == 1){
                    retakeImage();
                }else {
                    saveImageWithoutWatermark(1);
                }
            }
        });
        if (sp.getInt("qrcodeButton",1) != 1){
            showQrCode.setVisibility(View.GONE);
        }else {
            showQrCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isNetworkAvailable()) {
                        loadingLayout.setVisibility(View.VISIBLE);
                        uploadImage();
                    } else {
                        Toast.makeText(SaveOrRetakeActivity.this, "Please Connect to the internet", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        closeQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (watermarkMode == 1){
                    saveImage();
                }else {
                    saveImageWithoutWatermark(0);
                }
                qrCodeLayout.setVisibility(View.GONE);
            }
        });
        closePrintingScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (watermarkMode == 1){
                    saveImage();
                }else {
                    saveImageWithoutWatermark(0);
                }
                printingLayout.setVisibility(View.GONE);
            }
        });
        startPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkAvailable()){
                    startPrint.setClickable(false);
                    printingLayout.setVisibility(View.VISIBLE);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            imageWithWatermark.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            uploadImageForPrint("PRINTER_"+sp.getString("PrinterID","1")+"_"+previewPath.split("/")[8],bos.toByteArray());
                        }
                    });
                }else {
                    Toast.makeText(SaveOrRetakeActivity.this, "No internet,Please check your connection and try again...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void setUpPrev() {
        File imgFile = new File(previewPath);
        if (watermarkMode == 1){
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                Bitmap watermarkBitmap = getResizedBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.watermark),400);
                imageWithWatermark = overlay(myBitmap,watermarkBitmap);
                imgPrev.setImageBitmap(imageWithWatermark);
                myBitmap = null;
                watermarkBitmap = null;
            }else {
                finish();
            }
        }else {
            imageWithWatermark = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imgPrev.setImageURI(Uri.fromFile(imgFile));
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
        String imageName = previewPath.split("/")[8];
        String imagePathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+".temp"+"/";
        String finalPathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+CustomerID+"/Good_Pictures"+"/";
        saveImageWithWaterMark(finalPathWithoutName,imageName,imagePathWithoutName+imageName,0);
    }
    private void retakeImage() {
        String imageName = previewPath.split("/")[8];
        String imagePathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+".temp"+"/";
        String finalPathWithoutName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+CustomerID+"/Other_Pictures"+"/";
        saveImageWithWaterMark(finalPathWithoutName,imageName.replace(".jpeg",".jpg"),imagePathWithoutName+imageName,1);
    }
    private void saveImageWithoutWatermark(int retake){
        File goodImagePathWithName = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie/"+CustomerID+"/Good_Pictures"+"/"+previewPath.split("/")[8]);
        File badImagePathWithName = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie/"+CustomerID+"/Other_Pictures"+"/"+previewPath.split("/")[8]);
        File prevFile = new File(previewPath);
        if (retake == 1){
            move(prevFile,badImagePathWithName,Environment.getExternalStorageDirectory() + "/" +Environment.DIRECTORY_DCIM + "/" +"GoSelfie/"+CustomerID+"/Other_Pictures");
            if (isNetworkAvailable()) {
                uploadToFirebase(badImagePathWithName.getPath(), previewPath.split("/")[8], CustomerID,"Other_Pictures");
            }
            startMainActivity(1);
        }else {
            move(prevFile,goodImagePathWithName,Environment.getExternalStorageDirectory() + "/" +Environment.DIRECTORY_DCIM + "/" +"GoSelfie/"+CustomerID+"/Good_Pictures");
            if (isNetworkAvailable()) {
                uploadToFirebase(goodImagePathWithName.getPath(), previewPath.split("/")[8], CustomerID,"Good_Pictures");
            }
            startMainActivity(0);
        }
    }
    private void saveImageWithWaterMark(String finalPath,String imageName,String oldImagePath,int retakeMode) {
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

        //Causes temporary memory leak for better user experience
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(output);
                    imageWithWatermark.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    if (isNetworkAvailable()){
                        if (retakeMode != 1){
                            uploadToFirebase(output.getPath(),imageName,CustomerID,"Good_Pictures");
                        }else {
                            uploadToFirebase(output.getPath(),imageName,CustomerID,"Other_Pictures");
                        }
                    }
                    outputStream = null;
                    imageWithWatermark = null;
                    deleteFile(oldImagePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        if (retakeMode == 0){
            startMainActivity(0);
        }else if(retakeMode == 1){
            startMainActivity(1);
        }
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
    public void copyFile(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {

                copyFile(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);

            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }

    }
    public void move(File sourceLocation, File targetLocation,String targetFolder){
        try {
            File mainFolder = new File(targetFolder);
            if (!mainFolder.exists()){
                mainFolder.mkdir();
            }
            copyFile(sourceLocation,targetLocation);
            deleteFile(sourceLocation.getAbsolutePath().toString());
        } catch (IOException e) {
            Log.d(TAG,"MoveFailed: "+e.getMessage());
            e.printStackTrace();
        }

    }
    private void startMainActivity(int retakeMode){
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra("retake",retakeMode);
        startActivity(intent);
        finish();
    }

    //Backend
    private void uploadImage(){
        //Upload Image
        String url = "https://gopho2.com/picture/uploadImage.php?";
        String imageData = getTextImage();
        String imageName = previewPath.split("/")[8].replace(".jpg","");
        Log.d(TAG, " uploadImage Called: ");
        StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject respObj = new JSONObject(response);
                    Log.d("Debug","Res: "+response);
                    String jsonArray = respObj.getString("response_message");
                    Log.d("Debug","response_message: "+jsonArray);
                    try {
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        //Bitmap bitmap = barcodeEncoder.encodeBitmap("https://gopho2.com/picture/"+imageName+".png", BarcodeFormat.QR_CODE, 400, 400);
                        Bitmap bitmap = barcodeEncoder.encodeBitmap("https://gopho2.com/index.php/"+imageName, BarcodeFormat.QR_CODE, 400, 400);
                        qrcodeImage.setImageBitmap(bitmap);
                        qrCodeLayout.setVisibility(View.VISIBLE);
                    } catch(Exception e) {
                        Log.d("Debug","qr Error: "+e.getMessage());
                    }
                    loadingLayout.setVisibility(View.GONE);

                } catch (JSONException e) {
                    Log.d("Debug","Res Error: "+e.getMessage());
                    loadingLayout.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Debug","ErrorInUploadImage: "+error.getMessage()+" E2: "+error.networkResponse);
                loadingLayout.setVisibility(View.GONE);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("image", imageData);
                params.put("imageName", imageName);
                return params;
            }
        };
        mQueue.add(request);
    }
    public String getTextImage(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        getResizedBitmap(imageWithWatermark,2500).compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        //imageWithWatermark.compress(Bitmap.CompressFormat.PNG,100,byteArrayOutputStream);
        byte[] image = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(image, Base64.DEFAULT);
    }
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
    private void uploadToFirebase(String filePath,String imageName,String customerID,String folderName){
        storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child(customerID+"/"+folderName+"/"+imageName);
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
                }
            });
        } catch (FileNotFoundException e) {
            Log.d("Debug","Input File Stream Error: "+e.getMessage());
            e.printStackTrace();
        }
    }


    //Print Request Functions
    private void uploadImageForPrint(String imageName,byte[] bitmapdata){
        storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("toPrint/"+imageName);
        try {
            Log.d("Debug","ImageBitmapBytes: "+bitmapdata.length);
            ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
            //InputStream stream = new FileInputStream(new File(previewPath));
            UploadTask uploadTask = imagesRef.putStream(bs);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(getApplicationContext(), "Can't upload image for print request, Error: "+exception.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("Debug","Firebase printer Can't Upload: "+exception.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    sendFileToServer(imageName);
                    Log.d("Debug","Firebase printer Uploaded image successfully ImageName: "+imageName);
                }
            });
            bitmapdata = null;
        } catch (Exception e) {
            Log.d("Debug","Input File Stream Error: "+e.getMessage());
            e.printStackTrace();
        }
    }
    private void sendFileToServer(String ImageName){
        Log.d("Debug","SendFileToServer Print Started");
        //Check for requests
        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Requests").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.d("Debug", "Printer Error getting data", task.getException());
                }
                else {
                    if (String.valueOf(task.getResult().getValue()).equals("done")){
                        startFirstPrintRequest(ImageName);
                    }else {
                        startPrintRequest(ImageName);
                    }
                    Log.d("Debug", String.valueOf(task.getResult().getValue()));
                }
            }
        });
    }
    private void startFirstPrintRequest(String ImageName){
        //Because in the firebase 'PrinterPRINTER_IDData' wasn't equal to 'done' this function was called
        Log.d("Debug","Start Print Array");
        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Data").setValue(ImageName);
        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Requests").setValue("1");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"Printing Request Sent Successfully!",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void startPrintRequest(String ImageName){
        Log.d("Debug","Add To print Array");
        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Data").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Error in updating data for print,Error: "+task.getException(), Toast.LENGTH_SHORT).show();
                    Log.d("Debug", "Error getting data", task.getException());
                }
                else {
                    String[] data = String.valueOf(task.getResult().getValue()).split(",");
                    String finalString = "";
                    //Make sure ImageName is not null
                    if (ImageName.length() > 3){
                        //The split function always return 1 even if there's no data ... So to prevent sending empty data we use this if statement
                        //Empty data as For Example: img1,,img2 ...etc
                        if (data[0].length() > 3){
                            //data[0].length() > 3 This condition make sure that the 1 returned isn't an empty string
                            // and we used number 3 because at least the image will end with .jpg
                            for(int i = 0;i< data.length;i++){
                                if (i == 0){
                                    finalString = ImageName + "," +data[i];
                                }else {
                                    finalString = finalString + "," + data[i];
                                }
                            }
                        }else {
                            finalString = ImageName;
                        }
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startPrint.setClickable(true);
                                Toast.makeText(getApplicationContext(), "Couldn't update ImageData in database! Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    Log.d("Debug","Print Final Array: "+finalString);
                    mDatabase.child("printer"+sp.getString("PrinterID","1")+"Data").setValue(finalString);
                    updatePrintRequestsNumber();
                    Log.d("Debug", String.valueOf(task.getResult().getValue()));
                }
            }
        });
    }
    private void updatePrintRequestsNumber(){
        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Requests").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startPrint.setClickable(true);
                            Toast.makeText(getApplicationContext(), "Error in updating data for print,Error: "+task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.d("Debug", "Error getting data", task.getException());
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Print Request is sent successfully!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    if (task.getResult().getValue() != null && !task.getResult().getValue().toString().equals("")){
                        int currentReq = Integer.parseInt(task.getResult().getValue().toString());
                        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Requests").setValue(String.valueOf(currentReq+1));
                        Log.d("Debug", String.valueOf(task.getResult().getValue()));
                    }else {
                        mDatabase.child("printer"+sp.getString("PrinterID","1")+"Requests").setValue("1");
                    }
                }
            }
        });
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