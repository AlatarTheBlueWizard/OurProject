package com.example.practicewizards;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.view.Surface;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * A class representing our Controller for the cameras
 * This class will take care of the threads created for the capture sessions
 * and file saving. Here we will control which camera is active, rear or front
 * facing. Also, for later milestones, this class will automatically flip the
 * camera from rear to front (e.g. close one camera and open the other)
 */
public class ManageCameras {
    // Capture session
    private CameraCaptureSession cameraCaptureSession;
    // Capture request
    private CaptureRequest captureRequest;
    // Builder for the Capture request
    private CaptureRequest.Builder buildCapRequest;
    // State Callback object to receive updates about the camera's state
    // Returns a camera device to set up the specific camera device
    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        /**
         * Method called when specific camera device has called CameraDevice.close()
         * @param camera
         */
        @Override
        public void onClosed(@NonNull CameraDevice camera) {

        }

        /**
         * Method to be called when specific camera device has finished opening
         * @param camera
         */
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

        }

        /**
         * Method to be called when specific camera device is no longer available to be used
         * @param camera
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        /**
         * Method to be called when camera device has a fatal error
         * @param camera
         * @param error
         */
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }; // End state call back declaration

    // Not sure what this is used for but it was included on CameraAPI tutorial
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    // Is flash supported?
    private boolean flashSupported;
    // Handler for background
    private Handler backgroundHandler;
    // Handler for background thread
    private HandlerThread backgroundHandlerThread;
    // Size of image
    private Size imageSize;
    // For Reading Image
    private ImageReader imageReader;

    /**
     * Non-default Constructor for ManageCameras to be called from main in the onCreate() method
     */
    ManageCameras() {
    }

    /**
     * Creates and opens a front or rear facing camera (based on boolean), and creates a
     * captureSession
     * @return void for now, could return an image view or a file
     */
    void createCaptureSession(boolean isRear) {
        if (isRear) {
            cameraCaptureSession = new CameraCaptureSession() {
                @NonNull
                @Override
                public CameraDevice getDevice() {
                    return null;
                }

                @Override
                public void prepare(@NonNull Surface surface) throws CameraAccessException {

                }

                @Override
                public void finalizeOutputConfigurations(List<OutputConfiguration> outputConfigs)
                        throws CameraAccessException {

                }

                @Override
                public int capture(@NonNull CaptureRequest request, @Nullable CameraCaptureSession.
                        CaptureCallback listener, @Nullable Handler handler)
                        throws CameraAccessException {
                    return 0;
                }

                @Override
                public int captureBurst(@NonNull List<CaptureRequest> requests, @Nullable
                        CameraCaptureSession.CaptureCallback listener, @Nullable Handler handler)
                        throws CameraAccessException {
                    return 0;
                }

                @Override
                public int setRepeatingRequest(@NonNull CaptureRequest request, @Nullable
                        CameraCaptureSession.CaptureCallback listener, @Nullable Handler handler)
                        throws CameraAccessException {
                    return 0;
                }

                @Override
                public int setRepeatingBurst(@NonNull List<CaptureRequest> requests, @Nullable
                        CameraCaptureSession.CaptureCallback listener, @Nullable Handler handler)
                        throws CameraAccessException {
                    return 0;
                }

                @Override
                public void stopRepeating() throws CameraAccessException {

                }

                @Override
                public void abortCaptures() throws CameraAccessException {

                }

                @Override
                public boolean isReprocessable() {
                    return false;
                }

                @Nullable
                @Override
                public Surface getInputSurface() {
                    return null;
                }

                @Override
                public void close() {

                }
            };
        }
    }
}
