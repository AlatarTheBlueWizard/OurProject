package com.example.practicewizards;

import android.hardware.camera2.CameraDevice;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void multiply_isCorrect() {
        assertEquals(10, 5*2 );
    }

    @Test
    public boolean are_files_saved() {
        return false;
    }

    @Test
    public boolean test_if_image_returns() {
        return false;
    }

    @Test
    public boolean manager_cameras_open_correctly() {
        // Test camera's through manageCameras
        ManageCameras manageCameras = new ManageCameras();
        String cameraId = manageCameras.open(true);
        assert(cameraId != null);

        // Test front facing
        manageCameras = new ManageCameras();
        cameraId = manageCameras.open(false);
        assert(cameraId != null);

        return true;
    }

    // Going to test getters and setters because we I don't have much idea currently how to
    // unit test a camera

    @Test
    public boolean test_camera_getters_setters() {
        // Create our camera object, rear facing
        Camera camera = new Camera(true);
        camera.openCamera();
        assert(camera.getCameraDevice() != null);
        return true;
    }

    @Test
    public boolean test_manager_getters_setters() {
        // Create camera manager
        ManageCameras manageCameras = new ManageCameras();
        manageCameras.open(true);
        assert(manageCameras.getRear() != null);
        return true;
    }



}
