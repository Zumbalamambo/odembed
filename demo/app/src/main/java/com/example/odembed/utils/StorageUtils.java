package com.example.odembed.utils;

import java.io.FileOutputStream;

import android.content.Context;
import android.util.Log;


public class StorageUtils {
  private static final String TAG = "StorageUtils";

  public static void writeFile(Context context, String filename, byte[] byteArrayData) {
    FileOutputStream outputStream;

    try {
      outputStream = context.openFileOutput(filename, Context.MODE_APPEND);
      outputStream.write(byteArrayData);
      outputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

