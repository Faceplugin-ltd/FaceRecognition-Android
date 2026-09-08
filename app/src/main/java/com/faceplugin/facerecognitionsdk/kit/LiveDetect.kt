package com.faceplugin.facerecognitionsdk.kit

import com.faceplugin.facerecognitionsdk.FaceBox
import com.faceplugin.facerecognitionsdk.FaceDetectionParam
import kotlin.math.max
import kotlin.math.min

/** Narrow still-image processing-block detects used next to VideoWorker. */
object LiveDetect {
    @JvmStatic
    fun livenessOnly(level: Int): FaceDetectionParam {
        val p = FaceDetectionParam()
        p.check_pose = false
        p.check_landmarks = false
        p.check_liveness = true
        p.check_liveness_level = level
        return p
    }

    @JvmStatic
    fun eyesOnly(): FaceDetectionParam {
        val p = FaceDetectionParam()
        p.check_pose = false
        p.check_landmarks = false
        p.check_eye_closeness = true
        return p
    }

    @JvmStatic
    fun mergeLiveness(track: List<FaceBox>, pb: List<FaceBox>?) {
        if (pb.isNullOrEmpty()) return
        for (dst in track) {
            val src = bestMatch(dst, pb) ?: continue
            dst.liveness = src.liveness
            dst.livenessLabel = src.livenessLabel
        }
    }

    @JvmStatic
    @JvmOverloads
    fun mergeEyes(track: List<FaceBox>, pb: List<FaceBox>?, swapLeftRight: Boolean = false) {
        if (pb.isNullOrEmpty()) return
        for (dst in track) {
            val src = bestMatch(dst, pb) ?: continue
            if (swapLeftRight) {
                dst.eyesLeftLabel = src.eyesRightLabel
                dst.eyesRightLabel = src.eyesLeftLabel
                dst.left_eye_closed = src.right_eye_closed
                dst.right_eye_closed = src.left_eye_closed
            } else {
                dst.eyesLeftLabel = src.eyesLeftLabel
                dst.eyesRightLabel = src.eyesRightLabel
                dst.left_eye_closed = src.left_eye_closed
                dst.right_eye_closed = src.right_eye_closed
            }
        }
    }

    private fun bestMatch(dst: FaceBox, pb: List<FaceBox>): FaceBox? {
        if (pb.size == 1) return pb[0]
        var best: FaceBox? = null
        var bestIou = 0.1f
        for (src in pb) {
            val iou = iou(dst, src)
            if (iou > bestIou) {
                bestIou = iou
                best = src
            }
        }
        return best ?: pb[0]
    }

    private fun iou(a: FaceBox, b: FaceBox): Float {
        val left = max(a.x1, b.x1)
        val top = max(a.y1, b.y1)
        val right = min(a.x2, b.x2)
        val bottom = min(a.y2, b.y2)
        val inter = max(0, right - left) * max(0, bottom - top)
        val areaA = max(0, a.x2 - a.x1) * max(0, a.y2 - a.y1)
        val areaB = max(0, b.x2 - b.x1) * max(0, b.y2 - b.y1)
        val union = areaA + areaB - inter
        if (union <= 0) return 0f
        return inter.toFloat() / union
    }
}
