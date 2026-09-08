package com.faceplugin.facerecognitionsdk.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/** Draws a face crop with 14-point landmark dots and index labels. */
class LandmarkImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val landmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00E5FF")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 9f * density
        isFakeBoldText = true
    }

    private var bitmap: Bitmap? = null
    private var landmarks: List<PointF> = emptyList()
    private val drawMatrix = Matrix()

    fun setContent(image: Bitmap?, points: List<PointF>) {
        bitmap = image
        landmarks = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        if (bmp.isRecycled || width <= 0 || height <= 0) return
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = min(width / bw, height / bh)
        val dx = (width - bw * scale) / 2f
        val dy = (height - bh * scale) / 2f
        drawMatrix.reset()
        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)
        canvas.drawBitmap(bmp, drawMatrix, bitmapPaint)

        val mapped = landmarks.map { PointF(it.x * scale + dx, it.y * scale + dy) }
        val r = max(3f * density, min(5f * density, (bw * scale) * 0.018f))
        mapped.forEachIndexed { index, pt ->
            canvas.drawCircle(pt.x, pt.y, r, landmarkPaint)
            canvas.drawText((index + 1).toString(), pt.x, pt.y - r - 1f, labelPaint)
        }
    }
}
