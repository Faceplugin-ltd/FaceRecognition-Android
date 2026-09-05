package com.faceplugin.facerecognitionsdk.kit

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF

data class FaceAttribute(
    val value: String,
    val confidence: String? = null,
)

data class DetectedFace(
    val faceId: Int,
    val region: RectF,
    val attributes: Map<String, FaceAttribute>,
    val yaw: Double,
    val pitch: Double,
    val roll: Double,
    val landmarkCount: Int,
    val landmarks: List<PointF>,
) {
    fun mergingAttributes(extra: Map<String, FaceAttribute>): DetectedFace {
        val merged = attributes.toMutableMap()
        for ((key, value) in extra) {
            if (!merged.containsKey(key)) merged[key] = value
        }
        return copy(attributes = merged)
    }
}

data class VideoWorkerActiveLiveness(
    val verdict: String,
    val checkType: String,
    val progress: Double,
)

data class VideoWorkerMatch(
    val matched: Boolean,
    val personIndex: Int?,
    val score: Double?,
)

data class VideoWorkerFace(
    val trackId: Int,
    val region: RectF,
    val landmarks: List<PointF>,
    val weak: Boolean,
    val match: VideoWorkerMatch?,
    val age: Double?,
    val gender: String?,
    val emotion: String?,
    val activeLiveness: VideoWorkerActiveLiveness?,
    val yaw: Double = 0.0,
    val pitch: Double = 0.0,
    val roll: Double = 0.0,
)

sealed class VideoWorkerEvent {
    data class Tracking(
        val frameId: Int,
        val faces: List<VideoWorkerFace>,
        val singleFace: Boolean,
        val frameWidth: Float,
        val frameHeight: Float,
    ) : VideoWorkerEvent()

    data class Match(
        val trackId: Int,
        val matched: Boolean,
        val personIndex: Int?,
        val score: Double?,
    ) : VideoWorkerEvent()
}

data class GalleryFaceCandidate(
    val crop: Bitmap,
    val region: RectF,
)

data class EnrolledPerson(
    val id: String,
    var name: String,
    val featureBase64: String,
    val thumbnailFile: String?,
    val createdAt: Long,
)

data class BestMatch(
    val person: EnrolledPerson,
    val score: Float,
)

data class Passive2DVerdict(
    val passed: Boolean,
    val label: String,
    val score: Float?,
)
