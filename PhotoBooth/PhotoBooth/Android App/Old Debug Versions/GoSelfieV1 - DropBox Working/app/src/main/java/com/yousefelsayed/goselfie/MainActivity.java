package com.yousefelsayed.goselfie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "Debug";
    private TextView countDownText ,goodPicturesNumber;
    private ImageView takeImage ,flashOn ,flashOff ,adminPanelLogin;
    private LinearLayout flashesSelect;
    private View flashWhiteView;
    private int flashMode = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    SharedPreferences sp;
    Intent intent;

    //CameraPreview
    private TextureView textureView;
    private TextureView.SurfaceTextureListener textureListener;
    private Size mPreviewSize;
    private String CameraID;
    private CaptureRequest mPreviewCaptureRequest;
    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback;
    //Camera
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceCallback;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    //Saving Image
    private static File imageFile;
    private ImageReader imageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            backgroundHandler.post(new ImageSaver(imageReader.acquireNextImage()));
        }
    };
    private static class ImageSaver implements Runnable {
        private final Image image;

        private ImageSaver(Image image1){
            image = image1;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(imageFile);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
                if (fileOutputStream != null){
                    try {
                        fileOutputStream.close();
                        byteBuffer = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //Audio
    MediaPlayer bgMediaPlayer,countDownPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeTheAppFullScreen();
        setContentView(R.layout.activity_main);
        init();
        setListeners();
        checkForRetake();
        startButtonAnimation();
        checkGoodPicturesNumber();

    }
    private void makeTheAppFullScreen() {
        //For FullScreen
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.KEEP_SCREEN_ON;
        decorView.setSystemUiVisibility(uiOptions);
    }

    //Layout Views and Listeners
    private void init() {
        textureView = findViewById(R.id.textureView);
        takeImage = findViewById(R.id.btnCapture);
        flashWhiteView = findViewById(R.id.flashWhiteLayout);
        flashesSelect = findViewById(R.id.flashesLayout);
        flashOn = findViewById(R.id.flashOn);
        flashOff = findViewById(R.id.flashOff);
        countDownText = findViewById(R.id.countDownText);
        goodPicturesNumber = findViewById(R.id.goodPicturesNumber);
        adminPanelLogin = findViewById(R.id.adminPanelButton);
        sp = getSharedPreferences("GoSelfieSP",0);
        intent = new Intent(MainActivity.this,SaveOrRetakeActivity.class);
        setUpFlashSettings();
    }
    private void setListeners() {
        //CameraTexture
        textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera(width, height);
                transformImage(width,height);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };
        mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);

            }
        };
        //Camera
        cameraDeviceCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice1) {
                cameraDevice = cameraDevice1;
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice1) {
                cameraDevice1.close();
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice1, int i) {
                cameraDevice1.close();
                cameraDevice = null;

            }
        };
        //Camera and Preview Listeners
        if (textureView != null) {
            textureView.setSurfaceTextureListener(textureListener);
        }
        if (takeImage != null) {
            takeImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("Debug1","TakeImageButtonClicked");
                    startCountdown();
                }
            });
        }
        //Flash Settings Listeners
        flashesSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flashesSelect.setClickable(false);
                flashOn.setClickable(true);
                flashOff.setClickable(true);

                flashOn.setVisibility(View.VISIBLE);
                flashOff.setVisibility(View.VISIBLE);
            }
        });
        flashOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFlashMode(1);
            }
        });
        flashOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFlashMode(0);
            }
        });

        //Views
        adminPanelLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,AdminLoginActivity.class);
                startActivity(intent);
            }
        });
    }
    private void startCountdown() {
        Log.d("Debug1","CountDownStarted");
        if (bgMediaPlayer != null){
            stopBackgroundAudio();
        }
        startCountDownAudio();
        flashesSelect.setClickable(false);
        takeImage.setVisibility(View.INVISIBLE);
        countDownText.setVisibility(View.VISIBLE);
        new CountDownTimer(4000, 1000) {
            public void onTick(long millisUntilFinished) {
                countDownText.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                Log.d("Debug1","CountDownEnded");
                countDownText.setVisibility(View.GONE);
                takeImage.setVisibility(View.VISIBLE);
                startFlash();
                captureStillImage();
                flashesSelect.setClickable(true);
            }
        }.start();
    }
    private void checkForRetake() {
        if (getIntent().getIntExtra("retake", 0) == 1) {
            startCountdown();
        }
    }
    private void checkGoodPicturesNumber(){
        File mainFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" + "GoSelfie/" +sp.getString("ID","0")+"/Good_Pictures" + "/");
        if (mainFolder.exists()){
            File[] list = mainFolder.listFiles();
            if (list != null){
                goodPicturesNumber.setText(list.length + "");
            }else {
                goodPicturesNumber.setText("0");
            }
        }else {
            goodPicturesNumber.setText("0");
        }
    }

    //Audio
    private void checkIfCanStartBackgroundAudio(){
        if (countDownPlayer != null){
            if(!countDownPlayer.isPlaying()){
                setupBackgroundAudio();
            }
        }else {
            setupBackgroundAudio();
        }
    }
    private void setupBackgroundAudio(){
        bgMediaPlayer = MediaPlayer.create(this, R.raw.nothing_cut);
        if (!bgMediaPlayer.isPlaying()){
            bgMediaPlayer.setLooping(true);
            bgMediaPlayer.start();
        }
    }
    private void stopBackgroundAudio(){
        if (bgMediaPlayer != null){
            bgMediaPlayer.stop();
            bgMediaPlayer = null;
        }
    }
    private void startCountDownAudio(){
        countDownPlayer = MediaPlayer.create(this, R.raw.countdown_new);
        countDownPlayer.start();
    }

    //Animation
    private void startButtonAnimation() {
        takeImage.animate()
                .scaleY(1.1f)
                .scaleX(1.1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        stopButtonAnimation();
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                })
                .setDuration(500);
    }
    private void stopButtonAnimation(){
        takeImage.animate()
                .scaleY(1.0f)
                .scaleX(1.0f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        startButtonAnimation();
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                })
                .setDuration(500);
    }

    //Flash Settings
    private void setUpFlashSettings() {
        flashOn.setClickable(false);
        flashOff.setClickable(false);
    }
    private void changeFlashMode(int mode) {
        flashMode = mode;
        switch (mode) {
            case 0:
                flashOn.setVisibility(View.GONE);
                flashOff.setVisibility(View.VISIBLE);
                break;
            case 1:
                flashOn.setVisibility(View.VISIBLE);
                flashOff.setVisibility(View.GONE);
                break;
        }
        flashOn.setClickable(false);
        flashOff.setClickable(false);
        flashesSelect.setClickable(true);
    }
    private void startFlash() {
        if (flashMode == 0){
            return;
        }
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);
        flashWhiteView.setVisibility(View.VISIBLE);
    }
    private void stopFlash() {
        if (flashMode == 0){
            return;
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            flashWhiteView.setVisibility(View.GONE);
                            WindowManager.LayoutParams layout = getWindow().getAttributes();
                            layout.screenBrightness = 0.5F;
                            getWindow().setAttributes(layout);
                        }
                    });

                }
            }
        };
        thread.start();
        thread = null;
    }

    //CameraPreview
    private void setUpCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : cameraManager.getCameraIdList()) {
                //Use The Front Camera
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                //Get Preview Size
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //Filter Sizes
                mPreviewSize = chooseImageSize(map.getOutputSizes(SurfaceTexture.class));
                //mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                CameraID = cameraID;

                //Set Taken Photo Size
                Size largestImageSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size size, Size t1) {
                        return Long.signum(size.getWidth() * size.getHeight() - t1.getWidth() * t1.getHeight());
                    }
                });
                //imageReader = ImageReader.newInstance(largestImageSize.getWidth(), largestImageSize.getHeight(), ImageFormat.YUV_420_888 ,1);
                imageReader = ImageReader.newInstance(largestImageSize.getWidth(), largestImageSize.getHeight(), ImageFormat.JPEG ,1);
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener,backgroundHandler);
                checkIfCanStartBackgroundAudio();
                return;
            }
        } catch (CameraAccessException e) {
            Log.d(TAG,"Error in setUpCamera fun: "+e.getMessage());
            Toast.makeText(MainActivity.this,"Error in setUpCamera fun: "+e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    private void transformImage(int width,int height){
        if (textureView == null || mPreviewSize == null){
            return;
        }
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0,0,width,height);
        RectF previewRectF = new RectF(0,0,mPreviewSize.getHeight(),mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            previewRectF.offset(centerX - previewRectF.centerX(),centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF,previewRectF,Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) width / mPreviewSize.getWidth() , (float) height / mPreviewSize.getHeight());
            matrix.postScale(scale,scale,centerX,centerY);
            matrix.postRotate(90 * (rotation - 2),centerX,centerY);
        }
        textureView.setTransform(matrix);
    }
    //Filter All Dimensions
    protected Size chooseImageSize(Size[] choices) {
        List<Size> smallEnough = new ArrayList<>();

        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                smallEnough.add(size);
            }
        }
        if (smallEnough.size() > 0) {
            return Collections.max(smallEnough, new CompareSizeByArea());
        }

        return choices[choices.length - 1];
    }
    public static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
    //OldFilter
    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : mapSizes) {
            if (height > width) {
                if (option.getHeight() > height && option.getWidth() > width) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getHeight() > width && option.getWidth() > height) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size size, Size t1) {
                    return Long.signum(size.getWidth() * size.getHeight() - t1.getHeight() * t1.getWidth());
                }
            });
        } else {
            return mapSizes[0];
        }
    }
    private void createCameraPreviewSession(){
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewCaptureRequestBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface , imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null)
                        return;
                    try {
                        mPreviewCaptureRequest = mPreviewCaptureRequestBuilder.build();
                        mCameraCaptureSession = cameraCaptureSession;
                        mCameraCaptureSession.setRepeatingRequest(mPreviewCaptureRequest,mSessionCaptureCallback,backgroundHandler);
                    }catch (CameraAccessException e){
                        Log.d(TAG,"Error in createCameraPreviewSession fun: "+e.getMessage());
                        Toast.makeText(MainActivity.this,"Error in createCameraPreviewSession fun: "+e.getMessage(),Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d("Debug","onConfigureFailed...");
                }
            },null);
        }catch (CameraAccessException e){
            Log.d(TAG,"Error in createCameraPreviewSession fun: "+e.getMessage());
            Toast.makeText(MainActivity.this,"Error in createCameraPreviewSession fun: "+e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //Camera
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CAMERA_PERMISSION);
                return;
            }
            cameraManager.openCamera(CameraID, cameraDeviceCallback, backgroundHandler);
        }catch (CameraAccessException e){
            Log.d(TAG,"Error in open camera fun: "+e.getMessage());
            Toast.makeText(MainActivity.this,"Error in open camera fun: "+e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    private void closeCamera(){
        if (mCameraCaptureSession != null){
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null){
            imageReader.close();
            imageReader = null;
        }
    }
    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("Camera2 BackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    private void closeBackgroundThread() {
        backgroundThread.quitSafely();
        try{
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
    private void captureStillImage(){
        Log.d("Debug1","CaptureStillImageFunctionStarted");
        setUpImageFilePath();
        try{
            Log.d("Debug1","captureStillImage Try Started");
            CaptureRequest.Builder captureStillBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStillBuilder.addTarget(imageReader.getSurface());
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATION.get(rotation));

            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.d("Debug","Image Has been taken and saved in: "+imageFile);
                    intent.putExtra("path",imageFile.toString());
                    intent.putExtra("watermark",sp.getInt("watermark",1));
                    intent.putExtra("ID",sp.getString("ID","0"));
                    //To Stop MemoryLeak Don't write code below of handler
                    stopFlash();
                    closeCamera();
                    startActivity(intent);
                    finish();
                }
            };
            //This is running in the background thread anyway no need for background handler
            Log.d("Debug1","CameraCaptureSessionWillBeCalled");
            mCameraCaptureSession.capture(captureStillBuilder.build(),captureCallback,null);
            Log.d("Debug1","cameraCaptureSessionIsCalled");
        }catch (CameraAccessException e){
            Log.d(TAG,"Error in open captureStillImage fun: "+e.getMessage());
            Toast.makeText(MainActivity.this,"Error in open captureStillImage fun: "+e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    File createImageFile() throws IOException {
        Log.d("Debug1","CreateImageStarted");
        String pathD = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/" +"GoSelfie"+ "/"+sp.getString("ID","0")+"/";
        File mediaStorageDir = new File(pathD, ".temp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File image = new File(mediaStorageDir,"IMG"+"_"+ timeStamp+".jpeg");
        return image;

    }
    private void setUpImageFilePath(){
        Log.d("Debug1","SetUpImageFilePathStarted");
        try {
            imageFile = createImageFile();
        }catch (IOException e){
            Log.d(TAG,"Error in setUpImageFilePath fun: "+e.getMessage());
            Toast.makeText(MainActivity.this,"Error in setUpImageFilePath fun: "+e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }



    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        /* Workaround a probable Android bug with fullscreen activities:
         * on resume status bar hides and black margin stays,
         * reproducible half of the time when coming back from lock screen
         * (tested on HTC Wildfire)
         * No idea why but this empty overload method fixed it.
         */
    }
    @Override
    protected void onResume() {
        super.onResume();
        makeTheAppFullScreen();
        openBackgroundThread();
        checkIfCanStartBackgroundAudio();
        if(textureView.isAvailable()) {
            setUpCamera(textureView.getWidth(),textureView.getHeight());
            transformImage(textureView.getWidth(),textureView.getHeight());
            openCamera();
        }
        else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        closeCamera();
        closeBackgroundThread();
        stopBackgroundAudio();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please grant requested permissions.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}