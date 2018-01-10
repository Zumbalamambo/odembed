package com.example.odembed;

import android.app.Activity;
import android.os.Bundle;

public class ClassifierActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_classifier);
    if (null == savedInstanceState) {
      getFragmentManager()
          .beginTransaction()
          .replace(R.id.container, CameraFragment.newInstance(CameraFragment.CameraMode.CLASSIFIER))
          .commit();
    }
  }
}
