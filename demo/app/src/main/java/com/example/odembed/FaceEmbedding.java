package com.example.odembed;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class FaceEmbedding {
  public int id;
  public String name;
  public float[] embedding;

  public FaceEmbedding(int id, String name, float[] embedding) {
    this.id = id;
    this.name = name;
    this.embedding = embedding;
  }

  public FaceEmbedding(int id, String name, byte[] embedding) {
    this.id = id;
    this.name = name;
    this.embedding = byteArray2FloatArray(embedding);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public float[] getEmbedding() {
    return embedding;
  }

  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }

  public String getEmbeddingString() {
    StringBuilder sb = new StringBuilder();
    for (float f : embedding) {
      sb.append(f).append(" ");
    }
    return sb.toString();
  }

  public static byte[] floatArray2ByteArray(float[] values){
    ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

    for (float value : values){
      buffer.putFloat(value);
    }

    return buffer.array();
  }

  public static float[] byteArray2FloatArray(byte[] values){
    ByteBuffer buffer = ByteBuffer.wrap(values);
    FloatBuffer fb = buffer.asFloatBuffer();

    float[] floatArray = new float[fb.limit()];
    fb.get(floatArray);

    return floatArray;
  }

}
