package com.example.odembed;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import com.example.odembed.utils.DatabaseUtils;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class FaceRegister extends ObjectDetector {
  private static final String TAG = "FaceRegister";
  private static final int IMAGE_MEAN = 117;
  private static final float IMAGE_STD = 1;
  private static final String INPUT_NAME = "input";
  private static final String EMBEDDING_NAME = "embeddings";
  private static final float MINIMUM_DETECTION_CONFIDENCE = 0.6f;

  private Activity activity;
  private String modelFile;
  private String labelFile;
  private int modelInputWidth;
  private int modelInputHeight;
  private String registerName;

  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private float[] floatValues;
  private float[] outputEmbeddings;
  private long embeddingLength;
  private String[] outputNames;

  private boolean logStats = false;

  private TensorFlowInferenceInterface inferenceInterface;
  private DatabaseUtils dbUtils;

  public FaceRegister(
    Activity activity,
    String modelFile,
    String labelFile,
    int modelInputWidth,
    int modelInputHeight,
    String registerName) throws IOException {
    super(activity,
      "ssd_mobilenet_face.pb",
      "face_labels_list.txt",
      300,
      300);
    this.activity = activity;
    this.modelFile = modelFile;
    this.labelFile = labelFile;
    this.modelInputWidth = modelInputWidth;
    this.modelInputHeight = modelInputHeight;
    this.registerName = registerName;

    dbUtils = new DatabaseUtils(activity);
    AssetManager assetManager = activity.getAssets();

    inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFile);

    final Graph g = inferenceInterface.graph();

    final Operation inputOp = g.operation(INPUT_NAME);
    if (inputOp == null) {
      throw new RuntimeException("Failed to find input Node '" + INPUT_NAME + "'");
    }

    Log.d(TAG, "input op #: " + inputOp.numOutputs());

    final Operation embeddingOp = g.operation(EMBEDDING_NAME);
    if (embeddingOp == null) {
      throw new RuntimeException("Failed to find input Node '" + EMBEDDING_NAME + "'");
    }

    embeddingLength = embeddingOp.output(0).shape().size(1);
    Log.d(TAG, "embeddingOp shape: " + embeddingLength);

    outputNames = new String[] {EMBEDDING_NAME};
    intValues = new int[modelInputHeight * modelInputWidth];
    floatValues = new float[modelInputHeight * modelInputWidth *3];
  }

  @Override
  public String getModelFile() {
    return modelFile;
  }

  @Override
  public String getLabelFile() {
    return labelFile;
  }

  @Override
  public int getModelInputWidth() {
    // 模型输入用的是 Object Detector 的尺寸，而非 facenet 的尺寸
    return super.getModelInputWidth();
  }

  @Override
  public int getModelInputHeight() {
    // 模型输入用的是 Object Detector 的尺寸，而非 facenet 的尺寸
    return super.getModelInputHeight();
  }

  public boolean saveFace(Bitmap face) {
    long start = SystemClock.uptimeMillis();
    // Preprocess the image data from 0-255 int to normalized float based
    // on the provided parameters.
    face.getPixels(
      intValues,
      0,
      face.getWidth(),
      0, 0,
      face.getWidth(),
      face.getHeight());

    for (int i = 0; i < intValues.length; ++i) {
      final int val = intValues[i];
      floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
      floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
      floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
    }

    long startFeed = SystemClock.uptimeMillis();
    inferenceInterface.feed(INPUT_NAME, floatValues, 1, modelInputWidth, modelInputHeight, 3);
    //inferenceInterface.feed(PHASE_TRAIN, phaseValues, 1);

    inferenceInterface.run(outputNames, logStats);

    outputEmbeddings = new float[(int) embeddingLength];
    inferenceInterface.fetch(outputNames[0], outputEmbeddings);

    long runTime = SystemClock.uptimeMillis() - startFeed;
    long preprocessImageTime = startFeed - start;

    StringBuilder sb = new StringBuilder();
    sb.append("recog preprocess: ").append(preprocessImageTime).append("ms\n")
      .append("recog run: ").append(runTime).append("ms\n");

    dbUtils.addEmbedding(new FaceEmbedding(0, registerName, outputEmbeddings));

    return true;
  }

  @Override
  public FrameInferencerResult inferenceFrame(final Bitmap bitmap) {
    FrameInferencerResult detectorResult = super.inferenceFrame(bitmap);

    List<Recognition> recognitions = detectorResult.getRecognitions();
    if (recognitions.size() == 0) return detectorResult;

    Recognition best = recognitions.get(0);

    for (Recognition recog : recognitions) {
      if (recog.getConfidence() > best.getConfidence())
        best = recog;
    }

    if (best.getLocation() != null && best.getConfidence() >= MINIMUM_DETECTION_CONFIDENCE) {
      RectF location = best.getLocation();
      assert(location.left >= 0 && location.top >= 0
        && location.width() >= 0 && location.height() >= 0
        && location.width() <= bitmap.getWidth()
        && location.height() <= bitmap.getHeight()
      );
      Bitmap face = Bitmap.createBitmap(bitmap,
        (int)location.left, (int)location.top, (int)location.width(), (int)location.height());
      Bitmap scaledFace = Bitmap.createScaledBitmap(face, modelInputWidth, modelInputHeight, false);
      // TODO: alignment
      boolean success = saveFace(scaledFace);
      if (!success) {
        Log.e(TAG, "Error saving face embedding");
      } else {
        Log.i(TAG, "Registered face " + this.registerName);
        // Quit this activity if face registered
        activity.finish();
      }
    }

    return detectorResult;
  }

  @Override
  public void close() {
    inferenceInterface.close();
    super.close();
  }

}

