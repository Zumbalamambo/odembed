package com.example.odembed.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Activity;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.odembed.FrameInferencer;
import com.example.odembed.ImageClassifier;
import com.example.odembed.R;
import com.example.odembed.Recognition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class PhotoClassifierActivity extends Activity {
  private static final String TAG = "PhotoClassifierActivity";
  private ImageView imageView;
  private TextView textView;
  private CountDownTimer countDownTimer;
  private ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
  private String modelFile;
  private String labelFile;
  private int modelInputWidth;
  private int modelInputHeight;
  private FrameInferencer inferencer;
  private boolean usingNNAPI = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_photo_detector);

    modelFile = getIntent().getStringExtra("modelFile");
    labelFile = getIntent().getStringExtra("labelFile");
    modelInputWidth = getIntent().getIntExtra("modelInputWidth", 0);
    modelInputHeight = getIntent().getIntExtra("modelInputHeight", 0);

    imageView = (ImageView)findViewById(R.id.image_view);
    textView = (TextView)findViewById(R.id.text_view);

    try {
      String[] images = getAssets().list("sample_images");
      for (String image : images) {
        InputStream inputStream = getAssets().open("sample_images/" + image);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        bitmaps.add(bitmap);
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to open asset image");
    }

    countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
      private int index = 0;

      public void onTick(long millisUntilFinished) {
        if (bitmaps != null) {
          Bitmap b = bitmaps.get(index);
          if (b != null) {
            runInference(bitmaps.get(index));
            index++;
            if (index >= bitmaps.size()) {
              index = 0;
            }
          }
        }
      }

      public void onFinish() {
        start();
      }
    };

    try {
      inferencer = new ImageClassifier(
        this, modelFile, labelFile, modelInputWidth, modelInputHeight);
    } catch (IOException e) {
      Log.e(TAG, "Failed to initialize an object detector.");
    }

  }

  @Override
  protected void onPause() {
    super.onPause();
    try {
      countDownTimer.cancel();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    try {
      countDownTimer.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // create action bar button
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_config, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem nnapiItem = menu.findItem(R.id.nnapi);
    nnapiItem.setChecked(usingNNAPI);
    inferencer.useNNAPI(usingNNAPI);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.nnapi) {
      usingNNAPI = !item.isChecked();
      item.setChecked(usingNNAPI);
      inferencer.useNNAPI(usingNNAPI);
    }
    return super.onOptionsItemSelected(item);
  }

  public void runInference(Bitmap bitmap) {
    Bitmap resized = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true);

    long start = SystemClock.uptimeMillis();
    FrameInferencer.FrameInferencerResult result = inferencer.inferenceFrame(resized);
    long end = SystemClock.uptimeMillis();

    StringBuilder stringBuilder = new StringBuilder();
    for (Recognition recog: result.getRecognitions()) {
      stringBuilder
        .append(recog.getTitle()).append(": ").append(recog.getConfidence()).append("\n");
    }

    String textToShow = "Inference: " + (end - start) + "ms\n"
      + stringBuilder.toString() + result.getInfrenceMessage();
    textView.setText(textToShow);
    imageView.setImageBitmap(bitmap);
  }

}

