package com.example.odembed.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.odembed.FaceRecognizer;
import com.example.odembed.FaceRegister;
import com.example.odembed.FrameInferencer;
import com.example.odembed.ImageClassifier;
import com.example.odembed.ObjectDetector;
import com.example.odembed.R;
import com.example.odembed.Recognition;
import com.example.odembed.utils.ImageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraFragment extends Fragment
    implements FragmentCompat.OnRequestPermissionsResultCallback {

  private static final String TAG = "odembed";
  private static final String FRAGMENT_DIALOG = "dialog";
  private static final String HANDLE_THREAD_NAME = "CameraBackground";
  private static final int PERMISSIONS_REQUEST_CODE = 1;
  private static final int MAX_PREVIEW_WIDTH = 1920;
  private static final int MAX_PREVIEW_HEIGHT = 1080;
  private static final float MINIMUM_DETECTION_CONFIDENCE = 0.6f;

  private final Object lock = new Object();
  private boolean runInferencer = false;
  private boolean checkedPermissions = false;
  private TextView textView;
  private FrameInferencer inferencer;
  private String cameraId;
  private AutoFitTextureView textureView;
  private BoxView boxView;
  private CameraCaptureSession captureSession;
  private CameraDevice cameraDevice;
  private Size previewSize;
  private HandlerThread backgroundThread;
  private Handler backgroundHandler;
  private ImageReader imageReader;
  private CaptureRequest.Builder previewRequestBuilder;
  private CaptureRequest previewRequest;
  private Matrix cropToViewMatrix = new Matrix();

  private Semaphore cameraOpenCloseLock = new Semaphore(1);

  private boolean debug = false;
  private String registerName = "";

  public enum CameraMode {
    CLASSIFIER,
    DETECTOR,
    FACE,
    FACE_REGISTER
  }

  private CameraMode cameraMode;
  private String modelFile;
  private String labelFile;
  private int modelInputWidth;
  private int modelInputHeight;
  private boolean useFrontCamera = false;

  public static CameraFragment newInstance(
      CameraMode mode,
      String modelFile,
      String labelFile,
      int modelInputWidth,
      int modelInputHeight,
      boolean useFrontCamera) {
    CameraFragment theInstance = new CameraFragment();
    theInstance.cameraMode = mode;
    theInstance.modelFile = modelFile;
    theInstance.labelFile = labelFile;
    theInstance.modelInputWidth = modelInputWidth;
    theInstance.modelInputHeight = modelInputHeight;
    theInstance.useFrontCamera = useFrontCamera;

    return theInstance;
  }

  // TODO: remove this ugly code, decouple camera UI and logic
  void setRegisterName(String name) {
    registerName = name;
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return inflater.inflate(R.layout.fragment_camera, container, false);
  }

  @Override
  public void onViewCreated(final View view, Bundle savedInstanceState) {
    textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    textView = (TextView) view.findViewById(R.id.text);
    boxView = (BoxView) view.findViewById(R.id.boxview);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (cameraMode == CameraMode.CLASSIFIER) {
      try {
        inferencer = new ImageClassifier(
            getActivity(), modelFile, labelFile, modelInputWidth, modelInputHeight);
      } catch (IOException e) {
        Log.e(TAG, "Failed to initialize an image classifier.");
      }
    }
    else if (cameraMode == CameraMode.DETECTOR) {
      try {
        inferencer = new ObjectDetector(
            getActivity(), modelFile, labelFile, modelInputWidth, modelInputHeight);
      } catch (IOException e) {
        Log.e(TAG, "Failed to initialize an object detector.");
      }
    }
    else if (cameraMode == CameraMode.FACE) {
      try {
        inferencer = new FaceRecognizer(
          getActivity(), modelFile, labelFile, modelInputWidth, modelInputHeight);
      } catch (IOException e) {
        Log.e(TAG, "Failed to initialize an face recgonizer.");
      }
    }
    else if (cameraMode == CameraMode.FACE_REGISTER) {
      try {
        inferencer = new FaceRegister(
          getActivity(), modelFile, labelFile, modelInputWidth, modelInputHeight, registerName);
      } catch (IOException e) {
        Log.e(TAG, "Failed to initialize an face register.");
      }
    }
    startBackgroundThread();
  }

  @Override
  public void onResume() {
    super.onResume();
    startBackgroundThread();

    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).
    if (textureView.isAvailable()) {
      openCamera(textureView.getWidth(), textureView.getHeight());
    } else {
      textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
          openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
          configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
          return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
      });
    }
  }

  @Override
  public void onPause() {
    closeCamera();
    stopBackgroundThread();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    inferencer.close();
    super.onDestroy();
  }

  private void showInfo(final String text) {
    final Activity activity = getActivity();
    if (activity != null) {
      activity.runOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              textView.setText(text);
            }
          });
    }
  }

  private void showRegisterSuccessToast(String name) {
    final Activity activity = getActivity();
    if (activity != null) {
      Toast.makeText(activity, name + " registered", Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * choose the smallest size that is at least as large as the respective texture view size, and
   * that is at most as large as the respective max size, and whose aspect ratio matches with the
   * specified value. If such size doesn't exist, choose the largest one that is at most as large
   * as the respective max size, and whose aspect ratio matches with the specified value.
   *
   * @param choices The list of sizes that the camera supports for the intended output class
   * @param textureViewWidth The width of the texture view relative to sensor coordinate
   * @param textureViewHeight The height of the texture view relative to sensor coordinate
   * @param maxWidth The maximum width that can be chosen
   * @param maxHeight The maximum height that can be chosen
   * @param aspectRatio The aspect ratio
   * @return The optimal size, or an arbitrary one if none were big enough
   */
  private static Size chooseOptimalSize(
      Size[] choices,
      int textureViewWidth,
      int textureViewHeight,
      int maxWidth,
      int maxHeight,
      Size aspectRatio) {

    // Collect the supported resolutions that are at least as big as the preview Surface
    List<Size> bigEnough = new ArrayList<>();
    // Collect the supported resolutions that are smaller than the preview Surface
    List<Size> notBigEnough = new ArrayList<>();
    int w = aspectRatio.getWidth();
    int h = aspectRatio.getHeight();
    for (Size option : choices) {
      if (option.getWidth() <= maxWidth
          && option.getHeight() <= maxHeight
          && option.getHeight() == option.getWidth() * h / w) {
        if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
          bigEnough.add(option);
        } else {
          notBigEnough.add(option);
        }
      }
    }

    // Pick the smallest of those big enough. If there is no one big enough, pick the
    // largest of those not big enough.
    if (bigEnough.size() > 0) {
      return Collections.min(bigEnough, new CompareSizesByArea());
    } else if (notBigEnough.size() > 0) {
      return Collections.max(notBigEnough, new CompareSizesByArea());
    } else {
      Log.e(TAG, "Couldn't find any suitable preview size");
      return choices[0];
    }
  }

  /**
   * Sets up member variables related to camera.
   *
   * @param width The width of available size for camera preview
   * @param height The height of available size for camera preview
   */
  private void setUpCameraOutputs(int width, int height) {
    Activity activity = getActivity();
    CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    try {
      for (String cameraId : manager.getCameraIdList()) {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT && !useFrontCamera) {
          continue;
        }

        if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK && useFrontCamera) {
          continue;
        }

        StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
          continue;
        }

        // For still image captures, we use the largest available size.
        Size largest = Collections.max(
                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
        imageReader = ImageReader.newInstance(
                largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/ 2);

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions = false;
        switch (displayRotation) {
          case Surface.ROTATION_0:
          case Surface.ROTATION_180:
            if (sensorOrientation == 90 || sensorOrientation == 270) {
              swappedDimensions = true;
            }
            break;
          case Surface.ROTATION_90:
          case Surface.ROTATION_270:
            if (sensorOrientation == 0 || sensorOrientation == 180) {
              swappedDimensions = true;
            }
            break;
          default:
            Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
          rotatedPreviewWidth = height;
          rotatedPreviewHeight = width;
          maxPreviewWidth = displaySize.y;
          maxPreviewHeight = displaySize.x;
        }

        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
          maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
          maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        previewSize =
            chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture.class),
                rotatedPreviewWidth,
                rotatedPreviewHeight,
                maxPreviewWidth,
                maxPreviewHeight,
                largest);

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
          textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
          boxView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        } else {
          textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
          boxView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
        }

        this.cameraId = cameraId;
        return;
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    } catch (NullPointerException e) {
      // Camera2API is used but not supported on the device
      ErrorDialog.newInstance(getString(R.string.camera_error))
          .show(getChildFragmentManager(), FRAGMENT_DIALOG);
    }
  }

  private String[] getRequiredPermissions() {
    Activity activity = getActivity();
    try {
      PackageInfo info = activity
              .getPackageManager()
              .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  private void openCamera(int width, int height) {
    if (!checkedPermissions && !allPermissionsGranted()) {
      FragmentCompat.requestPermissions(this, getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
      return;
    } else {
      checkedPermissions = true;
    }
    setUpCameraOutputs(width, height);
    configureTransform(width, height);
    Activity activity = getActivity();
    CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    try {
      if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw new RuntimeException("Time out waiting to lock camera opening.");
      }
      manager.openCamera(cameraId, new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice currentCameraDevice) {
          cameraOpenCloseLock.release();
          cameraDevice = currentCameraDevice;
          createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice currentCameraDevice) {
          cameraOpenCloseLock.release();
          currentCameraDevice.close();
          cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice currentCameraDevice, int error) {
          cameraOpenCloseLock.release();
          currentCameraDevice.close();
          cameraDevice = null;
          Activity activity = getActivity();
          if (null != activity) {
            activity.finish();
          }
        }
      }, backgroundHandler);
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (CameraAccessException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (ContextCompat.checkSelfPermission(getActivity(), permission)
          != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  private void closeCamera() {
    try {
      cameraOpenCloseLock.acquire();
      if (null != captureSession) {
        captureSession.close();
        captureSession = null;
      }
      if (null != cameraDevice) {
        cameraDevice.close();
        cameraDevice = null;
      }
      if (null != imageReader) {
        imageReader.close();
        imageReader = null;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
    } finally {
      cameraOpenCloseLock.release();
    }
  }

  private void startBackgroundThread() {
    backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
    synchronized (lock) {
      runInferencer = true;
    }
    backgroundHandler.post(periodicInference);
  }

  private void stopBackgroundThread() {
    backgroundThread.quitSafely();
    try {
      backgroundThread.join();
      backgroundThread = null;
      backgroundHandler = null;
      synchronized (lock) {
        runInferencer = false;
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private Runnable periodicInference =
      new Runnable() {
        @Override
        public void run() {
          synchronized (lock) {
            if (runInferencer) {
              runInference();
            }
          }
          backgroundHandler.post(periodicInference);
        }
      };

  private CameraCaptureSession.StateCallback
          stateCallback = new CameraCaptureSession.StateCallback() {

    @Override
    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
      // The camera is already closed
      if (null == cameraDevice) {
        return;
      }

      // When the session is ready, we start displaying the preview.
      captureSession = cameraCaptureSession;
      try {
        // Auto focus should be continuous for camera preview.
        previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        // Finally, we start displaying the camera preview.
        previewRequest = previewRequestBuilder.build();
        captureSession.setRepeatingRequest(
                previewRequest, captureCallback, backgroundHandler);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
      showInfo("Failed");
    }
  };

  private CameraCaptureSession.CaptureCallback
          captureCallback = new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureProgressed(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull CaptureResult partialResult) {}

    @Override
    public void onCaptureCompleted(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull TotalCaptureResult result) {}
  };

  private void createCameraPreviewSession() {
    try {
      SurfaceTexture texture = textureView.getSurfaceTexture();
      assert texture != null;

      // We configure the size of default buffer to be the size of camera preview we want.
      texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

      // This is the output Surface we need to start preview.
      Surface surface = new Surface(texture);

      // We set up a CaptureRequest.Builder with the output Surface.
      previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      previewRequestBuilder.addTarget(surface);

      // Here, we create a CameraCaptureSession for camera preview.
      cameraDevice.createCaptureSession(Arrays.asList(surface), stateCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * Configures transformation for TextureView
   *
   * @param viewWidth The width of `textureView`
   * @param viewHeight The height of `textureView`
   */
  private void configureTransform(int viewWidth, int viewHeight) {
    Activity activity = getActivity();
    if (null == textureView || null == previewSize || null == activity) {
      return;
    }
    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    Matrix matrix = new Matrix();
    RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
    RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
    float centerX = viewRect.centerX();
    float centerY = viewRect.centerY();
    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
      bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
      matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
      float scale = Math.max(
        (float) viewHeight / previewSize.getHeight(),
        (float) viewWidth / previewSize.getWidth());
      matrix.postScale(scale, scale, centerX, centerY);
      matrix.postRotate(90 * (rotation - 2), centerX, centerY);
    } else if (Surface.ROTATION_180 == rotation) {
      matrix.postRotate(180, centerX, centerY);
    }
    textureView.setTransform(matrix);

    if (cameraMode == CameraMode.DETECTOR
      || cameraMode == CameraMode.FACE
      || cameraMode == CameraMode.FACE_REGISTER) {
      RectF detectRect =
          new RectF(
              0,
              0,
              inferencer.getModelInputWidth(),
              inferencer.getModelInputHeight());

      // Attention: the bitmap fed into CNN is sampled from TextureView, NOT from camera preview!
      cropToViewMatrix.setRectToRect(detectRect, viewRect, Matrix.ScaleToFit.FILL);
    }
  }

  private void runInference() {
    if (inferencer == null || getActivity() == null || cameraDevice == null) {
      showInfo("Uninitialized inferencer or invalid context.");
      return;
    }

    // This bitmap has been scaled
    Bitmap bitmap = textureView.getBitmap(inferencer.getModelInputWidth(), inferencer.getModelInputHeight());
    if (debug)
      ImageUtils.saveBitmap(bitmap);

    long start = SystemClock.uptimeMillis();
    FrameInferencer.FrameInferencerResult result = inferencer.inferenceFrame(bitmap);
    long end = SystemClock.uptimeMillis();

    if (cameraMode == CameraMode.CLASSIFIER) {
      StringBuilder stringBuilder = new StringBuilder();
      for (Recognition recog: result.getRecognitions()) {
        stringBuilder
            .append(recog.getTitle()).append(": ").append(recog.getConfidence()).append("\n");
      }
      showInfo(
          stringBuilder.toString() +
          "Inference: " + (end - start) + "ms\n" +
          result.getInfrenceMessage());
    }
    else if (cameraMode == CameraMode.DETECTOR) {
      showInfo("Inference: " + (end - start) + "ms\n" + result.getInfrenceMessage());

      List<Recognition> recognitions = result.getRecognitions();
      final List<Recognition> mappedRecognitions = new LinkedList<Recognition>();

      for (final Recognition recog : recognitions) {
        final RectF location = recog.getLocation();
        if (location != null && recog.getConfidence() >= MINIMUM_DETECTION_CONFIDENCE) {
          cropToViewMatrix.mapRect(location);
          recog.setLocation(location);
          mappedRecognitions.add(recog);
        }
      }
      boxView.drawRecognition(mappedRecognitions);
    }
    else if (cameraMode == CameraMode.FACE || cameraMode == CameraMode.FACE_REGISTER) {
      showInfo("Inference: " + (end - start) + "ms\n" + result.getInfrenceMessage());

      List<Recognition> recognitions = result.getRecognitions();
      final List<Recognition> mappedRecognitions = new LinkedList<Recognition>();

      for (final Recognition recog : recognitions) {
        final RectF location = recog.getLocation();
        if (location != null && recog.getConfidence() >= MINIMUM_DETECTION_CONFIDENCE) {
          cropToViewMatrix.mapRect(location);
          recog.setLocation(location);
          mappedRecognitions.add(recog);
        }
      }
      boxView.drawRecognition(mappedRecognitions);
    }

    bitmap.recycle();

  }

  private static class CompareSizesByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow
      return Long.signum(
          (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }

  public static class ErrorDialog extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    public static ErrorDialog newInstance(String message) {
      ErrorDialog dialog = new ErrorDialog();
      Bundle args = new Bundle();
      args.putString(ARG_MESSAGE, message);
      dialog.setArguments(args);
      return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Activity activity = getActivity();
      return new AlertDialog.Builder(activity)
          .setMessage(getArguments().getString(ARG_MESSAGE))
          .setPositiveButton(
              android.R.string.ok,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  activity.finish();
                }
              })
          .create();
    }
  }
}
