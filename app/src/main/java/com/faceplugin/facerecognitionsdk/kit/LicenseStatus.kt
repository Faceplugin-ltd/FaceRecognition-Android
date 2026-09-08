package com.faceplugin.facerecognitionsdk.kit

import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK
import org.json.JSONObject

/** Parsed [FaceRecognitionSDK.getLicenseStatus] for UI and capability checks. */
data class LicenseStatus(
    val licensed: Boolean,
    val level: Int,
    val levelName: String,
    val recognition: Boolean,
    val liveness: Boolean,
    val label: String,
) {
    companion object {
        fun current(): LicenseStatus {
            return try {
                fromJson(FaceRecognitionSDK.getLicenseStatus())
            } catch (_: Throwable) {
                notLicensed()
            }
        }

        fun fromJson(json: String?): LicenseStatus {
            return try {
                val o = JSONObject(json ?: "{}")
                LicenseStatus(
                    licensed = o.optBoolean("licensed", false),
                    level = o.optInt("level", -1),
                    levelName = o.optString("levelName", "None"),
                    recognition = o.optBoolean("recognition", false),
                    liveness = o.optBoolean("liveness", false),
                    label = o.optString("label", "Not licensed"),
                )
            } catch (_: Exception) {
                notLicensed()
            }
        }

        private fun notLicensed() = LicenseStatus(
            licensed = false,
            level = -1,
            levelName = "None",
            recognition = false,
            liveness = false,
            label = "Not licensed",
        )
    }

    fun denyMessage(wantRecognition: Boolean, wantLiveness: Boolean): String? {
        val parts = mutableListOf<String>()
        if (wantLiveness && !liveness) {
            parts += "Liveness is not available on this license ($label)."
        }
        if (wantRecognition && !recognition) {
            parts += "Recognition is not available on this license ($label)."
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}
