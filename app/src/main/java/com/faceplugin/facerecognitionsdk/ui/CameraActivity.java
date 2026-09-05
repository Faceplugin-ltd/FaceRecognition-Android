package com.faceplugin.facerecognitionsdk.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.faceplugin.facerecognitionsdk.FaceBox;
import com.faceplugin.facerecognitionsdk.FaceDetectionParam;
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK;
import com.faceplugin.facerecognitionsdk.R;
import com.faceplugin.facerecognitionsdk.kit.CameraFrameUtils;
import com.faceplugin.facerecognitionsdk.kit.CameraPreview;
import com.faceplugin.facerecognitionsdk.kit.EnrolledPerson;
import com.faceplugin.facerecognitionsdk.kit.FaceJson;
import com.faceplugin.facerecognitionsdk.kit.FaceRecognitionClient;
import com.faceplugin.facerecognitionsdk.kit.LiveDetect;
import com.faceplugin.facerecognitionsdk.kit.VideoWorkerEvent;
import com.faceplugin.facerecognitionsdk.kit.VideoWorkerFace;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends AppCompatActivity {

    private ExecutorService cameraExecutorService;
    private PreviewView viewFinder;
    private FaceView faceView;
    private Context context;
    private final AtomicBoolean recognized = new AtomicBoolean(false);
    private final AtomicBoolean confirming = new AtomicBoolean(false);
    private final AtomicBoolean pbBusy = new AtomicBoolean(false);
    private volatile boolean videoWorkerReady = false;
    private ProcessCameraProvider cameraProvider = null;
    private Bitmap lastFrame = null;
    private int lastFrameW;
    private int lastFrameH;
    private volatile List<FaceBox> lastLivenessBoxes = java.util.Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        context = this;
        viewFinder = findViewById(R.id.preview);
        faceView = findViewById(R.id.faceView);
        cameraExecutorService = Executors.newFixedThreadPool(1);
        faceView.setMirrorX(SettingsActivity.getCameraLens(this) == CameraSelector.LENS_FACING_FRONT);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            viewFinder.post(this::setUpCamera);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recognized.get()) return;
        confirming.set(false);
        startVideoWorker();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopVideoWorker();
        lastLivenessBoxes = java.util.Collections.emptyList();
        faceView.setFaceBoxes(null);
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

    private void startVideoWorker() {
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        client.setVideoWorkerEventHandler(this::onVideoWorkerEvent);
        float threshold = SettingsActivity.getIdentifyThreshold(this);
        client.async(() -> {
            FaceRecognitionSDK.VideoWorkerConfig config = client.makeTrackingConfig(threshold);
            int started = client.startVideoWorker(config);
            int synced = client.syncDatabase(threshold);
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                FaceRecognitionSDK.stopVideoWorker();
                return;
            }
            videoWorkerReady = started == 0 && synced == 0;
        });
    }

    private void stopVideoWorker() {
        videoWorkerReady = false;
        FaceRecognitionClient.get(this).stopVideoWorker();
    }

    private void onVideoWorkerEvent(String json) {
        if (recognized.get()) return;
        VideoWorkerEvent event = FaceJson.parseVideoWorkerEvent(json);
        if (event == null) return;
        if (event instanceof VideoWorkerEvent.Tracking) {
            VideoWorkerEvent.Tracking tracking = (VideoWorkerEvent.Tracking) event;
            List<FaceBox> boxes = FaceJson.toFaceBoxes(tracking.getFaces(), true);
            LiveDetect.mergeLiveness(boxes, lastLivenessBoxes);
            int frameW = lastFrameW > 0 ? lastFrameW : (int) tracking.getFrameWidth();
            int frameH = lastFrameH > 0 ? lastFrameH : (int) tracking.getFrameHeight();
            runOnUiThread(() -> {
                if (frameW > 0 && frameH > 0) {
                    faceView.setFrameSize(new Size(frameW, frameH));
                }
                faceView.setFaceBoxes(boxes);
            });
            for (VideoWorkerFace face : tracking.getFaces()) {
                if (face.getMatch() != null && face.getMatch().getMatched()) {
                    tryConfirmMatch(face.getMatch().getPersonIndex(), face.getMatch().getScore());
                    break;
                }
            }
        } else if (event instanceof VideoWorkerEvent.Match) {
            VideoWorkerEvent.Match match = (VideoWorkerEvent.Match) event;
            if (match.getMatched()) {
                tryConfirmMatch(match.getPersonIndex(), match.getScore());
            }
        }
    }

    private void tryConfirmMatch(Integer personIndex, Double score) {
        if (personIndex == null || score == null) return;
        if (lastLivenessBoxes == null || lastLivenessBoxes.isEmpty()) return;
        if (liveLivenessFailed()) return;
        if (recognized.get() || !confirming.compareAndSet(false, true)) return;
        Bitmap frame;
        synchronized (this) {
            frame = lastFrame == null ? null : lastFrame.copy(Bitmap.Config.ARGB_8888, false);
        }
        if (frame == null) {
            confirming.set(false);
            return;
        }
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        client.async(() -> {
            try {
                FaceDetectionParam param = FaceDetectionParam.allAttributes();
                param.check_liveness_level = SettingsActivity.getLivenessLevel(this);
                List<FaceBox> detected = client.faceDetection(frame, param);
                FaceBox faceBox = detected.isEmpty() ? null : detected.get(0);
                if (faceBox == null && lastLivenessBoxes != null && !lastLivenessBoxes.isEmpty()) {
                    faceBox = lastLivenessBoxes.get(0);
                }
                if (faceBox == null) {
                    confirming.set(false);
                    return;
                }
                if (!SettingsActivity.livenessPassed(context, faceBox.liveness, faceBox.livenessLabel)
                        && lastLivenessBoxes != null && !lastLivenessBoxes.isEmpty()) {
                    FaceBox live = lastLivenessBoxes.get(0);
                    faceBox.liveness = live.liveness;
                    faceBox.livenessLabel = live.livenessLabel;
                }
                EnrolledPerson person = client.personAtVideoWorkerIndex(personIndex);
                if (person == null && personIndex > 0) {
                    person = client.personAtVideoWorkerIndex(personIndex - 1);
                }
                if (person == null) {
                    confirming.set(false);
                    return;
                }
                recognized.set(true);
                Bitmap faceImage = Utils.cropFace(frame, faceBox);
                Bitmap enrolledFace = client.thumbnail(person);
                final EnrolledPerson matched = person;
                final FaceBox resultBox = faceBox;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    ResultActivity.identifiedFace = faceImage;
                    ResultActivity.enrolledFace = enrolledFace;
                    Intent intent = new Intent(context, ResultActivity.class);
                    intent.putExtra(FaceBoxExtras.IDENTIFIED_NAME, matched.getName());
                    intent.putExtra(FaceBoxExtras.SIMILARITY, score.floatValue());
                    FaceBoxExtras.putBox(intent, resultBox);
                    if (faceImage != null) {
                        FaceBoxExtras.putCropLandmarks(intent, Utils.mapLandmarksToCrop(
                                frame, resultBox, faceImage.getWidth(), faceImage.getHeight()));
                    }
                    try {
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        recognized.set(false);
                        confirming.set(false);
                    }
                });
            } catch (Exception e) {
                confirming.set(false);
            }
        });
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

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(androidx.camera.core.ImageProxy imageProxy) {
        Bitmap frame = null;
        try {
            if (recognized.get() || !videoWorkerReady) return;
            boolean backCamera = SettingsActivity.getCameraLens(context) == CameraSelector.LENS_FACING_BACK;
            frame = CameraFrameUtils.fromImageProxy(imageProxy, backCamera);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
        if (frame == null || recognized.get() || !videoWorkerReady) return;
        synchronized (this) {
            if (lastFrame != null && lastFrame != frame && !lastFrame.isRecycled()) {
                lastFrame.recycle();
            }
            lastFrame = frame;
            lastFrameW = frame.getWidth();
            lastFrameH = frame.getHeight();
        }
        FaceRecognitionClient.get(this).addFrame(frame);
        requestLiveness(frame);
    }

    private void requestLiveness(Bitmap frame) {
        if (recognized.get() || !pbBusy.compareAndSet(false, true)) return;
        final Bitmap copy;
        try {
            copy = CameraFrameUtils.copyArgb(frame);
        } catch (Exception e) {
            pbBusy.set(false);
            return;
        }
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        int level = SettingsActivity.getLivenessLevel(this);
        client.async(() -> {
            try {
                lastLivenessBoxes = client.faceDetection(copy, LiveDetect.livenessOnly(level));
            } catch (Exception ignored) {
            } finally {
                if (!copy.isRecycled()) copy.recycle();
                pbBusy.set(false);
            }
        });
    }

    private boolean liveLivenessFailed() {
        List<FaceBox> pb = lastLivenessBoxes;
        if (pb == null || pb.isEmpty()) return false;
        FaceBox live = pb.get(0);
        return !SettingsActivity.livenessPassed(context, live.liveness, live.livenessLabel);
    }
}
