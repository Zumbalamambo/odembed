package com.example.odembed;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// TF Lite interface, in libtensorflowlite_jni.so
import org.tensorflow.lite.Interpreter;

public class ImageClassifier implements FrameInferencer {
  private static final String TAG = "odembed";

  private static final String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
  private static final String LABEL_PATH = "labels.txt";
  private static final int MAX_RESULTS = 3;
  private static final int DIM_BATCH_SIZE = 1;
  private static final int DIM_PIXEL_CHANNEL = 3;

  static final int DIM_IMG_SIZE_X = 224;
  static final int DIM_IMG_SIZE_Y = 224;

  // 3-channel char converted to a single int
  private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
  private Interpreter tflite;
  private List<String> labelList;

  private ByteBuffer imgInput = null;
  private byte[][] labelOutput = null;

  private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
    new PriorityQueue<>(
            MAX_RESULTS,
      new Comparator<Map.Entry<String, Float>>() {
        @Override
        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
          return (o1.getValue()).compareTo(o2.getValue());
        }
      });

  ImageClassifier(Activity activity) throws IOException {
    tflite = new Interpreter(loadModelFile(activity));
    labelList = loadLabelList(activity);
    imgInput = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE * DIM_IMG_SIZE_Y * DIM_IMG_SIZE_X * DIM_PIXEL_CHANNEL);
    imgInput.order(ByteOrder.nativeOrder());
    labelOutput = new byte[1][labelList.size()];
    Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
  }

  public FrameInferencerResult inferenceFrame(Bitmap bitmap) {
    if (tflite == null) {
      return new FrameInferencerResult(null, "classifier not initialized");
    }
    long start = SystemClock.uptimeMillis();
    convertBitmapToByteBuffer(bitmap);
    long preprocessEnd = SystemClock.uptimeMillis();
    tflite.run(imgInput, labelOutput);
    long runEnd = SystemClock.uptimeMillis();
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("preprocess: ").append(preprocessEnd - start).append("ms\n")
            .append("run: ").append(runEnd - preprocessEnd).append("ms\n");
    List<Recognition> recognitions = getTopKLabels();
    return new FrameInferencerResult(recognitions, stringBuilder.toString());
  }

  @Override
  public void close() {
    tflite.close();
    tflite = null;
  }

  /** Reads label list from Assets. */
  private List<String> loadLabelList(Activity activity) throws IOException {
    List<String> labelList = new ArrayList<String>();
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
    String line;
    while ((line = reader.readLine()) != null) {
      labelList.add(line);
    }
    reader.close();
    return labelList;
  }

  /** Memory-map the model file in Assets. */
  private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
    AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
  }

  private void convertBitmapToByteBuffer(Bitmap bitmap) {
    if (imgInput == null) {
      return;
    }
    imgInput.rewind();
    // ARGB
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    // Convert the image to floating point.
    int pixel = 0;
    for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
      for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
        final int val = intValues[pixel++];
        imgInput.put((byte) ((val >> 16) & 0xFF)); // R in least significant byte
        imgInput.put((byte) ((val >> 8) & 0xFF));
        imgInput.put((byte) (val & 0xFF));         // B in most significant byte
      }
    }
  }

  private String printTopKLabels() {
    for (int i = 0; i < labelList.size(); ++i) {
      // 注意，quantized 模型输出是一个 byte，转回 float 概率
      sortedLabels.add(
          new AbstractMap.SimpleEntry<>(labelList.get(i), (labelOutput[0][i] & 0xff) / 255.0f));
      if (sortedLabels.size() > MAX_RESULTS) {
        sortedLabels.poll();
      }
    }
    String textToShow = "";
    final int size = sortedLabels.size();
    for (int i = 0; i < size; ++i) {
      Map.Entry<String, Float> label = sortedLabels.poll();
      textToShow = "\n" + label.getKey() + ":" + Float.toString(label.getValue()) + textToShow;
    }
    return textToShow;
  }

  private List<Recognition> getTopKLabels() {
    List<Recognition> recognitions = new ArrayList<Recognition>();
    for (int i = 0; i < labelList.size(); ++i) {
      // 注意，quantized 模型输出是一个 byte，转回 float 概率
      sortedLabels.add(
              new AbstractMap.SimpleEntry<>(labelList.get(i), (labelOutput[0][i] & 0xff) / 255.0f));
      if (sortedLabels.size() > MAX_RESULTS) {
        sortedLabels.poll();
      }
    }
    final int size = sortedLabels.size();
    for (int i = 0; i < size; ++i) {
      Map.Entry<String, Float> label = sortedLabels.poll();
      recognitions.add(new Recognition(String.valueOf(i), label.getKey(), label.getValue(), null));
    }
    return recognitions;
  }
}
