package com.example.odembed.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.odembed.R;

import java.util.HashMap;

public class FaceRegisterActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_face);

    Intent intent = getIntent();
    String modelFile = intent.getStringExtra("modelFile");
    String labelFile = intent.getStringExtra("labelFile");
    int modelInputWidth = intent.getIntExtra("modelInputWidth", 0);
    int modelInputHeight = intent.getIntExtra("modelInputHeight", 0);
    String registerName = intent.getStringExtra("registerName");

    boolean useFrontCamera = false;
    HashMap<String, String> hashMap = (HashMap<String, String>)intent.getSerializableExtra("extraParams");
    if (hashMap != null && hashMap.get("useFrontCamera").equals("true")) {
      useFrontCamera = true;
    }
    if (null == savedInstanceState) {
      CameraFragment cameraFragment = CameraFragment.newInstance(
        CameraFragment.CameraMode.FACE_REGISTER,
        modelFile,
        labelFile,
        modelInputWidth,
        modelInputHeight,
        useFrontCamera);

      cameraFragment.setRegisterName(registerName);

      getFragmentManager()
        .beginTransaction()
        .replace(
          R.id.container2,
          cameraFragment
        )
        .commit();
    }
  }
}
