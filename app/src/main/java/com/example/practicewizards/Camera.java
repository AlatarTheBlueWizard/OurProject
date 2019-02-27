package com.example.practicewizards;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.support.annotation.RequiresApi;


import java.util.List;

/**
 * Class holding a single cameraDevice personalized for us to use and manipulate
 */
public class Camera {
    // Tag
    private static final String TAG = "pracitcewizards.Camera";
    // Camera ID
    private String cameraId;
    // Camera object
    public CameraDevice cameraDevice;



    /**
     * Constructor for a Camera
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Camera(boolean isRear) {
        if (isRear) {
            // Construct camera using rear facing view
        }
    }




}
