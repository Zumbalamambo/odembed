package com.example.odembed.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.odembed.R;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private ListView listView;
  private ArrayAdapter<String> listAdapter;

  public class Model {
    private int id;
    private int imageId;
    private Class<?> cls;
    private String name;
    private String description;
    private String modelFile = "";
    private String labelFile;
    private int modelInputWidth;
    private int modelInputHeight;

    public Model(
      int id,
      int imageId,
      Class<?> cls,
      String name,
      String description,
      String modelFile,
      String labelFile,
      int modelInputWidth,
      int modelInputHeight) {
      this.id = id;
      this.cls = cls;
      this.name = name;
      this.description = description;
      this.modelFile = modelFile;
      this.labelFile = labelFile;
      this.modelInputWidth = modelInputWidth;
      this.modelInputHeight = modelInputHeight;
      this.imageId = imageId;
    }

    public Model(
      int id,
      int imageId,
      Class<?> cls,
      String name,
      String description,
      String modelFile,
      String labelFile) {
      this(id, imageId, cls, name, description, modelFile, labelFile, 0, 0);
    }

    public Model(
      int id,
      int imageId,
      Class<?> cls,
      String name,
      String description,
      String modelFile) {
      this(id, imageId, cls, name, description, modelFile, "", 0, 0);
    }

    public Model(
      int id,
      int imageId,
      Class<?> cls,
      String name,
      String description,
      String modelFile,
      int modelInputWidth,
      int modelInputHeight) {
      this(id, imageId, cls, name, description, modelFile, "", modelInputWidth, modelInputHeight);
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public Class<?> getCls() {
      return cls;
    }

    public void setCls(Class<?> cls) {
      this.cls = cls;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getModelFile() {
      return modelFile;
    }

    public void setModelFile(String modelFile) {
      this.modelFile = modelFile;
    }

    public String getLabelFile() {
      return labelFile;
    }

    public void setLabelFile(String labelFile) {
      this.labelFile = labelFile;
    }

    public int getModelInputWidth() {
      return modelInputWidth;
    }

    public void setModelInputWidth(int modelInputWidth) {
      this.modelInputWidth = modelInputWidth;
    }

    public int getModelInputHeight() {
      return modelInputHeight;
    }

    public void setModelInputHeight(int modelInputHeight) {
      this.modelInputHeight = modelInputHeight;
    }

    public int getImageId() {
      return imageId;
    }

    public void setImageId(int imageId) {
      this.imageId = imageId;
    }

  }

  public class ModelAdapter extends ArrayAdapter<Model> {
    public ModelAdapter(Context context, ArrayList<Model> users) {
      super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Model model = getItem(position);
      if (convertView == null) {
        convertView = LayoutInflater.from(
          getContext()).inflate(R.layout.model_item_layout, parent, false);
      }

      TextView nameTextView = (TextView) convertView.findViewById(R.id.list_item_name_view);
      TextView descTextView = (TextView) convertView.findViewById(R.id.list_item_desc_view);
      ImageView imageView = (ImageView) convertView.findViewById(R.id.list_item_image_view);
      nameTextView.setText(model.getName());
      descTextView.setText(model.getDescription());
      imageView.setImageResource(model.getImageId());
      return convertView;
    }
  }

  ArrayList<Model> modelList = null;

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    modelList = new ArrayList<Model>();
    modelList.add(
      new Model(
        1,
        R.drawable.ic_model,
        ClassifierActivity.class,
        "imagenet classifier",
        "mobilenet imagenet classifier, TF Lite, quantized, 224x224",
        "mobilenet_quant_v1_224.tflite",
        "imagenet_labels_list.txt",
        224,
        224
        ));
    modelList.add(
      new Model(
        2,
        R.drawable.ic_model,
        DetectorActivity.class,
        "coco detector",
        "mobilenet ssd coco detector, TF Mobile, 300x300, transfer learning on coco, old",
        "ssd_mobilenet_v1_android_export.pb",
        "coco_labels_list.txt",
        300,
        300
        ));
    modelList.add(
      new Model(
        3,
        R.drawable.ic_model,
        DetectorActivity.class,
        "coco detector",
        "mobilenet ssd coco detector, TF Mobile, 300x300, transfer learning on coco",
        "ssd_mobilenet_v1_coco_2017_11_17.pb",
        "coco_labels_list.txt",
        300,
        300
        ));
    modelList.add(
      new Model(
        4,
        R.drawable.ic_model,
        DetectorActivity.class,
        "face detector",
        "mobilenet ssd face detector, TF Mobile, 300x300, transfer learning on wideface",
        "ssd_mobilenet_face.pb",
        "face_labels_list.txt",
        300,
        300
        ));
    modelList.add(
      new Model(
        5,
        R.drawable.ic_model,
        PhotoDetectorActivity.class,
        "coco detector(no camera)",
        "mobilenet ssd coco detector, TF Mobile, 300x300, transfer learning on coco, using sample images",
        "ssd_mobilenet_v1_coco_2017_11_17.pb",
        "coco_labels_list.txt",
        300,
        300
      ));
    modelList.add(
      new Model(
        6,
        R.drawable.ic_model,
        PhotoClassifierActivity.class,
        "imagenet classifier(no camera)",
        "mobilenet imagenet classifier, TF Lite, quantized, 224x224",
        "mobilenet_quant_v1_224.tflite",
        "imagenet_labels_list.txt",
        224,
        224
      ));
    modelList.add(
      new Model(
        7,
        R.drawable.ic_model,
        FaceActivity.class,
        "face recognition",
        "facenet on inception resnet v1, TF Mobile",
        //"facenet_inception_resnet_v1.pb",
        "facenet.pb",
        "notused.txt",
        160,
        160
      ));
    modelList.add(
      new Model(
        8,
        R.drawable.ic_model,
        FaceMgmtActivity.class,
        "face management",
        "manage faces using the same facenet as above",
        "facenet.pb",
        "notused.txt",
        160,
        160
      ));


    ListView lv = (ListView) findViewById(R.id.listView);

    ModelAdapter modelAdapter = new ModelAdapter(this, modelList);
    lv.setAdapter(modelAdapter);

    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Model model = modelList.get(position);

        int modelId = model.getId();
        Intent intent = new Intent(MainActivity.this, model.getCls());
        intent.putExtra("modelFile",model.getModelFile());
        intent.putExtra("labelFile", model.getLabelFile());
        intent.putExtra("modelInputWidth", model.getModelInputWidth());
        intent.putExtra("modelInputHeight", model.getModelInputHeight());

        if (modelId == 4 || modelId == 7 || modelId == 8) {
          HashMap<String, String> hashMap = new HashMap<String, String>();
          hashMap.put("useFrontCamera", "true");
          intent.putExtra("extraParams", hashMap);
        }

        startActivity(intent);
      }
    });
  }

}
