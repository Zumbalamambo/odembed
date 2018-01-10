package com.example.odembed;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Vector;

// TF Mobile interface, in libtensorflow_inference.so
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class ObjectDetector implements FrameInferencer {
  private static final String TAG = "ObjectDetector";
  private static final int MAX_RESULTS = 100;
  private static final String MODEL_PATH = "ssd_mobilenet_v1_android_export.pb";
  private static final String LABEL_PATH = "coco_labels_list.txt";
  public static final int MODEL_INPUT_SIZE_WIDTH = 300;
  public static final int MODEL_INPUT_SIZE_HEIGHT = 300;

  private String inputName;

  // Pre-allocated buffers.
  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private byte[] byteValues;
  private float[] outputLocations;
  private float[] outputScores;
  private float[] outputClasses;
  private float[] outputNumDetections;
  private String[] outputNames;

  private boolean logStats = false;

  private TensorFlowInferenceInterface inferenceInterface;

  public ObjectDetector(Activity activity) throws IOException {
    AssetManager assetManager = activity.getAssets();
    InputStream labelsInput = null;
    labelsInput = assetManager.open(LABEL_PATH);
    BufferedReader br = null;
    br = new BufferedReader(new InputStreamReader(labelsInput));
    String line;
    while ((line = br.readLine()) != null) {
      labels.add(line);
    }
    br.close();

    inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_PATH);

    final Graph g = inferenceInterface.graph();

    inputName = "image_tensor";
    // inputName node shape: NHWC
    // C == 3, RGB
    final Operation inputOp = g.operation(inputName);
    if (inputOp == null) {
      throw new RuntimeException("Failed to find input Node '" + inputName + "'");
    }
    // outputScoresName node shape: [N, NumLocations]
    final Operation outputOp1 = g.operation("detection_scores");
    if (outputOp1 == null) {
      throw new RuntimeException("Failed to find output Node 'detection_scores'");
    }
    final Operation outputOp2 = g.operation("detection_boxes");
    if (outputOp2 == null) {
      throw new RuntimeException("Failed to find output Node 'detection_boxes'");
    }
    final Operation outputOp3 = g.operation("detection_classes");
    if (outputOp3 == null) {
      throw new RuntimeException("Failed to find output Node 'detection_classes'");
    }

    // Pre-allocate buffers.
    outputNames = new String[] {"detection_boxes", "detection_scores",
            "detection_classes", "num_detections"};
    intValues = new int[MODEL_INPUT_SIZE_HEIGHT * MODEL_INPUT_SIZE_WIDTH];
    byteValues = new byte[MODEL_INPUT_SIZE_HEIGHT * MODEL_INPUT_SIZE_WIDTH * 3];
    outputScores = new float[MAX_RESULTS];
    outputLocations = new float[MAX_RESULTS * 4];
    outputClasses = new float[MAX_RESULTS];
    outputNumDetections = new float[1];
  }

  @Override
  public FrameInferencerResult inferenceFrame(final Bitmap bitmap) {
    long start = SystemClock.uptimeMillis();
    // Preprocess the image data from 0-255 int to normalized float based
    // on the provided parameters.
    bitmap.getPixels(
            intValues,
            0,
            bitmap.getWidth(),
            0, 0,
            bitmap.getWidth(),
            bitmap.getHeight());

    for (int i = 0; i < intValues.length; ++i) {
      byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
      byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
      byteValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
    }
    long preprocessImageTime = SystemClock.uptimeMillis() - start;

    // Copy the input data into TensorFlow.
    long startFeed = SystemClock.uptimeMillis();
    inferenceInterface.feed(inputName, byteValues, 1, MODEL_INPUT_SIZE_WIDTH, MODEL_INPUT_SIZE_HEIGHT, 3);
    long feedTime = SystemClock.uptimeMillis() - startFeed;

    // Run the inference call.
    long startRun = SystemClock.uptimeMillis();
    inferenceInterface.run(outputNames, logStats);
    long runTime = SystemClock.uptimeMillis() - startRun;

    // Copy the output Tensor back into the output array.
    long startFetch = SystemClock.uptimeMillis();
    outputLocations = new float[MAX_RESULTS * 4];
    outputScores = new float[MAX_RESULTS];
    outputClasses = new float[MAX_RESULTS];
    outputNumDetections = new float[1];
    inferenceInterface.fetch(outputNames[0], outputLocations);
    inferenceInterface.fetch(outputNames[1], outputScores);
    inferenceInterface.fetch(outputNames[2], outputClasses);
    inferenceInterface.fetch(outputNames[3], outputNumDetections);
    long fetchTime = SystemClock.uptimeMillis() - startFetch;

    StringBuilder sb = new StringBuilder();
    sb.append("preprocess: ").append(preprocessImageTime).append("ms\n")
        .append("feed: ").append(feedTime).append("ms\n")
        .append("run: ").append(runTime).append("ms\n")
        .append("fetch: ").append(fetchTime).append("ms\n");

    // Find the best detections.
    final PriorityQueue<Recognition> pq =
      new PriorityQueue<Recognition>(
        1,
        new Comparator<Recognition>() {
          @Override
          public int compare(final Recognition lhs, final Recognition rhs) {
            // high confidence at the head of the queue.
            return Float.compare(rhs.getConfidence(), lhs.getConfidence());
          }
        });

    // Scale them back to the input size.
    for (int i = 0; i < outputScores.length; ++i) {
      final RectF detection =
        new RectF(
                outputLocations[4 * i + 1] * MODEL_INPUT_SIZE_WIDTH,
                outputLocations[4 * i] * MODEL_INPUT_SIZE_WIDTH,
                outputLocations[4 * i + 3] * MODEL_INPUT_SIZE_WIDTH,
                outputLocations[4 * i + 2] * MODEL_INPUT_SIZE_WIDTH);
      pq.add(
          new Recognition("" + i, labels.get((int) outputClasses[i]), outputScores[i], detection));
    }

    final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
    for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
      recognitions.add(pq.poll());
    }
    return new FrameInferencerResult(recognitions, sb.toString());
  }

  @Override
  public void close() {
    inferenceInterface.close();
  }

  public String getStatString() {
    return inferenceInterface.getStatString();
  }

}
