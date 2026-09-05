package com.faceplugin.facerecognitionsdk.kit

import android.graphics.PointF
import android.graphics.RectF
import android.util.Base64
import com.faceplugin.facerecognitionsdk.FaceBox
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

object FaceJson {
    fun parseDetect(json: String?): List<DetectedFace> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val root = JSONObject(json)
            val faces = root.optJSONArray("data") ?: return emptyList()
            buildList {
                for (i in 0 until faces.length()) {
                    val face = faces.optJSONObject(i) ?: continue
                    val regionObj = face.optJSONObject("faceRegion") ?: continue
                    val x = doubleValue(regionObj.opt("x")) ?: continue
                    val y = doubleValue(regionObj.opt("y")) ?: continue
                    val w = doubleValue(regionObj.opt("width")) ?: continue
                    val h = doubleValue(regionObj.opt("height")) ?: continue
                    val pose = face.optJSONObject("facePose")
                    val points = face.optJSONArray("facePoints")
                    val landmarks = parseLandmarks(points)
                    add(
                        DetectedFace(
                            faceId = intValue(face.opt("faceId")) ?: 0,
                            region = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat()),
                            attributes = parseAttributes(face.optJSONObject("attributes")),
                            yaw = doubleValue(pose?.opt("yaw")) ?: 0.0,
                            pitch = doubleValue(pose?.opt("pitch")) ?: 0.0,
                            roll = doubleValue(pose?.opt("roll")) ?: 0.0,
                            landmarkCount = landmarks.size,
                            landmarks = landmarks,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @JvmStatic
    fun parseVideoWorkerEvent(json: String?): VideoWorkerEvent? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            when (root.optString("event")) {
                "tracking" -> {
                    val faces = parseVideoWorkerFaces(root.optJSONArray("faces"))
                    val fw = doubleValue(root.opt("frame_width")) ?: 0.0
                    val fh = doubleValue(root.opt("frame_height")) ?: 0.0
                    val (w, h) = videoWorkerFrameSize(fw.toInt(), fh.toInt())
                    VideoWorkerEvent.Tracking(
                        frameId = intValue(root.opt("frame_id")) ?: 0,
                        faces = faces,
                        singleFace = root.optBoolean("single_face", faces.size == 1),
                        frameWidth = w,
                        frameHeight = h,
                    )
                }
                "match" -> VideoWorkerEvent.Match(
                    trackId = intValue(root.opt("track_id")) ?: 0,
                    matched = root.optBoolean("matched", false),
                    personIndex = intValue(root.opt("person_index")),
                    score = doubleValue(root.opt("score")),
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseQualityAttributes(json: String?): Map<String, FaceAttribute> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(json)
            val items = root.optJSONArray("data") ?: return emptyMap()
            val first = items.optJSONObject(0) ?: return emptyMap()
            parseAttributes(first.optJSONObject("attributes"))
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun parseFeatureData(json: String?): ByteArray? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val results = root.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val features = results.optJSONObject(0)?.optJSONArray("features")
                val b64 = features?.optJSONObject(0)?.optString("feature")
                if (!b64.isNullOrBlank()) return Base64.decode(b64, Base64.DEFAULT)
            }
            val features = root.optJSONArray("features")
            val b64 = features?.optJSONObject(0)?.optString("feature")
            if (!b64.isNullOrBlank()) Base64.decode(b64, Base64.DEFAULT) else null
        } catch (_: Exception) {
            null
        }
    }

    fun videoWorkerFrameSize(bufferWidth: Int, bufferHeight: Int): Pair<Float, Float> {
        if (bufferWidth <= 0 || bufferHeight <= 0) return 0f to 0f
        return bufferWidth.toFloat() to bufferHeight.toFloat()
    }

    @JvmStatic
    @JvmOverloads
    fun toFaceBoxes(faces: List<VideoWorkerFace>, includeWeak: Boolean = false): List<FaceBox> =
        faces.filter { includeWeak || !it.weak }.map { toFaceBox(it) }

    @JvmStatic
    fun toFaceBox(face: VideoWorkerFace): FaceBox {
        val box = FaceBox()
        box.x1 = face.region.left.roundToInt()
        box.y1 = face.region.top.roundToInt()
        box.x2 = face.region.right.roundToInt()
        box.y2 = face.region.bottom.roundToInt()
        box.yaw = face.yaw.toFloat()
        box.pitch = face.pitch.toFloat()
        box.roll = face.roll.toFloat()
        val n = min(face.landmarks.size, box.landmarks_68.size / 2)
        box.landmarkCount = n
        for (i in 0 until n) {
            box.landmarks_68[i * 2] = face.landmarks[i].x
            box.landmarks_68[i * 2 + 1] = face.landmarks[i].y
        }
        return box
    }

    private fun parseVideoWorkerFaces(faces: JSONArray?): List<VideoWorkerFace> {
        if (faces == null) return emptyList()
        return buildList {
            for (i in 0 until faces.length()) {
                val face = faces.optJSONObject(i) ?: continue
                val regionObj = face.optJSONObject("faceRegion") ?: continue
                val x = doubleValue(regionObj.opt("x")) ?: continue
                val y = doubleValue(regionObj.opt("y")) ?: continue
                val w = doubleValue(regionObj.opt("width")) ?: continue
                val h = doubleValue(regionObj.opt("height")) ?: continue
                val matchRaw = face.optJSONObject("match")
                val match = if (matchRaw != null) {
                    VideoWorkerMatch(
                        matched = matchRaw.optBoolean("matched", false),
                        personIndex = intValue(matchRaw.opt("person_index")),
                        score = doubleValue(matchRaw.opt("score")),
                    )
                } else null
                val pose = face.optJSONObject("facePose")
                add(
                    VideoWorkerFace(
                        trackId = intValue(face.opt("track_id")) ?: 0,
                        region = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat()),
                        landmarks = parseLandmarks(face.optJSONArray("facePoints")),
                        weak = face.optBoolean("weak", false),
                        match = match,
                        age = doubleValue(face.opt("age")),
                        gender = face.optString("gender").takeIf { it.isNotBlank() },
                        emotion = face.optString("emotion").takeIf { it.isNotBlank() },
                        activeLiveness = parseActiveLiveness(face.optJSONObject("activeLiveness")),
                        yaw = doubleValue(pose?.opt("yaw")) ?: 0.0,
                        pitch = doubleValue(pose?.opt("pitch")) ?: 0.0,
                        roll = doubleValue(pose?.opt("roll")) ?: 0.0,
                    ),
                )
            }
        }
    }

    private fun parseActiveLiveness(raw: JSONObject?): VideoWorkerActiveLiveness? {
        if (raw == null) return null
        return VideoWorkerActiveLiveness(
            verdict = raw.optString("verdict", "not_computed"),
            checkType = raw.optString("checkType", "none"),
            progress = doubleValue(raw.opt("progress")) ?: 0.0,
        )
    }

    private fun parseLandmarks(raw: JSONArray?): List<PointF> {
        if (raw == null) return emptyList()
        return buildList {
            for (i in 0 until raw.length()) {
                val point = raw.optJSONObject(i) ?: continue
                val x = doubleValue(point.opt("x")) ?: continue
                val y = doubleValue(point.opt("y")) ?: continue
                add(PointF(x.toFloat(), y.toFloat()))
            }
        }
    }

    private fun parseAttributes(raw: JSONObject?): Map<String, FaceAttribute> {
        if (raw == null) return emptyMap()
        val out = mutableMapOf<String, FaceAttribute>()
        val keys = raw.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = raw.opt(key) ?: continue
            if (value is JSONObject) {
                val valStr = formatAttributeValue(value.opt("value"))
                val conf = value.opt("confidence")?.let { formatAttributeValue(it) }
                if (valStr.isNotEmpty()) out[key] = FaceAttribute(valStr, conf)
            } else {
                val valStr = formatAttributeValue(value)
                if (valStr.isNotEmpty()) out[key] = FaceAttribute(valStr, null)
            }
        }
        return out
    }

    private fun formatAttributeValue(value: Any?): String {
        if (value == null || value === JSONObject.NULL) return ""
        return when (value) {
            is Number -> {
                val d = value.toDouble()
                if (abs(d - round(d)) < 0.001) round(d).toInt().toString() else "%.2f".format(d)
            }
            else -> value.toString()
        }
    }

    private fun doubleValue(value: Any?): Double? = when (value) {
        null, JSONObject.NULL -> null
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun intValue(value: Any?): Int? = when (value) {
        null, JSONObject.NULL -> null
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
}
