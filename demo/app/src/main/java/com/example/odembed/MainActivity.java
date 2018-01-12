package com.example.odembed;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        "ssd_mobilenet_v1_android_export.pb",
        "ssd_mobilenet_v1_coco_2017_11_17.pb",
        "ssd_mobilenet_face.pb"
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
          Intent intent = new Intent(MainActivity.this, ClassifierActivity.class);
          intent.putExtra("modelFile","mobilenet_quant_v1_224.tflite");
          intent.putExtra("labelFile", "imagenet_labels_list.txt");
          intent.putExtra("modelInputWidth", 224);
          intent.putExtra("modelInputHeight", 224);
          startActivity(intent);
        } else if (selectedItem.equals("ssd_mobilenet_v1_android_export.pb")) {
          Intent intent = new Intent(MainActivity.this, DetectorActivity.class);
          intent.putExtra("modelFile","ssd_mobilenet_v1_android_export.pb");
          intent.putExtra("labelFile","coco_labels_list.txt");
          intent.putExtra("modelInputWidth", 300);
          intent.putExtra("modelInputHeight", 300);
          startActivity(intent);
        } else if (selectedItem.equals("ssd_mobilenet_v1_coco_2017_11_17.pb")) {
          Intent intent = new Intent(MainActivity.this, DetectorActivity.class);
          intent.putExtra("modelFile","ssd_mobilenet_v1_coco_2017_11_17.pb");
          intent.putExtra("labelFile","coco_labels_list.txt");
          intent.putExtra("modelInputWidth", 300);
          intent.putExtra("modelInputHeight", 300);
          startActivity(intent);
        } else if (selectedItem.equals("ssd_mobilenet_face.pb")) {
          Intent intent = new Intent(MainActivity.this, DetectorActivity.class);
          intent.putExtra("modelFile","ssd_mobilenet_face.pb");
          intent.putExtra("labelFile","face_labels_list.txt");
          intent.putExtra("modelInputWidth", 300);
          intent.putExtra("modelInputHeight", 300);
          HashMap<String, String> hashMap = new HashMap<String, String>();
          hashMap.put("useFrontCamera", "true");
          intent.putExtra("extraParams", hashMap);
          startActivity(intent);
        } else {
          Log.e(TAG, "Unrecognized item: " + selectedItem + " at position: " + position);
        }
      }
    });
  }
}
