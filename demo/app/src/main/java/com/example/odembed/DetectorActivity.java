package com.example.odembed;

import android.app.Activity;
import android.os.Bundle;

public class DetectorActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        if (null == savedInstanceState) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container2, CameraFragment.newInstance(CameraFragment.CameraMode.DETECTOR))
                    .commit();
        }
    }
}
