package com.example.odembed;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.app.Activity;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.max;

public class PhotoDetectorActivity extends Activity {
  private static final String TAG = "PhotoDetectorActivity";
  private static final float MINIMUM_DETECTION_CONFIDENCE = 0.6f;
  private ImageView imageView;
  private TextView textView;
  private CountDownTimer countDownTimer;
  private ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
  private String modelFile;
  private String labelFile;
  private int modelInputWidth;
  private int modelInputHeight;
  private FrameInferencer inferencer;
  private Matrix cropToViewMatrix = new Matrix();
  private RectF detectRect;
  private Paint boxPaint = new Paint();
  private Paint textPaint = new Paint();
  private Random rand = new Random();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_photo_detector);

    modelFile = getIntent().getStringExtra("modelFile");
    labelFile = getIntent().getStringExtra("labelFile");
    modelInputWidth = getIntent().getIntExtra("modelInputWidth", 0);
    modelInputHeight = getIntent().getIntExtra("modelInputHeight", 0);

    imageView = (ImageView)findViewById(R.id.image_view);
    textView = (TextView) findViewById(R.id.text_view);

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
      inferencer = new ObjectDetector(
        this, modelFile, labelFile, modelInputWidth, modelInputHeight);
    } catch (IOException e) {
      Log.e(TAG, "Failed to initialize an object detector.");
    }

    detectRect = new RectF(0, 0, modelInputWidth, modelInputHeight);
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

  public void runInference(Bitmap bitmap) {
    Bitmap resized = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true);

    RectF originalRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
    cropToViewMatrix.setRectToRect(detectRect, originalRect, Matrix.ScaleToFit.FILL);

    long start = SystemClock.uptimeMillis();
    FrameInferencer.FrameInferencerResult result = inferencer.inferenceFrame(resized);
    long end = SystemClock.uptimeMillis();

    List<Recognition> recognitions = result.getRecognitions();
    final List<Recognition> mappedRecognitions = new LinkedList<Recognition>();

    for (final Recognition recog : recognitions) {
      final RectF location = recog.getLocation();
      if (location != null && recog.getConfidence() >= MINIMUM_DETECTION_CONFIDENCE) {
        cropToViewMatrix.mapRect(location);
        recog.setLocation(location);
        mappedRecognitions.add(recog);
      }
    }

    Bitmap bitmapWithBoxes = bitmap.copy(bitmap.getConfig(), true);
    Canvas canvas = new Canvas(bitmapWithBoxes);

    boxPaint.setStrokeWidth(1.0f);
    boxPaint.setStyle(Paint.Style.STROKE);
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(10);

    for (Recognition recog : mappedRecognitions) {
      boxPaint.setARGB(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
      canvas.drawRect(recog.getLocation(), boxPaint);
      String title = recog.getTitle();
      float confidence = recog.getConfidence();
      float x = recog.getLocation().left;
      float y = max(recog.getLocation().top, 40);
      String combined = title + " " + confidence;
      canvas.drawText(combined, x, y, textPaint);
    }

    String textToShow = "Inference: " + (end - start) + "ms\n" + result.getInfrenceMessage();
    textView.setText(textToShow);
    imageView.setImageBitmap(bitmapWithBoxes);
  }

}
