package com.example.odembed.utils;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.odembed.FaceEmbedding;

public class DatabaseUtils extends SQLiteOpenHelper {
  private static final String TAG = "DatabaseUtils";
  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "sqldb";
  private static final String TABLE_EMBEDDING = "embedding";

  // FaceEmbedding table columns
  private static final String KEY_ID = "id";
  private static final String KEY_NAME = "name";
  private static final String KEY_EMBEDDING = "embedding";

  public DatabaseUtils(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String CREATE_TABLE_STRING = "CREATE TABLE " + TABLE_EMBEDDING + "("
      + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
      + KEY_EMBEDDING + " TEXT" + ")";
    db.execSQL(CREATE_TABLE_STRING);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Drop older table if existed
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMBEDDING);

    // Create tables again
    onCreate(db);
  }

  public void addEmbedding(FaceEmbedding embedding) {
    SQLiteDatabase db = this.getWritableDatabase();

    if (findEmbedding(embedding.getName()) == null) {
      ContentValues values = new ContentValues();
      values.put(KEY_NAME, embedding.getName());
      values.put(KEY_EMBEDDING, FaceEmbedding.floatArray2ByteArray(embedding.getEmbedding()));

      db.insert(TABLE_EMBEDDING, null, values);
    } else {
      Log.w(TAG, "Trying to insert duplicate name " + embedding.getName());
    }
    db.close();
  }

  public FaceEmbedding getEmbedding(int id) {
    SQLiteDatabase db = this.getReadableDatabase();

    Cursor cursor = db.query(TABLE_EMBEDDING, new String[] { KEY_ID,
        KEY_NAME, KEY_EMBEDDING}, KEY_ID + "=?",
      new String[] { String.valueOf(id) }, null, null, null, null);
    if (cursor != null && cursor.moveToFirst()) {
      FaceEmbedding embedding = new FaceEmbedding(Integer.parseInt(cursor.getString(0)),
        cursor.getString(1), cursor.getBlob(2));

      return embedding;
    } else {
      return null;
    }
  }

  public FaceEmbedding findEmbedding(String name) {
    SQLiteDatabase db = this.getReadableDatabase();

    Cursor cursor = db.query(TABLE_EMBEDDING, new String[] { KEY_ID,
        KEY_NAME, KEY_EMBEDDING}, KEY_NAME + "=?",
      new String[] { name }, null, null, null, null);

    if (cursor != null && cursor.moveToFirst()) {
      FaceEmbedding embedding = new FaceEmbedding(Integer.parseInt(cursor.getString(0)),
        cursor.getString(1), cursor.getBlob(2));

      return embedding;
    } else {
      return null;
    }
  }

  public ArrayList<FaceEmbedding> getAllEmbeddings() {
    ArrayList<FaceEmbedding> embeddings = new ArrayList<FaceEmbedding>();
    String selectQuery = "SELECT  * FROM " + TABLE_EMBEDDING;

    SQLiteDatabase db = this.getWritableDatabase();
    Cursor cursor = db.rawQuery(selectQuery, null);

    if (cursor.moveToFirst()) {
      do {
        FaceEmbedding embedding = new FaceEmbedding(
          Integer.parseInt(cursor.getString(0)),
          cursor.getString(1),
          FaceEmbedding.byteArray2FloatArray(cursor.getBlob(2))
        );
        embeddings.add(embedding);
      } while (cursor.moveToNext());
    }

    return embeddings;
  }

  public int updateEmbedding(FaceEmbedding embedding) {
    SQLiteDatabase db = this.getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(KEY_NAME, embedding.getName());
    values.put(KEY_EMBEDDING, FaceEmbedding.floatArray2ByteArray(embedding.getEmbedding()));

    return db.update(TABLE_EMBEDDING, values, KEY_ID + " = ?",
      new String[] { String.valueOf(embedding.getId()) });
  }

  public void deleteEmbedding(FaceEmbedding embedding) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_EMBEDDING, KEY_ID + " = ?",
      new String[] { String.valueOf(embedding.getId()) });
    db.close();
  }

  public int getEmbeddingsCount() {
    String countQuery = "SELECT  * FROM " + TABLE_EMBEDDING;
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(countQuery, null);
    cursor.close();

    return cursor.getCount();
  }

}
