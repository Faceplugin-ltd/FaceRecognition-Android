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
    private volatile List<FaceBox> lastTrackBoxes = java.util.Collections.emptyList();
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
        leaveIdentify();
        lastTrackBoxes = java.util.Collections.emptyList();
        faceView.setFaceBoxes(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveIdentify();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
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
                client.stopVideoWorker();
                return;
            }
            videoWorkerReady = started == 0 && synced == 0;
        });
    }

    /**
     * Flutter {@code IdentifySession.leave()} / {@code stop()}: drop the handler and
     * stop ingest immediately, wait for in-flight liveness, then destroy VideoWorker
     * on the SDK thread. Never call {@code stopVideoWorker()} on the UI thread —
     * {@code FaceRecognitionQueue.sync} would freeze the looper while native stop
     * waits for a VideoWorker callback that itself posted to the UI.
     */
    private void leaveIdentify() {
        videoWorkerReady = false;
        FaceRecognitionClient client = FaceRecognitionClient.get(this);
        client.setVideoWorkerEventHandler((java.util.function.Consumer<String>) null);
        client.async(() -> {
            long deadline = System.currentTimeMillis() + 2000;
            while (pbBusy.get() && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    break;
                }
            }
            client.stopVideoWorker();
        });
    }

    private void onVideoWorkerEvent(String json) {
        if (recognized.get()) return;
        VideoWorkerEvent event = FaceJson.parseVideoWorkerEvent(json);
        if (event == null) return;
        if (event instanceof VideoWorkerEvent.Tracking) {
            VideoWorkerEvent.Tracking tracking = (VideoWorkerEvent.Tracking) event;
            List<FaceBox> boxes = FaceJson.toFaceBoxes(tracking.getFaces(), true);
            LiveDetect.mergeLiveness(boxes, lastLivenessBoxes);
            lastTrackBoxes = boxes;
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
        if (recognized.get() || !confirming.compareAndSet(false, true)) return;
        recognized.set(true);
        videoWorkerReady = false;
        leaveIdentify();
        Bitmap frame;
        synchronized (this) {
            frame = lastFrame == null ? null : lastFrame.copy(Bitmap.Config.ARGB_8888, false);
        }
        final List<FaceBox> tracked = lastTrackBoxes;
        if (frame == null || tracked == null || tracked.isEmpty()) {
            recognized.set(false);
            confirming.set(false);
            return;
        }
        final FaceBox faceBox = tracked.get(0);
        final int idx = personIndex;
        final float sim = score.floatValue();
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                FaceRecognitionClient client = FaceRecognitionClient.get(context);
                EnrolledPerson person = client.personAtVideoWorkerIndex(idx);
                if (person == null && idx > 0) {
                    person = client.personAtVideoWorkerIndex(idx - 1);
                }
                if (person == null) {
                    recognized.set(false);
                    confirming.set(false);
                    return;
                }
                Bitmap faceImage = Utils.cropFace(frame, faceBox);
                Bitmap enrolledFace = client.thumbnail(person);
                ResultActivity.identifiedFace = faceImage;
                ResultActivity.enrolledFace = enrolledFace;
                Intent intent = new Intent(context, ResultActivity.class);
                intent.putExtra(FaceBoxExtras.IDENTIFIED_NAME, person.getName());
                intent.putExtra(FaceBoxExtras.SIMILARITY, sim);
                FaceBoxExtras.putBox(intent, faceBox);
                if (faceImage != null) {
                    FaceBoxExtras.putCropLandmarks(intent, Utils.mapLandmarksToCrop(
                            frame, faceBox, faceImage.getWidth(), faceImage.getHeight()));
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                recognized.set(false);
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
        if (recognized.get() || !videoWorkerReady || !pbBusy.compareAndSet(false, true)) return;
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
                if (recognized.get() || !videoWorkerReady) return;
                lastLivenessBoxes = client.faceDetection(copy, LiveDetect.livenessOnly(0));
            } catch (Exception ignored) {
            } finally {
                if (!copy.isRecycled()) copy.recycle();
                pbBusy.set(false);
            }
        });
    }
}
