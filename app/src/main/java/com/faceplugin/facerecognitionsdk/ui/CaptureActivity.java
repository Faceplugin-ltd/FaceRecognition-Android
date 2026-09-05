package com.faceplugin.facerecognitionsdk.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.faceplugin.facerecognitionsdk.FaceBox;
import com.faceplugin.facerecognitionsdk.FaceDetectionParam;
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK;
import com.faceplugin.facerecognitionsdk.R;
import com.faceplugin.facerecognitionsdk.kit.CameraFrameUtils;
import com.faceplugin.facerecognitionsdk.kit.CameraPreview;
import com.faceplugin.facerecognitionsdk.kit.FaceJson;
import com.faceplugin.facerecognitionsdk.kit.FaceRecognitionClient;
import com.faceplugin.facerecognitionsdk.kit.LiveDetect;
import com.faceplugin.facerecognitionsdk.kit.VideoWorkerEvent;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CaptureActivity extends AppCompatActivity implements CaptureView.ViewModeChanged {

    private ExecutorService cameraExecutorService;
    private PreviewView viewFinder;
    private CaptureView captureView;
    private TextView warningTxt;
    private TextView livenessTxt;
    private TextView qualityTxt;
    private TextView luminaceTxt;
    private ConstraintLayout lytCaptureResult;
    private Context context;
    private Bitmap capturedBitmap = null;
    private FaceBox capturedFace = null;
    private ProcessCameraProvider cameraProvider = null;
    private volatile boolean videoWorkerReady = false;
    private final AtomicBoolean pbBusy = new AtomicBoolean(false);
    private Size lastFrameSize = new Size(CameraPreview.WIDTH, CameraPreview.HEIGHT);
    private Bitmap lastFrame = null;
    private volatile List<FaceBox> lastEyeBoxes = java.util.Collections.emptyList();

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
            viewFinder.post(this::setUpCamera);
        }

        captureView.setViewModeInterface(this);
        captureView.setViewMode(CaptureView.VIEW_MODE.NO_FACE_PREPARE);
        captureView.setMirrorX(SettingsActivity.getCameraLens(this) == CameraSelector.LENS_FACING_FRONT);

        findViewById(R.id.buttonEnroll).setOnClickListener(view -> {
            Bitmap bitmap = capturedBitmap;
            FaceBox face = capturedFace;
            if (bitmap == null || face == null) {
                Toast.makeText(context, getString(R.string.enroll_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            FaceRecognitionClient client = FaceRecognitionClient.get(context);
            client.async(() -> {
                Bitmap faceImage = Utils.cropFace(bitmap, face);
                byte[] templates = client.templateExtraction(bitmap, face);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (templates == null || templates.length == 0) {
                        Toast.makeText(context, getString(R.string.enroll_failed), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int random = new Random().nextInt(10001) + 10000;
                    client.enroll("Person" + random, templates, faceImage);
                    Toast.makeText(context, getString(R.string.person_enrolled), Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (captureView.viewMode != CaptureView.VIEW_MODE.FACE_CAPTURE_DONE) {
            startVideoWorker();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopVideoWorker();
        lastEyeBoxes = java.util.Collections.emptyList();
        captureView.setFaceBoxes(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoWorkerReady = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        FaceRecognitionClient.get(this).stopVideoWorker();
        cameraExecutorService.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            viewFinder.post(this::setUpCamera);
        }
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception ignored) {
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        try {
            CameraPreview.bind(
                    this,
                    cameraProvider,
                    viewFinder,
                    SettingsActivity.getCameraLens(this),
                    cameraExecutorService,
                    this::analyzeImage);
        } catch (Exception ignored) {
        }
    }

    private void startVideoWorker() {
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        client.setVideoWorkerEventHandler(this::onVideoWorkerEvent);
        client.async(() -> {
            FaceRecognitionSDK.VideoWorkerConfig config = client.makeTrackingConfig(0.8f);
            int started = client.startVideoWorker(config);
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                FaceRecognitionSDK.stopVideoWorker();
                return;
            }
            videoWorkerReady = started == 0;
        });
    }

    private void stopVideoWorker() {
        videoWorkerReady = false;
        FaceRecognitionClient.get(this).stopVideoWorker();
    }

    private void onVideoWorkerEvent(String json) {
        VideoWorkerEvent event = FaceJson.parseVideoWorkerEvent(json);
        if (!(event instanceof VideoWorkerEvent.Tracking)) return;
        VideoWorkerEvent.Tracking tracking = (VideoWorkerEvent.Tracking) event;
        List<FaceBox> faceBoxes = FaceJson.toFaceBoxes(tracking.getFaces(), false);
        LiveDetect.mergeEyes(
                faceBoxes,
                lastEyeBoxes,
                SettingsActivity.getCameraLens(this) == CameraSelector.LENS_FACING_FRONT);
        Bitmap frame;
        synchronized (this) {
            frame = lastFrame;
        }
        Size frameSize = lastFrameSize;
        if (frame != null) {
            frameSize = new Size(frame.getWidth(), frame.getHeight());
        }
        applyCaptureTracking(faceBoxes, frame, frameSize);
    }

    private void applyCaptureTracking(List<FaceBox> faceBoxes, Bitmap frame, Size frameSize) {
        FACE_CAPTURE_STATE faceCaptureState = checkFace(faceBoxes, this, frameSize);

        if (captureView.viewMode == CaptureView.VIEW_MODE.REPEAT_NO_FACE_PREPARE) {
            if (faceCaptureState.compareTo(FACE_CAPTURE_STATE.NO_FACE) > 0) {
                runOnUiThread(() -> captureView.setViewMode(CaptureView.VIEW_MODE.TO_FACE_CIRCLE));
            }
        } else if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CIRCLE) {
            Bitmap captureCopy = null;
            FaceBox captureFace = null;
            if (faceCaptureState == FACE_CAPTURE_STATE.CAPTURE_OK && frame != null && !faceBoxes.isEmpty()) {
                synchronized (this) {
                    if (frame != null && !frame.isRecycled()) {
                        captureCopy = frame.copy(Bitmap.Config.ARGB_8888, false);
                    }
                }
                captureFace = faceBoxes.get(0);
            }
            Bitmap captured = captureCopy;
            FaceBox capturedBox = captureFace;
            runOnUiThread(() -> {
                captureView.setFrameSize(frameSize);
                captureView.setFaceBoxes(faceBoxes);
                if (faceCaptureState == FACE_CAPTURE_STATE.NO_FACE) {
                    warningTxt.setText("");
                    captureView.setViewMode(CaptureView.VIEW_MODE.FACE_CIRCLE_TO_NO_FACE);
                } else if (faceCaptureState == FACE_CAPTURE_STATE.CAPTURE_OK) {
                    if (lastEyeBoxes == null || lastEyeBoxes.isEmpty()) {
                        warningTxt.setText("");
                        return;
                    }
                    warningTxt.setText("");
                    captureView.setViewMode(CaptureView.VIEW_MODE.FACE_CAPTURE_PREPARE);
                    if (captured != null && capturedBox != null) {
                        capturedBitmap = captured;
                        capturedFace = capturedBox;
                        captureView.setCapturedBitmap(capturedBitmap);
                    }
                } else {
                    warningTxt.setText(warningFor(faceCaptureState));
                }
            });
        } else if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_PREPARE) {
            if (faceCaptureState == FACE_CAPTURE_STATE.CAPTURE_OK && frame != null && !faceBoxes.isEmpty()) {
                Bitmap copy;
                synchronized (this) {
                    if (frame.isRecycled()) return;
                    copy = frame.copy(Bitmap.Config.ARGB_8888, false);
                }
                FaceBox face = faceBoxes.get(0);
                runOnUiThread(() -> {
                    capturedBitmap = copy;
                    capturedFace = face;
                    captureView.setCapturedBitmap(capturedBitmap);
                });
            }
        } else if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_DONE) {
            runOnUiThread(() -> {
                if (cameraProvider != null) cameraProvider.unbindAll();
            });
        }
    }

    @Override
    public void view5_finished() {
        Bitmap bitmap = capturedBitmap;
        FaceBox fallback = capturedFace;
        if (bitmap == null) {
            lytCaptureResult.setVisibility(View.VISIBLE);
            return;
        }
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        int level = SettingsActivity.getLivenessLevel(this);
        client.async(() -> {
            FaceBox shown = fallback;
            try {
                FaceDetectionParam param = FaceDetectionParam.allAttributes();
                param.check_liveness_level = level;
                List<FaceBox> faceBoxes = client.faceDetection(bitmap, param);
                if (faceBoxes != null && !faceBoxes.isEmpty()) {
                    shown = faceBoxes.get(0);
                }
            } catch (Exception ignored) {
            }
            FaceBox result = shown;
            runOnUiThread(() -> applyCaptureResult(result));
        });
    }

    private void applyCaptureResult(FaceBox shown) {
        if (isFinishing() || isDestroyed()) return;
        if (shown == null || capturedBitmap == null) {
            lytCaptureResult.setVisibility(View.VISIBLE);
            return;
        }
        capturedFace = shown;
        if (shown.livenessLabel != null && !shown.livenessLabel.isEmpty()
                && (shown.livenessLabel.toLowerCase().contains("spoof")
                || shown.livenessLabel.toLowerCase().contains("fake"))) {
            livenessTxt.setText("Liveness: Spoof, score = " + shown.liveness);
        } else if (shown.liveness >= SettingsActivity.getLivenessThreshold(context)) {
            livenessTxt.setText("Liveness: Real, score = " + shown.liveness);
        } else {
            livenessTxt.setText("Liveness: Spoof, score = " + shown.liveness);
        }
        qualityTxt.setText(ResultDetails.qualityText(shown.face_quality));
        if (shown.qualityLabel != null && !shown.qualityLabel.isEmpty()) {
            qualityTxt.append("\n" + shown.qualityLabel);
        }
        luminaceTxt.setText("Luminance: " + shown.face_luminance);
        FACE_CAPTURE_STATE stillState = checkFace(java.util.Collections.singletonList(shown), this,
                new Size(capturedBitmap.getWidth(), capturedBitmap.getHeight()));
        if (stillState == FACE_CAPTURE_STATE.FACE_OCCLUDED) {
            warningTxt.setText("Face occluded!");
        } else if (stillState == FACE_CAPTURE_STATE.EYE_CLOSED) {
            warningTxt.setText("Eye closed!");
        }
        lytCaptureResult.setVisibility(View.VISIBLE);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(androidx.camera.core.ImageProxy imageProxy) {
        Bitmap frame = null;
        try {
            if (captureView.viewMode == CaptureView.VIEW_MODE.NO_FACE_PREPARE
                    || captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_DONE
                    || !videoWorkerReady) {
                return;
            }
            boolean backCamera = SettingsActivity.getCameraLens(context) == CameraSelector.LENS_FACING_BACK;
            frame = CameraFrameUtils.fromImageProxy(imageProxy, backCamera);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
        if (frame == null
                || captureView.viewMode == CaptureView.VIEW_MODE.NO_FACE_PREPARE
                || captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_DONE
                || !videoWorkerReady) {
            return;
        }
        synchronized (this) {
            if (lastFrame != null && lastFrame != frame && !lastFrame.isRecycled()) {
                lastFrame.recycle();
            }
            lastFrame = frame;
            lastFrameSize = new Size(frame.getWidth(), frame.getHeight());
        }
        FaceRecognitionClient.get(this).addFrame(frame);
        requestEyes(frame);
    }

    private void requestEyes(Bitmap frame) {
        if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_DONE) return;
        if (!pbBusy.compareAndSet(false, true)) return;
        final Bitmap copy;
        try {
            copy = CameraFrameUtils.copyArgb(frame);
        } catch (Exception e) {
            pbBusy.set(false);
            return;
        }
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        client.async(() -> {
            try {
                lastEyeBoxes = client.faceDetection(copy, LiveDetect.eyesOnly());
            } catch (Exception ignored) {
            } finally {
                if (!copy.isRecycled()) copy.recycle();
                pbBusy.set(false);
            }
        });
    }

    private static String warningFor(FACE_CAPTURE_STATE state) {
        switch (state) {
            case MULTIPLE_FACES:
                return "Multiple face detected!";
            case FIT_IN_CIRCLE:
                return "Fit in circle!";
            case MOVE_CLOSER:
                return "Move closer!";
            case NO_FRONT:
                return "Not fronted face!";
            case FACE_OCCLUDED:
                return "Face occluded!";
            case EYE_CLOSED:
                return "Eye closed!";
            case SPOOFED_FACE:
                return "Spoof face";
            default:
                return "";
        }
    }

    public static FACE_CAPTURE_STATE checkFace(List<FaceBox> faceBoxes, Context context, Size frameSize) {
        if (faceBoxes == null || faceBoxes.size() == 0)
            return FACE_CAPTURE_STATE.NO_FACE;
        if (faceBoxes.size() > 1) return FACE_CAPTURE_STATE.MULTIPLE_FACES;

        FaceBox faceBox = faceBoxes.get(0);
        float faceLeft = Float.MAX_VALUE;
        float faceRight = 0f;
        float faceBottom = 0f;
        int nMarks = Math.max(0, Math.min(faceBox.landmarkCount, faceBox.landmarks_68.length / 2));
        if (nMarks >= 5) {
            for (int i = 0; i < nMarks; i++) {
                faceLeft = Math.min(faceLeft, faceBox.landmarks_68[i * 2]);
                faceRight = Math.max(faceRight, faceBox.landmarks_68[i * 2]);
                faceBottom = Math.max(faceBottom, faceBox.landmarks_68[i * 2 + 1]);
            }
        } else {
            faceLeft = faceBox.x1;
            faceRight = faceBox.x2;
            faceBottom = faceBox.y2;
        }

        float sizeRate = 0.30f;
        float interRate = 0.03f;
        if (frameSize == null || frameSize.getWidth() <= 0 || frameSize.getHeight() <= 0) {
            frameSize = new Size(CameraPreview.WIDTH, CameraPreview.HEIGHT);
        }
        RectF roiRect = CaptureView.getROIRect(frameSize);
        float centerY = (faceBox.y2 + faceBox.y1) / 2f;
        float topY = centerY - (faceBox.y2 - faceBox.y1) * 2f / 3f;
        float interX = Math.max(0f, roiRect.left - faceLeft) + Math.max(0f, faceRight - roiRect.right);
        float interY = Math.max(0f, roiRect.top - topY) + Math.max(0f, faceBottom - roiRect.bottom);
        if (interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE;
        }
        if ((faceBox.y2 - faceBox.y1) * (faceBox.x2 - faceBox.x1) < roiRect.width() * roiRect.height() * sizeRate) {
            return FACE_CAPTURE_STATE.MOVE_CLOSER;
        }
        if (Math.abs(faceBox.yaw) > SettingsActivity.getYawThreshold(context)
                || Math.abs(faceBox.roll) > SettingsActivity.getRollThreshold(context)
                || Math.abs(faceBox.pitch) > SettingsActivity.getPitchThreshold(context)) {
            return FACE_CAPTURE_STATE.NO_FRONT;
        }
        String mask = faceBox.maskLabel == null ? "" : faceBox.maskLabel.toLowerCase();
        if (mask.contains("yes")) {
            return FACE_CAPTURE_STATE.FACE_OCCLUDED;
        }
        String left = faceBox.eyesLeftLabel == null ? "" : faceBox.eyesLeftLabel.toLowerCase();
        String right = faceBox.eyesRightLabel == null ? "" : faceBox.eyesRightLabel.toLowerCase();
        if (left.contains("closed") || right.contains("closed")) {
            return FACE_CAPTURE_STATE.EYE_CLOSED;
        }
        if (left.isEmpty() && right.isEmpty()
                && (faceBox.left_eye_closed > SettingsActivity.getEyecloseThreshold(context)
                || faceBox.right_eye_closed > SettingsActivity.getEyecloseThreshold(context))) {
            return FACE_CAPTURE_STATE.EYE_CLOSED;
        }
        return FACE_CAPTURE_STATE.CAPTURE_OK;
    }
}
