package com.example.practicewizards;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Cache Manager
 */
public class CacheManager {
    private static final String TAG = "CacheManger";

    /**
     * Delete the cached directory to start fresh
     * Cited source:
     * https://stackoverflow.com/questions/52623190/how-to-clear-cache-on-a-specific-activity
     */
    public static void deleteCache(Context context) {
        // Try to get the cache directory and delete it
        try {
            File dir = context.getCacheDir(); // Get cached directory
            Log.d(TAG, "Delete cache directory");
            deleteDir(dir); // Delete the directory
        }
        catch (Exception e) {
            Log.e(TAG, "Error deleting cache directory");
            e.printStackTrace();
        }
    }

    /**
     * Deletes a given directory and all its children
     */
    public static boolean deleteDir(File file) {
        // Make sure file is not null
        if (file != null) {
            // See if dir is a directory
            if (file.isDirectory()) {
                Log.d(TAG, "Delete Directory");
                // We need to delete all its children
                String[] children = file.list(); // Declare a list of child filenames from the dir.list
                for (String child : children) {
                    Log.d(TAG, "Delete child");
                    // Recursively delete all the children in the parent dir
                    boolean success = deleteDir(new File(file, child)); // Recursive call
                    // Our recursive stopping point
                    if (!success) {
                        return false;
                    }
                }
                return file.delete(); // Recursively delete all directories in stack
            }
            // Else if dir is just a file, not an actual dir
            else if (file.isFile()) {
                Log.d(TAG, "Delete file");
                return file.delete(); // Return true if delete succeeded
            }
        }
        // Else null file
        return false;
    }
}

