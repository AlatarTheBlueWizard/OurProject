package com.example.practicewizards;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Toast;

public class Main2Activity extends AppCompatActivity {
    private TextureView selfieView;
    private TextureView.SurfaceTextureListener selfieTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(),"Selfie is Available!", Toast.LENGTH_LONG);
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

        if (selfieView.isAvailable()) {

        } else {
            selfieView.setSurfaceTextureListener(selfieTextListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
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

    private void closeCamera() {
        if(selfieCameraDevice != null) {
            selfieCameraDevice.close();
            selfieCameraDevice = null;
        }
    }
}
