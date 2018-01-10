package com.example.odembed;

import android.graphics.Bitmap;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;

import android.util.Log;


public class ImageUtils {
  private static final String TAG = "ImageUtils";

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   */
  public static void saveBitmap(final Bitmap bitmap) {
    saveBitmap(bitmap, "preview.png");
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap   The bitmap to save.
   * @param filename The location to save the bitmap to.
   */
  public static void saveBitmap(final Bitmap bitmap, final String filename) {
    final String root =
        Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "odembed";
    Log.i(TAG, "Saving %dx%d bitmap to " + bitmap.getWidth() + ", " + bitmap.getHeight() + root);
    final File myDir = new File(root);

    if (!myDir.mkdirs()) {
      Log.i(TAG, "Make dir failed");
    }

    final String fname = filename;
    final File file = new File(myDir, fname);
    if (file.exists()) {
      file.delete();
    }
    try {
      final FileOutputStream out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
      out.flush();
      out.close();
    } catch (final Exception e) {
      Log.e(TAG, e.getMessage());
    }
  }

}

