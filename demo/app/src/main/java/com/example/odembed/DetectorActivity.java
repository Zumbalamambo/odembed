package com.example.odembed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DetectorActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detect);
    Intent intent = getIntent();
    String modelFile = intent.getStringExtra("modelFile");
    String labelFile = intent.getStringExtra("labelFile");
    int modelInputWidth = intent.getIntExtra("modelInputWidth", 0);
    int modelInputHeight = intent.getIntExtra("modelInputHeight", 0);
    if (null == savedInstanceState) {
      getFragmentManager()
        .beginTransaction()
        .replace(
          R.id.container2,
          CameraFragment.newInstance(
            CameraFragment.CameraMode.DETECTOR,
            modelFile,
            labelFile,
            modelInputWidth,
            modelInputHeight)
        )
        .commit();
    }
  }
}
