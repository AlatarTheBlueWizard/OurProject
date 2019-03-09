package com.example.practicewizards;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.Arrays;

public class Main2Activity extends AppCompatActivity {
    private static int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private TextureView selfieView;
    private TextureView.SurfaceTextureListener selfieTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setUpCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    //CAMERA DEVICE FOR SELFIE VIEW
    private CameraDevice selfieCameraDevice;
    private CameraDevice.StateCallback selfieCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            selfieCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            selfieCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            selfieCameraDevice = null;
        }
    };

    //FUNCTIONS MEMBER VARIABLES
    private HandlerThread selfieBackgroundHandlerThread;
    private Handler selfieBackgroundHandler;
    private String selfieCameraDeviceId; // for setup of the camera
    private CaptureRequest.Builder selfieCaptureRequestBuilder;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Intent selfieIntent = getIntent();
        selfieView = (TextureView) findViewById(R.id.selfieView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if(selfieView.isAvailable()) {
            setUpCamera(selfieView.getWidth(), selfieView.getHeight());
            connectCamera();
        } else {
            selfieView.setSurfaceTextureListener(selfieTextListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Application won't run without camera services", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setUpCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    selfieCameraDeviceId = cameraId; //create a parameter for this
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                try {
                    cameraManager.openCamera(selfieCameraDeviceId, selfieCameraDeviceStateCallback, selfieBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "App requires access to camera", Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
            }
        } else {
            try {
                cameraManager.openCamera(selfieCameraDeviceId, selfieCameraDeviceStateCallback, selfieBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = selfieView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(selfieView.getWidth(), selfieView.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            selfieCaptureRequestBuilder = selfieCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            selfieCaptureRequestBuilder.addTarget(previewSurface);

            selfieCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(selfieCaptureRequestBuilder.build(), null, selfieBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Unable to setup camera preview", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void closeCamera() {
        if(selfieCameraDevice != null) {
            selfieCameraDevice.close();
            selfieCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        selfieBackgroundHandlerThread = new HandlerThread("GroupCameraThread");
        selfieBackgroundHandlerThread.start();
        selfieBackgroundHandler = new Handler(selfieBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        selfieBackgroundHandlerThread.quitSafely();
        try {
            selfieBackgroundHandlerThread.join();
            selfieBackgroundHandlerThread = null;
            selfieBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
