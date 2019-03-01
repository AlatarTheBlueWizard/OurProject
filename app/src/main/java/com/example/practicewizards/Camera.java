package com.example.practicewizards;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
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
     * Getter for current camera id
     * @return cameraId
     */
    public String getCameraId() {
        return cameraId;
    }

    /**
     * Setter for current camera id
     * @param cameraId received from Camera Manager
     */
    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    /**
     * Getter for current camera devide
     * @return a cameraDevice
     */
    public CameraDevice getCameraDevice() {
        return cameraDevice;
    }

    /**
     * Setter for current camera device
     * @param cameraDevice received from Camera Manager, or anyone else
     */
    public void setCameraDevice(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
    }

    /**
     * Constructor for a Camera
     * @param isRear determines whether the rear camera will be open or the front facing one will
     *               be opened
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Camera(boolean isRear) {
        if (isRear) {
            // Construct camera using rear facing view
        }
        else {
            // Construct camera using front facing view
        }
    }

    /**
     * Opens the member object camera
     */
    void openCamera() {

    }

    /**
     * Closes the member object camera
     */
    void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * @return characteristics of the current camera
     */
    CameraCharacteristics getCharacteristics() {
        // Working on this
        // return new CameraManager.getCameraCharacteristics(cameraDevice.getId());
        return null;
    }

}
