package com.faceplugin.facerecognitionsdk.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;

import com.faceplugin.facerecognitionsdk.FaceBox;

import java.util.List;

public class FaceView extends View {

    private Context context;
    private Paint realPaint;
    private Paint spoofPaint;
    private Paint trackPaint;

    private Size frameSize;
    private boolean mirrorX;

    private List<FaceBox> faceBoxes;

    public FaceView(Context context) {
        this(context, null);

        this.context = context;
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        realPaint = new Paint();
        realPaint.setStyle(Paint.Style.STROKE);
        realPaint.setStrokeWidth(3);
        realPaint.setColor(Color.GREEN);
        realPaint.setAntiAlias(true);
        realPaint.setTextSize(50);

        spoofPaint = new Paint();
        spoofPaint.setStyle(Paint.Style.STROKE);
        spoofPaint.setStrokeWidth(3);
        spoofPaint.setColor(Color.RED);
        spoofPaint.setAntiAlias(true);
        spoofPaint.setTextSize(50);

        trackPaint = new Paint();
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(3);
        trackPaint.setColor(Color.CYAN);
        trackPaint.setAntiAlias(true);
        trackPaint.setTextSize(50);
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public void setMirrorX(boolean mirrorX) {
        this.mirrorX = mirrorX;
    }

    public void setFaceBoxes(List<FaceBox> faceBoxes)
    {
        this.faceBoxes = faceBoxes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (frameSize != null &&  faceBoxes != null) {
            float frameW = frameSize.getWidth();
            float frameH = frameSize.getHeight();
            float viewW = canvas.getWidth();
            float viewH = canvas.getHeight();
            if (frameW <= 0f || frameH <= 0f || viewW <= 0f || viewH <= 0f) return;

            // PreviewView FILL_CENTER: scale to fill, then center (may crop).
            float scale = Math.max(viewW / frameW, viewH / frameH);
            float dx = (viewW - frameW * scale) / 2f;
            float dy = (viewH - frameH * scale) / 2f;

            for (int i = 0; i < faceBoxes.size(); i++) {
                FaceBox faceBox = faceBoxes.get(i);
                float left = mapX(faceBox.x1, frameW, scale, dx, viewW);
                float right = mapX(faceBox.x2, frameW, scale, dx, viewW);
                float top = faceBox.y1 * scale + dy;
                float bottom = faceBox.y2 * scale + dy;
                if (left > right) {
                    float tmp = left;
                    left = right;
                    right = tmp;
                }

                boolean livenessKnown = hasLiveness(faceBox);
                boolean live = livenessKnown
                        && SettingsActivity.livenessPassed(context, faceBox.liveness, faceBox.livenessLabel);
                Paint boxPaint;
                String label = null;
                if (!livenessKnown) {
                    boxPaint = trackPaint;
                } else if (!live) {
                    boxPaint = spoofPaint;
                    label = "SPOOF " + faceBox.liveness;
                } else {
                    boxPaint = realPaint;
                    label = "REAL " + faceBox.liveness;
                }

                if (label != null) {
                    boxPaint.setStrokeWidth(3);
                    boxPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    canvas.drawText(label, left + 10, top - 30, boxPaint);
                }
                boxPaint.setStrokeWidth(5);
                boxPaint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(new Rect((int) left, (int) top, (int) right, (int) bottom), boxPaint);

                boxPaint.setStyle(Paint.Style.FILL);
                int n = Math.max(0, Math.min(faceBox.landmarkCount, faceBox.landmarks_68.length / 2));
                for (int p = 0; p < n; p++) {
                    float lx = mapX(faceBox.landmarks_68[p * 2], frameW, scale, dx, viewW);
                    float ly = faceBox.landmarks_68[p * 2 + 1] * scale + dy;
                    canvas.drawCircle(lx, ly, 5, boxPaint);
                }
            }
        }
    }

    private float mapX(float frameX, float frameW, float scale, float dx, float viewW) {
        float vx = frameX * scale + dx;
        return mirrorX ? viewW - vx : vx;
    }

    private static boolean hasLiveness(FaceBox faceBox) {
        if (faceBox.livenessLabel != null && !faceBox.livenessLabel.isEmpty()) return true;
        return faceBox.liveness > 0.001f;
    }
}
