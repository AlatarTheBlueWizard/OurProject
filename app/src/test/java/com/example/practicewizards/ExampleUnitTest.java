package com.example.practicewizards;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
/**    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void multiply_isCorrect() {
        assertEquals(10, 5*2 );
    }

    @Test
    public void are_files_saved() {

    }

    @Test
    public void test_if_image_returns() {

    }

    @Test
    public void manager_cameras_open_correctly() {
        // Test camera's through manageCameras
        ManageCameras manageCameras = new ManageCameras();
        String cameraId = manageCameras.open(true);
        assert(cameraId != null);

        // Test front facing
        manageCameras = new ManageCameras();
        cameraId = manageCameras.open(false);
        assert(cameraId != null);
    }

    // Going to test getters and setters because we I don't have much idea currently how to
    // unit test a camera

    @Test
    public void test_camera_getters_setters() {
        // Create our camera object, rear facing
        Camera camera = new Camera(true);
        camera.openCamera();
        assert(camera.getCameraDevice() != null);
    }

    @Test
    public void test_manager_getters_setters() {
        // Create camera manager
        ManageCameras manageCameras = new ManageCameras();
        manageCameras.open(true);
        assert(manageCameras.getRear() != null);
    }
    */
}
