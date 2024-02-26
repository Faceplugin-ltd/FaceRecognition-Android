package com.faceplugin.facerecognition;


import static androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.ocp.facesdk.FaceBox;
import com.ocp.facesdk.FaceDetectionParam;
import com.ocp.facesdk.FaceSDK;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class CaptureActivity extends AppCompatActivity implements CaptureView.ViewModeChanged{

    static String TAG = CaptureActivity.class.getSimpleName();
    static int PREVIEW_WIDTH = 720;
    static int PREVIEW_HEIGHT = 1280;

    private ExecutorService cameraExecutorService;
    private PreviewView viewFinder;
    private Preview preview        = null;
    private ImageAnalysis imageAnalyzer  = null;
    private Camera camera         = null;
    private CameraSelector        cameraSelector = null;
    private ProcessCameraProvider cameraProvider = null;

    private CaptureView captureView;

    private TextView warningTxt;

    private TextView livenessTxt;

    private TextView qualityTxt;

    private TextView luminaceTxt;

    private ConstraintLayout lytCaptureResult;

    private Context context;

    private Bitmap capturedBitmap = null;

    private FaceBox capturedFace = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        context = this;

        viewFinder = findViewById(R.id.preview);
        captureView = findViewById(R.id.captureView);
        warningTxt = findViewById(R.id.txtWarning);
        livenessTxt = findViewById(R.id.txtLiveness);
        qualityTxt = findViewById(R.id.txtQuality);
        luminaceTxt = findViewById(R.id.txtLuminance);
        lytCaptureResult = findViewById(R.id.lytCaptureResult);
        cameraExecutorService = Executors.newFixedThreadPool(1);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            viewFinder.post(() ->
            {
                setUpCamera();
            });
        }

        captureView.setViewModeInterface(this);
        captureView.setViewMode(CaptureView.VIEW_MODE.NO_FACE_PREPARE);

        findViewById(R.id.buttonEnroll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap faceImage = Utils.cropFace(capturedBitmap, capturedFace);
                byte[] templates = FaceSDK.templateExtraction(capturedBitmap, capturedFace);

                DBManager dbManager = new DBManager(context);
                final int min = 10000;
                final int max = 20000;
                final int random = new Random().nextInt((max - min) + 1) + min;

                dbManager.insertPerson("Person" + random, faceImage, templates);
                Toast.makeText(context, getString(R.string.person_enrolled), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        captureView.setFaceBoxes(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                viewFinder.post(() ->
                {
                    setUpCamera();
                });
            }
        }
    }

    private void setUpCamera()
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(CaptureActivity.this);
        cameraProviderFuture.addListener(() -> {

            // CameraProvider
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
            } catch (InterruptedException e) {
            }

            // Build and bind the camera use cases
            bindCameraUseCases();

        }, ContextCompat.getMainExecutor(CaptureActivity.this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeExperimentalUsageError"})
    private void bindCameraUseCases()
    {
        int rotation = viewFinder.getDisplay().getRotation();

        cameraSelector = new CameraSelector.Builder().requireLensFacing(SettingsActivity.getCameraLens(this)).build();

        preview = new Preview.Builder()
                .setTargetResolution(new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT))
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT))
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutorService, new FaceAnalyzer());

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer);

            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        } catch (Exception exc) {
        }
    }

    @Override
    public void view5_finished() {

        FaceDetectionParam param = new FaceDetectionParam();
        param.check_liveness = true;
        param.check_liveness_level = SettingsActivity.getLivenessLevel(this);

        List<FaceBox> faceBoxes = FaceSDK.faceDetection(capturedBitmap, param);
        if(faceBoxes != null && faceBoxes.size() > 0) {
            if(faceBoxes.get(0).liveness > SettingsActivity.getLivenessThreshold(context)) {
                String msg = String.format("Liveness: Real, score = %.03f", faceBoxes.get(0).liveness);
                livenessTxt.setText(msg);
            }
            else {
                String msg = String.format("Liveness: Spoof, score =  %.03f", faceBoxes.get(0).liveness);
                livenessTxt.setText(msg);
            }
        }

        if(capturedFace.face_quality < 0.5f) {
            String msg = String.format("Quality: Low, score = %.03f", capturedFace.face_quality);
            qualityTxt.setText(msg);
        } else if(capturedFace.face_quality < 0.75f) {
            String msg = String.format("Quality: Medium, score = %.03f", capturedFace.face_quality);
            qualityTxt.setText(msg);
        } else {
            String msg = String.format("Quality: High, score = %.03f", capturedFace.face_quality);
            qualityTxt.setText(msg);
        }

        String msg = String.format("Luminance: %.03f", capturedFace.face_luminance);
        luminaceTxt.setText(msg);

        lytCaptureResult.setVisibility(View.VISIBLE);
    }

    class FaceAnalyzer implements ImageAnalysis.Analyzer
    {
        @SuppressLint("UnsafeExperimentalUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy)
        {
            analyzeImage(imageProxy);
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void analyzeImage(ImageProxy imageProxy)
    {
        if(captureView.viewMode == CaptureView.VIEW_MODE.NO_FACE_PREPARE) {
            imageProxy.close();
            return;
        }

        try
        {
            Image image = imageProxy.getImage();

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            Bitmap bitmap  = FaceSDK.yuv2Bitmap(nv21, image.getWidth(), image.getHeight(), 7);

            FaceDetectionParam param = new FaceDetectionParam();
            param.check_face_occlusion = true;
            param.check_eye_closeness = true;
            param.check_mouth_opened = true;

            List<FaceBox> faceBoxes = FaceSDK.faceDetection(bitmap, param);
            FACE_CAPTURE_STATE faceCaptureState = checkFace(faceBoxes, this);

            if(captureView.viewMode == CaptureView.VIEW_MODE.REPEAT_NO_FACE_PREPARE) {
                if(faceCaptureState.compareTo(FACE_CAPTURE_STATE.NO_FACE) > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            captureView.setViewMode(CaptureView.VIEW_MODE.TO_FACE_CIRCLE);
                        }
                    });
                }
            } else if(captureView.viewMode == CaptureView.VIEW_MODE.FACE_CIRCLE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        captureView.setFrameSize(new Size(bitmap.getWidth(), bitmap.getHeight()));
                        captureView.setFaceBoxes(faceBoxes);

                        if(faceCaptureState == FACE_CAPTURE_STATE.NO_FACE) {
                            warningTxt.setText("");

                            captureView.setViewMode(CaptureView.VIEW_MODE.FACE_CIRCLE_TO_NO_FACE);
                        }
                        else if(faceCaptureState == FACE_CAPTURE_STATE.MULTIPLE_FACES)
                            warningTxt.setText("Multiple face detected!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.FIT_IN_CIRCLE)
                            warningTxt.setText("Fit in circle!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.MOVE_CLOSER)
                            warningTxt.setText("Move closer!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.NO_FRONT)
                            warningTxt.setText("Not fronted face!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.FACE_OCCLUDED)
                            warningTxt.setText("Face occluded!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.EYE_CLOSED)
                            warningTxt.setText("Eye closed!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.MOUTH_OPENED)
                            warningTxt.setText("Mouth opened!");
                        else if(faceCaptureState == FACE_CAPTURE_STATE.SPOOFED_FACE)
                            warningTxt.setText("Spoof face");
                        else {
                            warningTxt.setText("");
                            captureView.setViewMode(CaptureView.VIEW_MODE.FACE_CAPTURE_PREPARE);

                            capturedBitmap = bitmap;
                            capturedFace = faceBoxes.get(0);
                            captureView.setCapturedBitmap(capturedBitmap);
                        }
                    }
                });
            } else if(captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_PREPARE) {
                if(faceCaptureState == FACE_CAPTURE_STATE.CAPTURE_OK) {
                    if(faceBoxes.get(0).face_quality > capturedFace.face_quality) {
                        capturedBitmap = bitmap;
                        capturedFace = faceBoxes.get(0);
                        captureView.setCapturedBitmap(capturedBitmap);
                    }
                }
            } else if(captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_DONE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cameraProvider.unbindAll();
                    }
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            imageProxy.close();
        }
    }

    public static FACE_CAPTURE_STATE checkFace(List<FaceBox> faceBoxes, Context context) {
        if(faceBoxes == null || faceBoxes.size() == 0)
            return FACE_CAPTURE_STATE.NO_FACE;

        if(faceBoxes.size() > 1) {
            return FACE_CAPTURE_STATE.MULTIPLE_FACES;
        }

        FaceBox faceBox = faceBoxes.get(0);
        float faceLeft = Float.MAX_VALUE;
        float faceRight = 0f;
        float faceBottom = 0f;
        for(int i = 0; i < 68; i ++) {
            faceLeft = Math.min(faceLeft, faceBox.landmarks_68[i * 2]);
            faceRight = Math.max(faceRight, faceBox.landmarks_68[i * 2]);
            faceBottom = Math.max(faceBottom, faceBox.landmarks_68[i * 2 + 1]);
        }

        float sizeRate = 0.30f;
        float interRate = 0.03f;
        Size frameSize = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        RectF roiRect = CaptureView.getROIRect(frameSize);
        float centerY = (faceBox.y2 + faceBox.y1) / 2;
        float topY = centerY - (faceBox.y2 - faceBox.y1) * 2 / 3;
        float interX = Math.max(0f, roiRect.left - faceLeft) + Math.max(0f, faceRight - roiRect.right);
        float interY = Math.max(0f, roiRect.top - topY) + Math.max(0f, faceBottom - roiRect.bottom);
        if(interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE;
        }

        if(interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE;
        }

        if((faceBox.y2 - faceBox.y1) * (faceBox.x2 - faceBox.x1) <  roiRect.width() * roiRect.height() * sizeRate) {
            return FACE_CAPTURE_STATE.MOVE_CLOSER;
        }

        if(Math.abs(faceBox.yaw) > SettingsActivity.getYawThreshold(context) ||
                Math.abs(faceBox.roll) > SettingsActivity.getRollThreshold(context) ||
                Math.abs(faceBox.pitch) > SettingsActivity.getPitchThreshold(context)) {
            return FACE_CAPTURE_STATE.NO_FRONT;
        }

        if(faceBox.face_occlusion > SettingsActivity.getOcclusionThreshold(context)) {
            return FACE_CAPTURE_STATE.FACE_OCCLUDED;
        }

        if(faceBox.left_eye_closed > SettingsActivity.getEyecloseThreshold(context) ||
                faceBox.right_eye_closed > SettingsActivity.getEyecloseThreshold(context)) {
            return FACE_CAPTURE_STATE.EYE_CLOSED;
        }

        if(faceBox.mouth_opened > SettingsActivity.getMouthopenThreshold(context)) {
            return FACE_CAPTURE_STATE.MOUTH_OPENED;
        }

        return FACE_CAPTURE_STATE.CAPTURE_OK;
    }
}