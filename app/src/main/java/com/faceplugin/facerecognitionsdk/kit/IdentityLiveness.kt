package com.faceplugin.facerecognitionsdk.kit

import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK

enum class IdentityLivenessType(val key: String, val title: String) {
    ACTIVE("active", "Active"),
    PASSIVE("passive", "Passive"),
    PASSIVE_BLINK("passiveBlink", "Passive + blink"),
    ;

    companion object {
        fun fromKey(raw: String?): IdentityLivenessType =
            entries.firstOrNull { it.key == raw } ?: ACTIVE
    }
}

enum class ActiveLivenessCheckKind(val key: String, val title: String, val sdkCheck: Int) {
    SMILE("smile", "Smile", FaceRecognitionSDK.AL_CHECK_SMILE),
    BLINK("blink", "Close your eyes", FaceRecognitionSDK.AL_CHECK_BLINK),
    TURN_UP("turnUp", "Look up", FaceRecognitionSDK.AL_CHECK_TURN_UP),
    TURN_DOWN("turnDown", "Look down", FaceRecognitionSDK.AL_CHECK_TURN_DOWN),
    TURN_RIGHT("turnRight", "Turn right", FaceRecognitionSDK.AL_CHECK_TURN_RIGHT),
    TURN_LEFT("turnLeft", "Turn left", FaceRecognitionSDK.AL_CHECK_TURN_LEFT),
    PERSPECTIVE("perspective", "Move closer / farther", FaceRecognitionSDK.AL_CHECK_PERSPECTIVE),
    ;

    val defaultPrompt: String get() = title

    companion object {
        fun fromKey(raw: String?): ActiveLivenessCheckKind? =
            entries.firstOrNull { it.key == raw }

        fun fromSdkCheckType(checkType: String): ActiveLivenessCheckKind? = when (checkType) {
            "smile" -> SMILE
            "blink" -> BLINK
            "turn_up" -> TURN_UP
            "turn_down" -> TURN_DOWN
            "turn_right" -> TURN_RIGHT
            "turn_left" -> TURN_LEFT
            "perspective" -> PERSPECTIVE
            else -> null
        }

        fun mirrored(kind: ActiveLivenessCheckKind): ActiveLivenessCheckKind = when (kind) {
            TURN_LEFT -> TURN_RIGHT
            TURN_RIGHT -> TURN_LEFT
            else -> kind
        }
    }
}

data class ActiveLivenessParams(
    val smileThreshold: Float = 0.9f,
    val blinksThreshold: Float = 0.3f,
    val blinksNumber: Int = 2,
    val yawThreshold: Float = 40f,
    val pitchThreshold: Float = 20f,
    val perspectiveThreshold: Float = 0.1f,
    val faceAlignAngle: Float = 10f,
    val maxFramesWait: Int = 200,
    val checkCount: Int = 3,
) {
    companion object {
        val DEFAULT = ActiveLivenessParams()
    }
}

object IdentityLiveness {
    val defaultChecksOrder: List<ActiveLivenessCheckKind> = listOf(
        ActiveLivenessCheckKind.SMILE,
        ActiveLivenessCheckKind.BLINK,
        ActiveLivenessCheckKind.TURN_LEFT,
    )

    fun makeTrackingConfig(matchThreshold: Float): FaceRecognitionSDK.VideoWorkerConfig {
        val config = FaceRecognitionSDK.VideoWorkerConfig.withMatchThreshold(matchThreshold)
        val al = FaceRecognitionSDK.ActiveLivenessConfig.defaults()
        al.enabled = false
        config.activeLiveness = al
        return config
    }

    fun makeIdentityConfig(
        matchThreshold: Float,
        livenessType: IdentityLivenessType,
        checks: List<ActiveLivenessCheckKind>,
        params: ActiveLivenessParams = ActiveLivenessParams.DEFAULT,
        @Suppress("UNUSED_PARAMETER") frontCamera: Boolean = false,
    ): FaceRecognitionSDK.VideoWorkerConfig {
        val config = FaceRecognitionSDK.VideoWorkerConfig.withMatchThreshold(matchThreshold)
        val al = FaceRecognitionSDK.ActiveLivenessConfig.defaults()
        al.enabled = true
        val resolved = when (livenessType) {
            IdentityLivenessType.PASSIVE_BLINK -> listOf(ActiveLivenessCheckKind.BLINK)
            IdentityLivenessType.ACTIVE -> if (checks.isEmpty()) defaultChecksOrder else checks
            IdentityLivenessType.PASSIVE -> emptyList()
        }
        // iOS mirrors BGRA into the engine for front camera, so it also swaps L/R checks.
        // Android feeds an unmirrored upright bitmap and only mirrors overlay drawing —
        // do not swap checks here or "Turn left/right" prompts invert vs head motion.
        // `frontCamera` is kept for API parity with iOS / FaceRecognitionClient.
        val engineChecks = resolved
        al.checks = engineChecks.map { it.sdkCheck }.toIntArray()
        al.smileThreshold = params.smileThreshold
        al.blinksThreshold = params.blinksThreshold
        al.blinksNumber = params.blinksNumber
        al.yawThreshold = params.yawThreshold
        al.pitchThreshold = params.pitchThreshold
        al.perspectiveThreshold = params.perspectiveThreshold
        al.faceAlignAngle = params.faceAlignAngle
        al.maxFramesWait = params.maxFramesWait
        al.checkCount = maxOf(params.checkCount, engineChecks.size)
        config.activeLiveness = al
        return config
    }

    fun instruction(
        checkType: String,
        verdict: String,
        prompts: Map<ActiveLivenessCheckKind, String> = emptyMap(),
        @Suppress("UNUSED_PARAMETER") frontCamera: Boolean = false,
    ): String {
        return when (verdict) {
            "waiting_face_align" -> "Center your face"
            "check_fail" -> "Try again"
            "all_checks_passed" -> "Liveness verified"
            else -> {
                // Same as config: Android frames are not mirrored into the SDK, so show
                // the engine checkType as-is (do not remap L/R for front camera).
                val kind = ActiveLivenessCheckKind.fromSdkCheckType(checkType)
                if (kind != null) prompts[kind] ?: kind.defaultPrompt else "Follow the prompt"
            }
        }
    }

    fun passive2DVerdict(attribute: FaceAttribute?, threshold: Float): Passive2DVerdict {
        val raw = (attribute?.value ?: "").trim()
        val lower = raw.lowercase()
        val score = attribute?.confidence?.toFloatOrNull()

        if (lower.contains("spoof") || lower.contains("fake")) {
            val label = score?.let { "Spoof (%.2f)".format(it) } ?: "Spoof"
            return Passive2DVerdict(false, label, score)
        }
        if (lower.contains("real")) {
            if (score != null && score + 0.0001f >= threshold) {
                return Passive2DVerdict(true, "Real (%.2f)".format(score), score)
            }
            if (score != null) {
                return Passive2DVerdict(false, "Below threshold (%.2f)".format(score), score)
            }
            return Passive2DVerdict(false, "Below threshold", null)
        }
        if (raw.isEmpty()) {
            return Passive2DVerdict(false, "Checking liveness…", score)
        }
        return Passive2DVerdict(false, raw, score)
    }
}
