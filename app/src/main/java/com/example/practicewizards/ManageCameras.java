package com.example.practicewizards;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Size;

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
     * Constructor for ManageCameras to be called from main in the onCreate() method
     */
}
