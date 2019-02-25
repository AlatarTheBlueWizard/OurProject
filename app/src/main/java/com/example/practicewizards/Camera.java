package com.example.practicewizards;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import java.util.List;

/**
 * Class holding a single cameraDevice personalized for us to use and manipulate
 */
public class Camera {
    // Camera object
    public CameraDevice cameraDevice;

    /**
     * Constructor for a Camera
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Camera() {
        cameraDevice = new CameraDevice() {
            @NonNull
            @Override
            public String getId() {
                return null;
            }

            @Override
            public void createCaptureSession(@NonNull List<Surface> outputs, @NonNull CameraCaptureSession.StateCallback callback, @Nullable Handler handler) throws CameraAccessException {

            }

            @Override
            public void createCaptureSessionByOutputConfigurations(List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, @Nullable Handler handler) throws CameraAccessException {

            }

            @Override
            public void createReprocessableCaptureSession(@NonNull InputConfiguration inputConfig, @NonNull List<Surface> outputs, @NonNull CameraCaptureSession.StateCallback callback, @Nullable Handler handler) throws CameraAccessException {

            }

            @Override
            public void createReprocessableCaptureSessionByConfigurations(@NonNull InputConfiguration inputConfig, @NonNull List<OutputConfiguration> outputs, @NonNull CameraCaptureSession.StateCallback callback, @androidx.annotation.Nullable Handler handler) throws CameraAccessException {

            }

            @Override
            public void createConstrainedHighSpeedCaptureSession(@androidx.annotation.NonNull List<Surface> outputs, @NonNull CameraCaptureSession.StateCallback callback, @Nullable Handler handler) throws CameraAccessException {

            }

            @androidx.annotation.NonNull
            @Override
            public CaptureRequest.Builder createCaptureRequest(int templateType) throws CameraAccessException {
                return null;
            }

            @androidx.annotation.NonNull
            @Override
            public CaptureRequest.Builder createReprocessCaptureRequest(@NonNull TotalCaptureResult inputResult) throws CameraAccessException {
                return null;
            }

            @Override
            public void close() {

            }
        };
    }
}
