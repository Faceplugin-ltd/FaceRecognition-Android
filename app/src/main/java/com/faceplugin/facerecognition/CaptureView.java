package com.faceplugin.facerecognition;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ocp.facesdk.FaceBox;

import java.util.List;

public class CaptureView extends View implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {

    enum VIEW_MODE {
        MODE_NONE,
        NO_FACE_PREPARE,
        REPEAT_NO_FACE_PREPARE,
        TO_FACE_CIRCLE,
        FACE_CIRCLE_TO_NO_FACE,
        FACE_CIRCLE,
        FACE_CAPTURE_PREPARE,
        FACE_CAPTURE_DONE,
    }

    private Context context;

    private Paint scrimPaint;

    private Paint eraserPaint;

    private Paint outSideRoundPaint;

    private Paint outSideRoundNonPaint;

    private Paint outSideActiveRoundPaint;

    private Paint outSideRoundNonFacePaint;

    private Paint outSideRoundFacePaint;

    private Paint outSideRoundActiveFacePaint;
    private boolean scrimInited;
    private Size frameSize = new Size(720, 1280);

    private List<FaceBox> faceBoxes;

    private float animateValue;
    private ValueAnimator valueAnimator;

    public VIEW_MODE viewMode = VIEW_MODE.MODE_NONE;

    private int repeatCount = 0;

    private ViewModeChanged viewModeInterface;

    private Bitmap capturedBitmap;

    private Bitmap roiBitmap;

    interface ViewModeChanged
    {
        public void view5_finished();
    }

    public void setViewModeInterface(ViewModeChanged viewMode) {
        viewModeInterface = viewMode;
    }

    public CaptureView(Context context) {
        this(context, null);

        this.context = context;
        init();
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        scrimPaint = new Paint();

        eraserPaint = new Paint();
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        outSideRoundPaint = new Paint();
        outSideRoundPaint.setStyle(Paint.Style.STROKE);
        outSideRoundPaint.setStrokeWidth(7);
        outSideRoundPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_errorContainer));
        outSideRoundPaint.setAntiAlias(true);

        outSideRoundNonPaint = new Paint();
        outSideRoundNonPaint.setStyle(Paint.Style.STROKE);
        outSideRoundNonPaint.setStrokeWidth(2);
        outSideRoundNonPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_inverseOnSurface));
        outSideRoundNonPaint.setAntiAlias(true);

        outSideActiveRoundPaint = new Paint();
        outSideActiveRoundPaint.setStyle(Paint.Style.STROKE);
        outSideActiveRoundPaint.setStrokeWidth(8);
        outSideActiveRoundPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_onSurface));
        outSideActiveRoundPaint.setAntiAlias(true);

        outSideRoundNonFacePaint = new Paint();
        outSideRoundNonFacePaint.setStyle(Paint.Style.STROKE);
        outSideRoundNonFacePaint.setStrokeWidth(10);
        outSideRoundNonFacePaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_inverseOnSurface));
        outSideRoundNonFacePaint.setAntiAlias(true);

        outSideRoundFacePaint = new Paint();
        outSideRoundFacePaint.setStyle(Paint.Style.STROKE);
        outSideRoundFacePaint.setStrokeWidth(10);
        outSideRoundFacePaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_errorContainer));
        outSideRoundFacePaint.setAntiAlias(true);

        outSideRoundActiveFacePaint = new Paint();
        outSideRoundActiveFacePaint.setStyle(Paint.Style.STROKE);
        outSideRoundActiveFacePaint.setStrokeWidth(10);
        outSideRoundActiveFacePaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_onPrimaryContainer));
        outSideRoundActiveFacePaint.setAntiAlias(true);
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public void setFaceBoxes(List<FaceBox> faceBoxes)
    {
        this.faceBoxes = faceBoxes;
        invalidate();
    }

    public void setCapturedBitmap(Bitmap bitmap) {
        capturedBitmap = bitmap;

        RectF roiRect = CaptureView.getROIRect1(frameSize);

        float ratioView = getWidth() / (float)getHeight();
        float ratioFrame = frameSize.getWidth() / (float)frameSize.getHeight();
        RectF roiViewRect = new RectF();

        if(ratioView < ratioFrame) {
            float dx = ((getHeight() * ratioFrame) - getWidth()) / 2;
            float dy = 0f;
            float ratio = getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        } else {
            float dx = 0;
            float dy = ((getWidth() / ratioFrame) - getHeight()) / 2;
            float ratio = getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        }

        Rect roiRectSrc = new Rect();
        Rect roiViewRectSrc = new Rect();
        roiRect.round(roiRectSrc);
        roiViewRect.round(roiViewRectSrc);
        roiBitmap = Bitmap.createBitmap(roiRectSrc.width(), roiRectSrc.height(), Bitmap.Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle(
                (float) (roiRectSrc.width() / 2)
                , (float) (roiRectSrc.height() / 2)
                , (float) Math.min(roiRectSrc.width(), (roiRectSrc.height() / 2))
                , Path.Direction.CCW
        );

        final Canvas canvas1 = new Canvas(roiBitmap);
        canvas1.clipPath(path);
        canvas1.drawBitmap(capturedBitmap, roiRectSrc, new Rect(0, 0, roiRectSrc.width(), roiRectSrc.height()), null);
    }

    public void setViewMode(VIEW_MODE mode) {
        this.viewMode = mode;

        if(valueAnimator != null) {
            valueAnimator.pause();
        }

        if(this.viewMode == VIEW_MODE.NO_FACE_PREPARE) {
            ValueAnimator animator = ValueAnimator.ofFloat(1.4f, 0.88f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setDuration(800);

            valueAnimator = animator;
        } else if(viewMode == VIEW_MODE.REPEAT_NO_FACE_PREPARE) {
            ValueAnimator animator = ValueAnimator.ofFloat(0.88f, 0.92f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(-1);
            animator.setDuration(1300);

            valueAnimator = animator;
        } else if(viewMode == VIEW_MODE.TO_FACE_CIRCLE) {
            ValueAnimator animator = ValueAnimator.ofFloat(1.4f, 0.0f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setDuration(800);

            valueAnimator = animator;
        } else if(viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1.0f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setDuration(600);

            valueAnimator = animator;
        } else if(viewMode == VIEW_MODE.FACE_CIRCLE) {
            invalidate();
            return;
        } else if(viewMode == VIEW_MODE.FACE_CAPTURE_PREPARE) {
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setDuration(500);

            valueAnimator = animator;
        } else if(viewMode == VIEW_MODE.FACE_CAPTURE_DONE) {
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setDuration(500);
        }

        valueAnimator.start();
    }

    @Override
    public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
        float value = (float)valueAnimator.getAnimatedValue();
        animateValue = value;
        invalidate();
    }

    @Override
    public void onAnimationStart(@NonNull Animator animator) {
        repeatCount = 0;
    }

    @Override
    public void onAnimationEnd(@NonNull Animator animator) {
        if(viewMode == VIEW_MODE.NO_FACE_PREPARE) {
            setViewMode(VIEW_MODE.REPEAT_NO_FACE_PREPARE);
        } else if(viewMode == VIEW_MODE.TO_FACE_CIRCLE) {
            setViewMode(VIEW_MODE.FACE_CIRCLE);
        } else if(viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {
            setViewMode(VIEW_MODE.NO_FACE_PREPARE);
        } else if(viewMode == VIEW_MODE.FACE_CAPTURE_PREPARE) {
            setViewMode(VIEW_MODE.FACE_CAPTURE_DONE);
        } else if(viewMode == VIEW_MODE.FACE_CAPTURE_DONE) {
            if(viewModeInterface != null) {
                viewModeInterface.view5_finished();
            }
        }
    }

    @Override
    public void onAnimationCancel(@NonNull Animator animator) {
    }

    @Override
    public void onAnimationRepeat(@NonNull Animator animator) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(scrimInited == false) {
            scrimInited = true;
            scrimPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            canvas.getWidth(),
                            canvas.getHeight(),
                            context.getColor(R.color.md_theme_dark_surface),
                            context.getColor(R.color.md_theme_dark_scrim),
                            Shader.TileMode.CLAMP));
        }

        if(viewMode == VIEW_MODE.FACE_CIRCLE ||
                viewMode == VIEW_MODE.FACE_CAPTURE_PREPARE ||
                viewMode == VIEW_MODE.FACE_CAPTURE_DONE ||
                (viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue < 1.0f) ||
                viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {

            if(viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {
                scrimPaint.setAlpha((int)((1 - animateValue) * 255));
            } else {
                scrimPaint.setAlpha(255);
            }
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);
        }

        RectF roiRect = CaptureView.getROIRect1(frameSize);

        float ratioView = canvas.getWidth() / (float)canvas.getHeight();
        float ratioFrame = frameSize.getWidth() / (float)frameSize.getHeight();
        RectF roiViewRect = new RectF();

        if(ratioView < ratioFrame) {
            float dx = ((canvas.getHeight() * ratioFrame) - canvas.getWidth()) / 2;
            float dy = 0f;
            float ratio = canvas.getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        } else {
            float dx = 0;
            float dy = ((canvas.getWidth() / ratioFrame) - canvas.getHeight()) / 2;
            float ratio = canvas.getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        }

        if(viewMode == VIEW_MODE.NO_FACE_PREPARE ||
                viewMode == VIEW_MODE.REPEAT_NO_FACE_PREPARE ||
                viewMode == VIEW_MODE.TO_FACE_CIRCLE ||
                viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {

            RectF scaleRoiRect = roiViewRect;
            if(viewMode == VIEW_MODE.NO_FACE_PREPARE ||
                    viewMode == VIEW_MODE.REPEAT_NO_FACE_PREPARE ||
                    (viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue > 1.0f)) {
                CaptureView.scale(scaleRoiRect, animateValue);
            }

            float lineWidth1 = scaleRoiRect.width() / 5;
            float lineWidthOffset1 = 0;
            if(viewMode == VIEW_MODE.FACE_CIRCLE ||
                    (viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue < 1.0f) ||
                    viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {
                lineWidth1 = lineWidth1 * animateValue;
                lineWidthOffset1 = scaleRoiRect.width() / 2 * (1 - animateValue);
            }
            float lineHeight1 = scaleRoiRect.height() / 5;
            float lineHeightOffset1 = 0;
            if(viewMode == VIEW_MODE.FACE_CIRCLE ||
                    (viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue < 1.0f) ||
                    viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {
                lineHeight1 = lineHeight1 * animateValue;
                lineHeightOffset1 = scaleRoiRect.height() / 2 * (1 - animateValue);
            }
            float quad_r1 = scaleRoiRect.width() / 12;
            if(viewMode == VIEW_MODE.FACE_CIRCLE ||
                    (viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue < 1.0f) ||
                    viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {
                quad_r1 = scaleRoiRect.width() / 12 + (scaleRoiRect.width() / 2 - scaleRoiRect.width() / 12) * (1 - animateValue) - 20;
            }

            Paint paint1 = new Paint();
            paint1.setStyle(Paint.Style.STROKE);
            paint1.setStrokeWidth(10);
            paint1.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_onPrimaryContainer));
            if(viewMode == VIEW_MODE.NO_FACE_PREPARE ||
                    (viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue > 1.0f)) {
                int alpha = Math.min(255, (int)((1.4 - animateValue) / 0.4 * 255));
                paint1.setAlpha(alpha);
            } else {
                paint1.setAlpha(255);
            }
            paint1.setAntiAlias(true);

            Path path1 = new Path();
            path1.moveTo(scaleRoiRect.left, scaleRoiRect.top + lineHeight1 + lineHeightOffset1);
            path1.lineTo(scaleRoiRect.left, scaleRoiRect.top + quad_r1);
            path1.arcTo(scaleRoiRect.left, scaleRoiRect.top, scaleRoiRect.left + quad_r1 * 2, scaleRoiRect.top + quad_r1 * 2, 180, 90, false);
            path1.lineTo(scaleRoiRect.left + lineWidth1 + lineWidthOffset1, scaleRoiRect.top);
            canvas.drawPath(path1, paint1);

            Path path2 = new Path();
            path2.moveTo(scaleRoiRect.right, scaleRoiRect.top + lineHeight1 + lineHeightOffset1);
            path2.lineTo(scaleRoiRect.right, scaleRoiRect.top + quad_r1);
            path2.arcTo(scaleRoiRect.right - quad_r1 * 2, scaleRoiRect.top, scaleRoiRect.right, scaleRoiRect.top + quad_r1 * 2, 0, -90, false);
            path2.lineTo(scaleRoiRect.right - lineWidth1 - lineWidthOffset1, scaleRoiRect.top);
            canvas.drawPath(path2, paint1);

            Path path3 = new Path();
            path3.moveTo(scaleRoiRect.right, scaleRoiRect.bottom - lineHeight1 - lineHeightOffset1);
            path3.lineTo(scaleRoiRect.right, scaleRoiRect.bottom - quad_r1);
            path3.arcTo(scaleRoiRect.right - quad_r1 * 2, scaleRoiRect.bottom - quad_r1 * 2, scaleRoiRect.right, scaleRoiRect.bottom, 0, 90, false);
            path3.lineTo(scaleRoiRect.right - lineWidth1 - lineWidthOffset1, scaleRoiRect.bottom);
            canvas.drawPath(path3, paint1);

            Path path4 = new Path();
            path4.moveTo(scaleRoiRect.left, scaleRoiRect.bottom - lineHeight1 - lineHeightOffset1);
            path4.lineTo(scaleRoiRect.left, scaleRoiRect.bottom - quad_r1);
            path4.arcTo(scaleRoiRect.left, scaleRoiRect.bottom - quad_r1 * 2, scaleRoiRect.left + quad_r1 * 2, scaleRoiRect.bottom, 180, -90, false);
            path4.lineTo(scaleRoiRect.left + lineWidth1 + lineWidthOffset1, roiViewRect.bottom);
            canvas.drawPath(path4, paint1);
        }

        if((viewMode == VIEW_MODE.TO_FACE_CIRCLE && animateValue < 1.0f) || viewMode == VIEW_MODE.FACE_CIRCLE_TO_NO_FACE) {

            float start_width = 0.8f * roiViewRect.width() * 0.5f / (float)Math.cos(45 * Math.PI / 180);

            float center_x = roiViewRect.centerX();
            float center_y = roiViewRect.centerY();
            float left = center_x - (roiViewRect.width() / 2 * (1 - animateValue) + start_width * animateValue);
            float top = center_y - (roiViewRect.width() / 2 * (1 - animateValue) + start_width * animateValue);
            float right = center_x + (roiViewRect.width() / 2 * (1 - animateValue) + start_width * animateValue);
            float bottom = center_y + (roiViewRect.width() / 2 * (1 - animateValue) + start_width * animateValue);
            RectF eraseRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(eraseRect, eraseRect.width() / 2, eraseRect.height() / 2, eraserPaint);
        } else if(viewMode == VIEW_MODE.FACE_CIRCLE) {
            canvas.drawRoundRect(roiViewRect, roiViewRect.width() / 2, roiViewRect.height() / 2, eraserPaint);

            double centerX = roiViewRect.centerX();
            double centerY = roiViewRect.centerY();

            for(int i = 0; i < 360; i += 5) {

                double a1 = roiViewRect.width() / 2 + 10;
                double b1 = roiViewRect.height() / 2 + 10;
                double a2 = roiViewRect.width() / 2 + 40;
                double b2 = roiViewRect.height() / 2 + 40;

                double th = i * Math.PI / 180;
                double x1 = a1 * b1 / Math.sqrt(Math.pow(b1, 2) + Math.pow(a1, 2) * Math.tan(th) * Math.tan(th));
                double x2 = a2 * b2 / Math.sqrt(Math.pow(b2, 2) + Math.pow(a2, 2) * Math.tan(th) * Math.tan(th));
                double y1 = Math.sqrt(1 - (x1 / a1) * (x1 / a1)) * b1;
                double y2 = Math.sqrt(1 - (x1 / a1) * (x1 / a1)) * b2;

                if((i % 360) > 90 && (i % 360) < 270) {
                    x1 = -x1;
                    x2 = -x2;
                }

                if((i % 360) > 180 && (i % 360) < 360) {
                    y1 = -y1;
                    y2 = -y2;
                }

                canvas.drawLine((float)(centerX + x1), (float)(centerY - y1), (float)(centerX + x2), (float)(centerY - y2), outSideActiveRoundPaint);
            }

            if(faceBoxes != null && faceBoxes.size() > 0) {
                Paint paint1 = new Paint();
                paint1.setStyle(Paint.Style.FILL_AND_STROKE);
                paint1.setStrokeWidth(6);
                paint1.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_onPrimaryContainer));
                paint1.setAlpha(128);
                paint1.setAntiAlias(true);

                FaceBox faceBox = faceBoxes.get(0);
                double yaw = faceBox.yaw;
                double pitch = faceBox.pitch;

                Path path1 = new Path();
                path1.moveTo(roiViewRect.centerX(), roiViewRect.top);
                path1.quadTo(roiViewRect.centerX() - roiViewRect.width() * (float) Math.sin(yaw * Math.PI / 180), roiViewRect.centerY(), roiViewRect.centerX(), roiViewRect.bottom);
                path1.quadTo(roiViewRect.centerX() - roiViewRect.width() * (float) Math.sin(yaw * Math.PI / 180) / 3, roiViewRect.centerY(), roiViewRect.centerX(), roiViewRect.top);
                canvas.drawPath(path1, paint1);

                Path path2 = new Path();
                path2.moveTo(roiViewRect.left, roiViewRect.centerY());
                path2.quadTo(roiViewRect.centerX(), roiViewRect.centerY() + roiViewRect.width() * (float) Math.sin(pitch * Math.PI / 180), roiViewRect.right, roiViewRect.centerY());
                path2.quadTo(roiViewRect.centerX(), roiViewRect.centerY() + roiViewRect.width() * (float) Math.sin(pitch * Math.PI / 180) / 3, roiViewRect.left, roiViewRect.centerY());
                canvas.drawPath(path2, paint1);
            }
        } else if(viewMode == VIEW_MODE.FACE_CAPTURE_PREPARE) {

            RectF borderRect = new RectF(roiViewRect);
            CaptureView.scale(borderRect, 1.04f);
            Paint paint1 = new Paint();
            paint1.setStyle(Paint.Style.FILL);
            paint1.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_onTertiary));
            paint1.setAntiAlias(true);
            canvas.drawCircle(borderRect.centerX(), borderRect.centerY(), borderRect.width() / 2, paint1);

            RectF innerRect = new RectF(roiViewRect);
            CaptureView.scale(innerRect, 1.0f - animateValue);
            canvas.drawRoundRect(innerRect, innerRect.width() / 2, innerRect.height() / 2, eraserPaint);
        } else if(viewMode == VIEW_MODE.FACE_CAPTURE_DONE) {
            RectF borderRect = new RectF(roiViewRect);
            CaptureView.scale(borderRect, 0.8f);

            Rect roiViewRectSrc = new Rect();
            borderRect.round(roiViewRectSrc);

            Paint paint1 = new Paint();
            paint1.setStyle(Paint.Style.STROKE);
            paint1.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_onTertiary));
            paint1.setStrokeWidth(15);
            paint1.setAntiAlias(true);

            canvas.translate(0, (getWidth() / 5 - roiViewRect.top) * animateValue);
            canvas.drawBitmap(roiBitmap, new Rect(0, 0, roiBitmap.getWidth(), roiBitmap.getHeight()), borderRect, null);
            canvas.drawCircle(borderRect.centerX(), borderRect.centerY(), borderRect.width() / 2, paint1);
        }
    }

    private static void scale(RectF rect, float factor){
        float diffHorizontal = (rect.right-rect.left) * (factor-1f);
        float diffVertical = (rect.bottom-rect.top) * (factor-1f);

        rect.top -= diffVertical/2f;
        rect.bottom += diffVertical/2f;

        rect.left -= diffHorizontal/2f;
        rect.right += diffHorizontal/2f;
    }

    public static RectF getROIRect(Size frameSize) {
        int margin = frameSize.getWidth() / 6;
        int rectHeight = (frameSize.getWidth() - 2 * margin) * 6 / 5;

        RectF roiRect = new RectF(margin,  (frameSize.getHeight() - rectHeight) / 2,
                frameSize.getWidth() - margin, (frameSize.getHeight() - rectHeight) / 2 + rectHeight);
        return roiRect;
    }

    public static RectF getROIRect1(Size frameSize) {
        int margin = frameSize.getWidth() / 6;
        int rectHeight = (frameSize.getWidth() - 2 * margin);

        RectF roiRect = new RectF(margin,  (frameSize.getHeight() - rectHeight) / 2,
                frameSize.getWidth() - margin, (frameSize.getHeight() - rectHeight) / 2 + rectHeight);
        return roiRect;
    }
}
