package com.example.odembed;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import com.example.odembed.utils.ImageUtils;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

// TF Mobile interface, in libtensorflow_inference.so

public class FaceRecognizer extends ObjectDetector {
  private static final String TAG = "FaceRecognizer";
  private static final int IMAGE_MEAN = 117;
  private static final float IMAGE_STD = 1;
  private static final String INPUT_NAME = "input";
  private static final String EMBEDDING_NAME = "embeddings";
  //private static final String PHASE_TRAIN = "phase_train";
  private static final float MINIMUM_DETECTION_CONFIDENCE = 0.6f;

  private boolean debug = true;

  private String modelFile;
  private String labelFile;
  private int modelInputWidth;
  private int modelInputHeight;

  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private float[] floatValues;
  //private boolean[] phaseValues;
  private float[] outputEmbeddings;
  private long embeddingLength;
  private String[] outputNames;

  private boolean logStats = false;

  private TensorFlowInferenceInterface inferenceInterface;

  public FaceRecognizer(
      Activity activity,
      String modelFile,
      String labelFile,
      int modelInputWidth,
      int modelInputHeight) throws IOException {
    super(activity,
      "ssd_mobilenet_face.pb",
      "face_labels_list.txt",
      300,
      300);
    this.modelFile = modelFile;
    this.labelFile = labelFile;
    this.modelInputWidth = modelInputWidth;
    this.modelInputHeight = modelInputHeight;
    AssetManager assetManager = activity.getAssets();
//    InputStream labelsInput = null;
//    labelsInput = assetManager.open(labelFile);
//    BufferedReader br = null;
//    br = new BufferedReader(new InputStreamReader(labelsInput));
//    String line;
//    while ((line = br.readLine()) != null) {
//      labels.add(line);
//    }
//    br.close();

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

    // phase_train is boolean which TF Mobile doesn't support,use the freezed model from:
    // https://github.com/apollo-time/facenet/blob/master/model/resnet/facenet.pb
    // where we don't need to feed phase_train
//    Operation phaseOp = g.operation(PHASE_TRAIN);
//    if (phaseOp == null) {
//      throw new RuntimeException("Failed to find input Node '" + PHASE_TRAIN + "'");
//    }
//    Log.d(TAG, "phase train op shape: " + phaseOp.output(0).shape());

    outputNames = new String[] {EMBEDDING_NAME};
    intValues = new int[modelInputHeight * modelInputWidth];
    floatValues = new float[modelInputHeight * modelInputWidth *3];
    //phaseValues = new boolean[] {false};
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

  public String recognizeFace(Bitmap face) {
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

    String embeddingString = "";
    for (float f : outputEmbeddings) {
      embeddingString += f + " ";
    }
    Log.d(TAG, "embeddings: " + embeddingString);

    return "hello Wayne";
  }

  @Override
  public FrameInferencerResult inferenceFrame(final Bitmap bitmap) {
    FrameInferencerResult detectorResult = super.inferenceFrame(bitmap);

    List<Recognition> recognitions = detectorResult.getRecognitions();
    for (final Recognition recog : recognitions) {
      final RectF location = recog.getLocation();
      if (location != null && recog.getConfidence() >= MINIMUM_DETECTION_CONFIDENCE) {
        Bitmap face = Bitmap.createBitmap(bitmap,
          (int)location.left, (int)location.top, (int)location.width(), (int)location.height());
        if (debug)
          ImageUtils.saveBitmap(face);

        // TODO: alignment
        String people = recognizeFace(face);
        recog.setTitle(people);
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
