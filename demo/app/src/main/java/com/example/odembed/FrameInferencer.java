package com.example.odembed;

import android.graphics.Bitmap;

import java.util.List;

public interface FrameInferencer {
  public class FrameInferencerResult {

    public FrameInferencerResult(List<Recognition> recogs, String message) {
      this.recognitions = recogs;
      this.infrenceMessage = message;
    }

    private List<Recognition> recognitions;
    private String infrenceMessage;

    public List<Recognition> getRecognitions() {
      return recognitions;
    }

    public void setRecognitions(List<Recognition> recognitions) {
      this.recognitions = recognitions;
    }

    public String getInfrenceMessage() {
      return infrenceMessage;
    }

    public void setInfrenceMessage(String infrenceMessage) {
      this.infrenceMessage = infrenceMessage;
    }
  }

  FrameInferencerResult inferenceFrame(Bitmap bitmap);
  String getModelFile();
  String getLabelFile();
  int getModelInputWidth();
  int getModelInputHeight();
  void useNNAPI(boolean nnapi);
  void close();
}
