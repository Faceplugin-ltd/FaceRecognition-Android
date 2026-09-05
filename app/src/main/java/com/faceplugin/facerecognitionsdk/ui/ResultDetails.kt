package com.faceplugin.facerecognitionsdk.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.faceplugin.facerecognitionsdk.FaceBox
import org.json.JSONObject

object FaceBoxExtras {
    const val IDENTIFIED_FACE = "identified_face"
    const val ENROLLED_FACE = "enrolled_face"
    const val FACE_IMAGE = "face_image"
    const val IDENTIFIED_NAME = "identified_name"
    const val SIMILARITY = "similarity"
    const val YAW = "yaw"
    const val ROLL = "roll"
    const val PITCH = "pitch"
    const val LIVENESS = "liveness"
    const val FACE_QUALITY = "face_quality"
    const val FACE_LUMINANCE = "face_luminance"
    const val LEFT_EYE_CLOSED = "left_eye_closed"
    const val RIGHT_EYE_CLOSED = "right_eye_closed"
    const val FACE_OCCLUSION = "face_occlusion"
    const val MOUTH_OPENED = "mouth_opened"
    const val AGE = "age"
    const val GENDER = "gender"
    const val LANDMARK_COUNT = "landmark_count"
    const val LANDMARKS_XY = "landmarks_xy"
    const val CROP_LANDMARKS_XY = "crop_landmarks_xy"
    const val LIVENESS_LABEL = "liveness_label"
    const val GENDER_LABEL = "gender_label"
    const val EMOTION_LABEL = "emotion_label"
    const val MASK_LABEL = "mask_label"
    const val QUALITY_LABEL = "quality_label"
    const val EYES_LEFT_LABEL = "eyes_left_label"
    const val EYES_RIGHT_LABEL = "eyes_right_label"
    const val EXTRA_JSON = "extra_json"
    const val BOX_X1 = "box_x1"
    const val BOX_Y1 = "box_y1"
    const val BOX_X2 = "box_x2"
    const val BOX_Y2 = "box_y2"

    @JvmStatic
    fun putBox(intent: Intent, box: FaceBox) {
        intent.putExtra(YAW, box.yaw)
        intent.putExtra(ROLL, box.roll)
        intent.putExtra(PITCH, box.pitch)
        intent.putExtra(LIVENESS, box.liveness)
        intent.putExtra(FACE_QUALITY, box.face_quality)
        intent.putExtra(FACE_LUMINANCE, box.face_luminance)
        intent.putExtra(LEFT_EYE_CLOSED, box.left_eye_closed)
        intent.putExtra(RIGHT_EYE_CLOSED, box.right_eye_closed)
        intent.putExtra(FACE_OCCLUSION, box.face_occlusion)
        intent.putExtra(MOUTH_OPENED, box.mouth_opened)
        intent.putExtra(AGE, box.age)
        intent.putExtra(GENDER, box.gender)
        intent.putExtra(LANDMARK_COUNT, box.landmarkCount)
        val n = Math.max(0, Math.min(box.landmarkCount, box.landmarks_68.size / 2))
        intent.putExtra(LANDMARKS_XY, box.landmarks_68.copyOf(n * 2))
        intent.putExtra(LIVENESS_LABEL, box.livenessLabel)
        intent.putExtra(GENDER_LABEL, box.genderLabel)
        intent.putExtra(EMOTION_LABEL, box.emotionLabel)
        intent.putExtra(MASK_LABEL, box.maskLabel)
        intent.putExtra(QUALITY_LABEL, box.qualityLabel)
        intent.putExtra(EYES_LEFT_LABEL, box.eyesLeftLabel)
        intent.putExtra(EYES_RIGHT_LABEL, box.eyesRightLabel)
        intent.putExtra(BOX_X1, box.x1)
        intent.putExtra(BOX_Y1, box.y1)
        intent.putExtra(BOX_X2, box.x2)
        intent.putExtra(BOX_Y2, box.y2)
        val extra = JSONObject()
        for ((key, value) in box.extraAttributes) {
            if (value.length > 2000) continue
            extra.put(key, value)
        }
        intent.putExtra(EXTRA_JSON, extra.toString())
    }

    @JvmStatic
    fun putCropLandmarks(intent: Intent, xy: FloatArray?) {
        if (xy != null) intent.putExtra(CROP_LANDMARKS_XY, xy)
    }

    fun cropLandmarkPoints(intent: Intent): List<PointF> {
        val xy = intent.getFloatArrayExtra(CROP_LANDMARKS_XY) ?: return emptyList()
        return (0 until xy.size / 2).map { PointF(xy[it * 2], xy[it * 2 + 1]) }
    }

    fun extraMap(intent: Intent): Map<String, String> {
        val raw = intent.getStringExtra(EXTRA_JSON) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, obj.optString(key))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

object ResultDetails {
    const val SECTION = "__section__"

    @JvmStatic
    fun qualityText(score: Float): String = when {
        score < 0.5f -> "Low · ${(score * 100).toInt()}%"
        score < 0.75f -> "Medium · ${(score * 100).toInt()}%"
        else -> "High · ${(score * 100).toInt()}%"
    }

    fun livenessText(score: Float, threshold: Float, label: String?): String {
        val lower = label.orEmpty().lowercase()
        val live = if (lower.contains("spoof") || lower.contains("fake")) {
            "Spoof"
        } else if (lower.contains("real")) {
            "Real"
        } else if (score >= threshold) {
            "Real"
        } else {
            "Spoof"
        }
        val already = label.orEmpty()
        if (already.contains(" · ")) return already
        return "$live · ${(score * 100).toInt()}%"
    }

    fun genderText(gender: Int, label: String?): String {
        if (!label.isNullOrBlank()) return label
        return when (gender) {
            0 -> "Male"
            1 -> "Female"
            else -> "Unknown"
        }
    }

    private fun engineAttr(extra: Map<String, String>, vararg keys: String): String {
        for (key in keys) {
            extra[key]?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
    }

    private fun addIf(rows: MutableList<Pair<String, String>>, title: String, value: String?) {
        if (!value.isNullOrBlank()) rows += title to value
    }

    fun rows(context: Context, intent: Intent, includeMatch: Boolean): List<Pair<String, String>> {
        val yaw = intent.getFloatExtra(FaceBoxExtras.YAW, 0f)
        val roll = intent.getFloatExtra(FaceBoxExtras.ROLL, 0f)
        val pitch = intent.getFloatExtra(FaceBoxExtras.PITCH, 0f)
        val liveness = intent.getFloatExtra(FaceBoxExtras.LIVENESS, 0f)
        val quality = intent.getFloatExtra(FaceBoxExtras.FACE_QUALITY, 0f)
        val luminance = intent.getFloatExtra(FaceBoxExtras.FACE_LUMINANCE, 0f)
        val age = intent.getIntExtra(FaceBoxExtras.AGE, 0)
        val gender = intent.getIntExtra(FaceBoxExtras.GENDER, 0)
        val x1 = intent.getIntExtra(FaceBoxExtras.BOX_X1, 0)
        val y1 = intent.getIntExtra(FaceBoxExtras.BOX_Y1, 0)
        val x2 = intent.getIntExtra(FaceBoxExtras.BOX_X2, 0)
        val y2 = intent.getIntExtra(FaceBoxExtras.BOX_Y2, 0)
        val extra = FaceBoxExtras.extraMap(intent)
        val used = mutableSetOf<String>()
        val rows = mutableListOf<Pair<String, String>>()

        fun take(title: String, vararg keys: String, fallback: String? = null) {
            val value = engineAttr(extra, *keys).ifBlank { fallback.orEmpty() }
            if (value.isNotBlank()) {
                rows += title to value
                keys.forEach { used += it }
            }
        }

        if (includeMatch) {
            val name = intent.getStringExtra(FaceBoxExtras.IDENTIFIED_NAME).orEmpty()
            val similarity = intent.getFloatExtra(FaceBoxExtras.SIMILARITY, 0f)
            rows += SECTION to "Match"
            rows += "Person" to name
            rows += "Similarity" to "${(similarity * 100).toInt()}%"
        }

        rows += SECTION to "Authenticity"
        take(
            "Liveness",
            "Liveness2D",
            fallback = livenessText(
                liveness,
                SettingsActivity.getLivenessThreshold(context),
                intent.getStringExtra(FaceBoxExtras.LIVENESS_LABEL),
            ),
        )
        rows += SECTION to "Person"
        take("Age", "Age", fallback = if (age > 0) age.toString() else null)
        take("Gender", "Gender", fallback = genderText(gender, intent.getStringExtra(FaceBoxExtras.GENDER_LABEL)))
        take("Emotion", "Emotion", fallback = intent.getStringExtra(FaceBoxExtras.EMOTION_LABEL))
        take("All emotions", "Emotions")

        rows += SECTION to "Face"
        take("Mask", "MedicalMask", "Mask", fallback = intent.getStringExtra(FaceBoxExtras.MASK_LABEL))
        take("Glasses", "Glasses")
        take("Sunglasses", "Sunglasses")
        val leftText = engineAttr(extra, "EyesLeft").ifBlank {
            intent.getStringExtra(FaceBoxExtras.EYES_LEFT_LABEL).orEmpty()
        }
        val rightText = engineAttr(extra, "EyesRight").ifBlank {
            intent.getStringExtra(FaceBoxExtras.EYES_RIGHT_LABEL).orEmpty()
        }
        if (leftText.isNotBlank() || rightText.isNotBlank()) {
            rows += "Eyes" to "Left  ${leftText.ifBlank { "—" }}\nRight  ${rightText.ifBlank { "—" }}"
            used += "EyesLeft"
            used += "EyesRight"
        }

        rows += SECTION to "Quality"
        val qualityLabel = extra["FaceQuality"] ?: intent.getStringExtra(FaceBoxExtras.QUALITY_LABEL)
        addIf(rows, "Overall", qualityLabel?.ifBlank { null } ?: if (quality > 0f) qualityText(quality) else null)
        used += "FaceQuality"
        for (key in listOf("Lighting", "Sharpness", "Noise", "Flare", "BlurLevel", "NoiseLevel")) {
            take(key, key)
        }

        rows += SECTION to "Geometry"
        rows += "Pose" to "yaw ${yaw}°   roll ${roll}°   pitch ${pitch}°"
        rows += "Box" to "$x1, $y1 → $x2, $y2"
        if (luminance > 0f) rows += "Luminance" to "${(luminance * 100).toInt()}%"

        val leftovers = extra.filter { (key, value) ->
            key !in used && value.isNotBlank() && key !in setOf("MouthOpened", "Deepfake", "Template")
        }
        if (leftovers.isNotEmpty()) {
            rows += SECTION to "More from engine"
            for ((key, value) in leftovers) {
                if (rows.none { it.first.equals(key, ignoreCase = true) }) {
                    rows += key to value
                }
            }
        }

        val marks = landmarkPositions(intent)
        if (marks.isNotBlank()) {
            rows += SECTION to "Landmarks"
            val count = intent.getIntExtra(FaceBoxExtras.LANDMARK_COUNT, 0)
            rows += "Count" to if (count > 0) "$count points" else ""
            rows += "Positions" to marks
        }
        return rows.filterIndexed { i, row ->
            if (row.first != SECTION) {
                row.second.isNotBlank()
            } else {
                val next = rows.getOrNull(i + 1)
                next != null && next.first != SECTION && next.second.isNotBlank()
            }
        }
    }

    fun landmarkPositions(intent: Intent): String {
        val xy = intent.getFloatArrayExtra(FaceBoxExtras.LANDMARKS_XY) ?: return ""
        val n = xy.size / 2
        if (n == 0) return ""
        return buildString {
            for (i in 0 until n) {
                if (i > 0) append('\n')
                append("${i + 1}: ${xy[i * 2]}, ${xy[i * 2 + 1]}")
            }
        }
    }

    fun bind(container: LinearLayout, rows: List<Pair<String, String>>) {
        container.removeAllViews()
        val ctx = container.context
        val pad = (16 * ctx.resources.displayMetrics.density).toInt()
        val gap = (10 * ctx.resources.displayMetrics.density).toInt()
        for ((title, value) in rows) {
            if (title == SECTION) {
                val header = TextView(ctx).apply {
                    text = value
                    textSize = 13f
                    letterSpacing = 0.06f
                    setTextColor(Color.parseColor("#D0BCFF"))
                    setPadding(pad, gap * 2, pad, gap / 2)
                    setAllCaps(true)
                }
                container.addView(
                    header,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
                continue
            }
            val label = TextView(ctx).apply {
                text = title
                textSize = 13f
                setTextColor(Color.parseColor("#938F99"))
                setPadding(pad, gap, pad, 0)
            }
            val body = TextView(ctx).apply {
                text = value
                textSize = 16f
                setTextColor(Color.parseColor("#E6E1E5"))
                setPadding(pad, 0, pad, gap)
                setTextIsSelectable(true)
            }
            container.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            container.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
