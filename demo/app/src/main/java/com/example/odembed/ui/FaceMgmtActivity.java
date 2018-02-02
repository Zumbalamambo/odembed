package com.example.odembed.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.odembed.FaceEmbedding;
import com.example.odembed.R;
import com.example.odembed.utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FaceMgmtActivity extends AppCompatActivity {
  private static final String TAG = "FaceMgmtActivity";
  private Button registerButton;
  private EditText inputText;
  private ListView listView;
  private FaceEmbeddingAdapter adapter;
  private ArrayList<FaceEmbedding> embeddingList;
  private DatabaseUtils dbUtils;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_face_mgmt);

    Intent intent = getIntent();
    String modelFile = intent.getStringExtra("modelFile");
    String labelFile = intent.getStringExtra("labelFile");
    int modelInputWidth = intent.getIntExtra("modelInputWidth", 0);
    int modelInputHeight = intent.getIntExtra("modelInputHeight", 0);
    HashMap<String, String> hashMap = (HashMap<String, String>)intent.getSerializableExtra("extraParams");

    Intent newIntent = new Intent(this, FaceRegisterActivity.class);
    newIntent.putExtra("modelFile", modelFile);
    newIntent.putExtra("labelFile", labelFile);
    newIntent.putExtra("modelInputWidth", modelInputWidth);
    newIntent.putExtra("modelInputHeight", modelInputHeight);
    newIntent.putExtra("extraParams", hashMap);

    dbUtils = new DatabaseUtils(getApplicationContext());

    listView = (ListView) findViewById(R.id.list_view);
    listView.setLongClickable(true);

    inputText = (EditText)findViewById(R.id.name_input);
    registerButton = (Button)findViewById(R.id.register_button);
    registerButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        String name = inputText.getText().toString();
        inputText.setText("");
        if (!name.equals("")) {
          newIntent.putExtra("registerName", name);
          startActivity(newIntent);
        }
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    embeddingList = dbUtils.getAllEmbeddings();
    adapter = new FaceEmbeddingAdapter(this, embeddingList);
    listView.setAdapter(adapter);
    listView.setLongClickable(true);
    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        removeItemFromList(position);
        return true;
      }
    });
  }

  private void removeItemFromList(int position) {
    AlertDialog.Builder alert = new AlertDialog.Builder(FaceMgmtActivity.this);
    FaceEmbedding toBeDeleted = embeddingList.get(position);

    alert.setTitle("Delete");
    alert.setMessage("Do you want delete this item?");
    alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        embeddingList.remove(position);
        dbUtils.deleteEmbedding(toBeDeleted);
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();
      }
    });
    alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });

    alert.show();
  }


  public class FaceEmbeddingAdapter extends ArrayAdapter<FaceEmbedding> {
    public FaceEmbeddingAdapter(Context context, ArrayList<FaceEmbedding> embeddings) {
      super(context, 0, embeddings);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      FaceEmbedding model = getItem(position);
      if (convertView == null) {
        convertView = LayoutInflater.from(
          getContext()).inflate(R.layout.face_item_layout, parent, false);
      }

      TextView nameTextView = (TextView) convertView.findViewById(R.id.face_item_name_view);
      TextView embeddingView = (TextView) convertView.findViewById(R.id.face_item_embedding_view);
      nameTextView.setText(model.getName());
      embeddingView.setText(model.getEmbeddingString());
      return convertView;
    }
  }


}
