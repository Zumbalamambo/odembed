package com.example.odembed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;
import java.util.Random;

import static java.lang.Math.max;

public class BoxView extends View {
  private int mRatioWidth = 0;
  private int mRatioHeight = 0;

  private Paint boxPaint = new Paint();
  private Paint textPaint = new Paint();
  private List<Recognition> recognitions;
  private Random rand = new Random();

  public BoxView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setAspectRatio(int width, int height) {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Size cannot be negative.");
    }
    mRatioWidth = width;
    mRatioHeight = height;
    requestLayout();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    if (0 == mRatioWidth || 0 == mRatioHeight) {
      setMeasuredDimension(width, height);
    } else {
      if (width < height * mRatioWidth / mRatioHeight) {
        setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
      } else {
        setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
      }
    }
  }

  public synchronized void drawRecognition(List<Recognition> recogs) {
    this.recognitions = recogs;
    postInvalidate();
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (recognitions != null) {
      boxPaint.setStrokeWidth(2.0f);
      boxPaint.setStyle(Paint.Style.STROKE);

      textPaint.setColor(Color.WHITE);
      textPaint.setTextSize(40);
      for (Recognition recog : this.recognitions) {
        boxPaint.setARGB(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        canvas.drawRect(recog.getLocation(), boxPaint);
        String title = recog.getTitle();
        float confidence = recog.getConfidence();
        float x = recog.getLocation().left;
        float y = max(recog.getLocation().top - 10, 0);
        String combined = title + " " + confidence;
        canvas.drawText(combined, x, y, textPaint);
      }
    }
  }
}
