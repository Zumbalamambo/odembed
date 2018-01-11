package com.example.odembed;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private ListView listView;
  private ArrayAdapter<String> listAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ListView lv = (ListView) findViewById(R.id.listView);

    String[] models = new String[] {
        "mobilenet_quant_v1_224.tflite",
        "ssd_mobilenet_v1_android_export.pb"
    };

    List<String> modelList = new ArrayList<String>(Arrays.asList(models));

    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
        (this, android.R.layout.simple_list_item_1, modelList);

    lv.setAdapter(arrayAdapter);

    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String selectedItem = (String) parent.getItemAtPosition(position);
        if (selectedItem.equals("mobilenet_quant_v1_224.tflite")) {
          startActivity(new Intent(MainActivity.this, ClassifierActivity.class));
        } else if (selectedItem.equals("ssd_mobilenet_v1_android_export.pb")) {
          startActivity(new Intent(MainActivity.this, DetectorActivity.class));
        } else {
          Log.e(TAG, "Unrecognized item: " + selectedItem + " at position: " + position);
        }
      }
    });
  }
}
