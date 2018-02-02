package com.example.odembed.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.odembed.R;

import java.util.HashMap;

public class ClassifierActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_classifier);
    Intent intent = getIntent();
    String modelFile = intent.getStringExtra("modelFile");
    String labelFile = intent.getStringExtra("labelFile");
    int modelInputWidth = intent.getIntExtra("modelInputWidth", 0);
    int modelInputHeight = intent.getIntExtra("modelInputHeight", 0);

    boolean useFrontCamera = false;
    HashMap<String, String> hashMap = (HashMap<String, String>)intent.getSerializableExtra("extraParams");
    if (hashMap != null && hashMap.get("useFrontCamera").equals("true")) {
      useFrontCamera = true;
    }

    if (null == savedInstanceState) {
      getFragmentManager()
        .beginTransaction()
        .replace(
          R.id.container,
          CameraFragment.newInstance(
            CameraFragment.CameraMode.CLASSIFIER,
            modelFile,
            labelFile,
            modelInputWidth,
            modelInputHeight,
            useFrontCamera)
        )
        .commit();
    }
  }
}
