package com.example.practicewizards;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.time.chrono.ThaiBuddhistEra;

public class FileSave {
   public void SaveImage(Context context, Bitmap ImageToSave) {
       Context ThePic = context;
       String file_path = Environment.getExternalStorageDirectory().getAbsolutePath();//+NameOfFolder;
       String CurrentDateAndTime = getCurrentDateAndTime();
   }
}
