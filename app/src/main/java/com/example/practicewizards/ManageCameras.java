package com.example.practicewizards;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.util.List;
import android.support.annotation.NonNull;
import android.widget.Toast;

/**
 * A class representing our Controller for the cameras
 * This class will take care of the threads created for the capture sessions
 * and file saving. Here we will control which camera is active, rear or front
 * facing. Also, for later milestones, this class will automatically flip the
 * camera from rear to front (e.g. close one camera and open the other)
 */
public class ManageCameras {
    // Camera Manager Object
    private CameraManager cameraManager;
    // TWO CAMERA OBJECTS
    Camera cam1; // Rear facing
    Camera cam2; // Front facing

    // ManageCameras contains a Weak Reference to whatever activity creates an object of it and
    // uses it
    WeakReference<AppCompatActivity> activityWeakReference;

    // Request Code for camera
    // Declared to some value
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 200;

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
            if (cam1.getCameraDevice() != null) {
                // Close our camera
                cam1.closeCamera();
            }
        }

        /**
         * Method to be called when specific camera device has finished opening
         * @param camera
         */
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // Set our camera to the one returned from the state call back
            // Not final yet, need to determine rear or front, will do later
            cam1.setCameraDevice(camera);
        }

        /**
         * Method to be called when specific camera device is no longer available to be used
         * @param camera
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            // Close camera and set our camera object keeping track of the camera device object
            // to null
            camera.close();
            cam1.setCameraDevice(null);
        }

        /**
         * Method to be called when camera device has a fatal error
         * @param camera
         * @param error
         */
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            // Close camera and set our camera object keeping track of the camera device object
            // to null
            camera.close();
            cam1.setCameraDevice(null);
        }
    }; // End state call back declaration
    // Camera Characteristics for the certain camera
    private CameraCharacteristics cameraCharacteristics;

    // Is flash supported?
    private boolean flashSupported;

    // Background Handler
    private Handler backgroundHandler;      // For handling threads
    // Background Thread
    private HandlerThread backgroundThread; // The specific background thread, runnable

    // Size of image
    private Size imageSize;
    // For Reading Image
    private ImageReader imageReader;
    // Tag for this class
    private static final String TAG = "ManageCameras";

    /**
     * Non-default Constructor for ManageCameras to be called from main in the onCreate() method
     */
    ManageCameras() {
        // Create our two cameras, one for rear, one for front facing
        // OR WE COULD START OUR CAMERAS AT NULL... setup cameras will create the cameras
        cam1 = new Camera(true);
        cam2 = new Camera(false);
    }

    /**
     * Opens a camera
     * @param isRear denotes which camera to open
     * @return returns a string representing the camera's id which was open
     */
    String open(boolean isRear) {
        // If is rear is true, open up rear camera (#1)
        if (isRear) {
            // Call its open method
            cam1.openCamera();
            return cam1.getCameraId(); // Return its ID
        }
        // Rear is false, open up front facing camera (#2)
        else {
            // Call its open method
            cam2.openCamera();
            return cam2.getCameraId(); // Return its ID
        }
    }

    /**
     * Sets up the rear facing camera and the front facing camera
     * This function will get the front facing cameraId and the rear facing cameraId
     * and set cam1 and cam2 Id's
     * @param width EXPLAIN THIS BENJAMIN
     * @param height Explain
     * @param context Needed for getting the system service of that context, in our
     *                case the view will need to pass it's context to us
     */
    void setupCameras(int width, int height, Context context) {
        // Create our cameraManager to equal the camera system service returned by the
        // context received
        // cameraManager will return us a list of the camera id's available
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        // Try to get the id's, may throw a CameraAccessException
        try {
            // For all the camera Ids returned by cameraManger, keep looping
            for (String cameraId : cameraManager.getCameraIdList()) {
                // Set our cameraCharacteristics object to the particular camera accessed through
                // Camera manager at camera id
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                // May produce a NullPtrException in the case the key is not found
                try {
                    // Now we can check the characteristics of the camera to see if it's rear or front
                    // facing
                    // Get a value from the map giving get the LENS_FACING key to see if the facing
                    // lens is rear or front
                    // IF lens is back facing, then set our cam1 object
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                            CameraCharacteristics.LENS_FACING_BACK) {
                        cam1.setCameraId(cameraId); // CHANGE THIS
                    }
                    // Check for front facing lens
                    else if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                             CameraCharacteristics.LENS_FACING_FRONT) {
                        cam2.setCameraId(cameraId); // CHANGE THIS
                    }
                    // Check if both cameraId's for front and back were found
                    if (cam1.getCameraId() != null && cam2.getCameraId() != null) {
                        // We're done, found both, return out
                        return;
                    }
                }
                catch (NullPointerException nullPtrExc) {
                    // Log Error message
                    Log.e(TAG, "Lens Facing Key Not Found");
                    nullPtrExc.printStackTrace(); // Print stack
                }
            }
        }
        catch (CameraAccessException camAccessExc) {
            // Log an error
            Log.e(TAG, "Error in retrieving camera ids");
            camAccessExc.printStackTrace(); // Print the Stack
        }
    }

    /**
     * Connects to cameras. EXPLAIN MORE
     * IMPORTANT: This method setupCameras() to be called first, as
     * setupCameras() allocates our cameraManager object to equal
     * context.getSystemService(Context.CAMERA_SERVICE);
     */
    void connectCameras() {
        // Try to connect, throw if something goes wrong
        try {
            // Make sure camera manager is not null
            if (cameraManager != null) {
                // Try to open camera, could through CameraAccessException
                try {
                    // Preceding call requires a minimum API level, check through IF statement
                    // See if sdk_int is >= 23 (Marshmallow Api Level)
                    // Prompt user to grant or deny permission to use Marshmallow API on their specific
                    // Device. This occurs once, during installation and first run-through of the app
                    // If the user grants permission, then else is always run, if not then hopefully
                    // user will grant because they can't use our app if not!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Check permissions using weakReference's compat activity through get()
                        // giving it the Manifest camera permissions static variable
                        // If true, then permission has been granted through package manager variable
                        if (ContextCompat.checkSelfPermission(activityWeakReference.get(),
                                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            // Open camera through camera id, the stateCallBack, and our created
                            // backgroundHandler
                            cameraManager.openCamera(cam1.getCameraId(), cameraStateCallback,
                                    backgroundHandler);
                        }
                        // Else ask for permissions
                        else {
                            // If user denied permissions once (at installation) but still wants to
                            // run our app, ask again (diligence never fails!)
                            // Call fancy show request permission through whatever reference we're
                            // Using. Give it Manifest Camera Permission variable
                            if (activityWeakReference.get().
                                    shouldShowRequestPermissionRationale(
                                            Manifest.permission.CAMERA)) {
                                // Make a simple toast, could do something different if we choose to
                                Toast.makeText(activityWeakReference.get(),
                                        "App requires access to back and front cameras",
                                        Toast.LENGTH_LONG).show();
                            }
                            // Now actually request the permissions through reference
                            activityWeakReference.get().requestPermissions(
                                    new String[] {Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION_RESULT);

                        }
                    }
                    // Else simply call open camera through cameraManager
                    else {
                        // Open camera through camera id, the stateCallBack, and our created
                        // backgroundHandler
                        cameraManager.openCamera(cam1.getCameraId(), cameraStateCallback,
                                backgroundHandler);
                    }
                }
                // Catch camera access exception
                catch (CameraAccessException camAccessExcep) {
                    Log.e(TAG, "ERROR accessing camera in connectCameras()"); // Error msg.
                    camAccessExcep.printStackTrace(); // Print Stack
                }
            }
            else { // Camera Manager is null
                throw new NullPointerException("MangeCameras.java");
            }
        }
        // Catch if Camera Manager is null
        catch (NullPointerException nullPtr) {
            Log.e(TAG, "Camera Manager is null");
            nullPtr.printStackTrace();
        }
    }

    /**
     * Creates and opens a front or rear facing camera (based on boolean), and creates a
     * captureSession
     * @param isRear signifies which camera needs to be created for our capture session
     * @return void for now, could return an image view or a file
     */
    void createCaptureSession(boolean isRear) {
        // IF isRear is true, create a capture session based on the rear facing camera
        if (isRear) {
            // Create capture session with rear facing camera
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

    /**
     * Creates a background thread for main to call, specifies which
     */

    /**
     * Pause any active cameras
     */
    void pause() {
        // Only one camera should be open at a time, check which one may be open
        // Check rear
        if (cam1.getCameraDevice() != null) {
            // Close cam1
            cam1.closeCamera();
        }
        // Else Check front facing
        else if (cam2.getCameraDevice() != null) {
            // Close cam2
            cam2.closeCamera();
        }
    }

    /**
     *
     * @return
     */
    public Camera getRear() {
        return cam1;
    }

    public void setRear(Camera cam1) {
        this.cam1 = cam1;
    }

    public Camera getFront() {
        return cam2;
    }

    public void setFront(Camera cam2) {
        this.cam2 = cam2;
    }
}
