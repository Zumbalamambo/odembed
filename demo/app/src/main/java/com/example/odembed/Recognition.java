package com.example.odembed;

import android.graphics.RectF;

public class Recognition {
  private final String id;
  private String title;
  private Float confidence;
  private RectF location;

  public Recognition(
          String id,
          String title,
          Float confidence,
          RectF location) {
    this.id = id;
    this.title = title;
    this.confidence = confidence;
    this.location = location;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Float getConfidence() {
    return confidence;
  }

  public void setConfidence(float confidence) {
    this.confidence = confidence;
  }

  public RectF getLocation() {
    return new RectF(location);
  }

  public void setLocation(RectF location) {
    this.location = location;
  }

  @Override
  public String toString() {
    String resultString = "";
    if (id != null) {
      resultString += "[" + id + "] ";
    }

    if (title != null) {
      resultString += title + " ";
    }

    if (confidence != null) {
      resultString += String.format("(%.1f%%) ", confidence * 100.0f);
    }

    if (location != null) {
      resultString += location + " ";
    }

    return resultString.trim();
  }
}