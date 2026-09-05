package com.faceplugin.facerecognitionsdk.kit

import android.content.Context
import android.graphics.Bitmap
import com.faceplugin.facerecognitionsdk.FaceBox
import com.faceplugin.facerecognitionsdk.FaceDetectionParam
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK

/**
 * Single entry point for integrators. Prefer this over calling [FaceRecognitionSDK] directly.
 */
class FaceRecognitionClient private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = FaceDatabase.get(appContext)

    val isEngineReady: Boolean get() = engineReady

    fun activate(license: String, completion: (Int) -> Unit) {
        FaceRecognitionQueue.async {
            if (engineReady) {
                completion(FaceRecognitionSDK.SDK_SUCCESS)
                return@async
            }
            try {
                FaceRecognitionSDK.getMachineCode(appContext)
                var ret = FaceRecognitionSDK.setActivation(appContext, license)
                if (ret == FaceRecognitionSDK.SDK_SUCCESS) {
                    ret = FaceRecognitionSDK.init(appContext)
                }
                if (ret == FaceRecognitionSDK.SDK_SUCCESS) {
                    engineReady = true
                }
                completion(ret)
            } catch (_: Throwable) {
                completion(FaceRecognitionSDK.SDK_INIT_FAILED)
            }
        }
    }

    fun deactivate() {
        FaceRecognitionQueue.sync {
            try {
                FaceRecognitionSDK.deinit()
            } catch (_: Throwable) {
            }
            engineReady = false
        }
    }

    fun detect(bitmap: Bitmap, crop: Boolean = false, flags: Int = FaceRecognitionSDK.DETECT_ALL): String? =
        FaceRecognitionQueue.detect(bitmap, crop, flags)

    fun faceDetection(bitmap: Bitmap, param: FaceDetectionParam? = null): List<FaceBox> =
        FaceRecognitionQueue.faceDetection(bitmap, param)

    fun templateExtraction(bitmap: Bitmap, face: FaceBox): ByteArray? =
        FaceRecognitionQueue.templateExtraction(bitmap, face)

    fun setLandmarkMode(mode: Int): Int = FaceRecognitionQueue.setLandmarkMode(mode)

    fun getLandmarkMode(): Int = FaceRecognitionQueue.getLandmarkMode()

    fun extractFeature(bitmap: Bitmap): ByteArray? {
        val json = FaceRecognitionQueue.extractFeature(bitmap) ?: return null
        return FaceJson.parseFeatureData(json)
    }

    fun similarity(feature1: ByteArray, feature2: ByteArray): Float =
        FaceRecognitionQueue.similarity(feature1, feature2)

    fun quality(bitmap: Bitmap, crop: Boolean = false): String? =
        FaceRecognitionQueue.quality(bitmap, crop)

    fun startVideoWorker(config: FaceRecognitionSDK.VideoWorkerConfig): Int =
        FaceRecognitionQueue.startVideoWorker(config)

    fun setVideoWorkerEventHandler(handler: ((String) -> Unit)?) {
        FaceRecognitionQueue.setVideoWorkerEventHandler(handler)
    }

    @JvmName("setVideoWorkerEventHandler")
    fun setVideoWorkerEventHandler(handler: java.util.function.Consumer<String>?) {
        if (handler == null) {
            FaceRecognitionQueue.setVideoWorkerEventHandler(null)
        } else {
            FaceRecognitionQueue.setVideoWorkerEventHandler { handler.accept(it) }
        }
    }

    fun syncDatabase(matchThreshold: Float): Int {
        val features = database.featureTemplates()
        return FaceRecognitionQueue.syncVideoWorkerDatabase(features, matchThreshold)
    }

    fun addFrame(bitmap: Bitmap): Int = FaceRecognitionQueue.addVideoWorkerFrame(bitmap)

    fun stopVideoWorker() {
        FaceRecognitionQueue.stopVideoWorker()
    }

    fun makeTrackingConfig(matchThreshold: Float): FaceRecognitionSDK.VideoWorkerConfig =
        IdentityLiveness.makeTrackingConfig(matchThreshold)

    fun makeIdentityConfig(
        matchThreshold: Float,
        livenessType: IdentityLivenessType,
        checks: List<ActiveLivenessCheckKind>,
        params: ActiveLivenessParams = ActiveLivenessParams.DEFAULT,
        frontCamera: Boolean = false,
    ): FaceRecognitionSDK.VideoWorkerConfig = IdentityLiveness.makeIdentityConfig(
        matchThreshold = matchThreshold,
        livenessType = livenessType,
        checks = checks,
        params = params,
        frontCamera = frontCamera,
    )

    val enrolledCount: Int get() = database.count
    val isEnrollmentEmpty: Boolean get() = database.isEmpty

    fun loadDatabase() {
        database.load()
    }

    fun enrolledPeople(): List<EnrolledPerson> = database.people

    fun enroll(name: String, feature: ByteArray, thumbnail: Bitmap?): EnrolledPerson? =
        database.add(name, feature, thumbnail)

    fun removeEnrolled(ids: Set<String>) {
        database.remove(ids)
    }

    fun clearEnrolled() {
        database.clear()
    }

    fun bestMatch(feature: ByteArray, threshold: Float): BestMatch? =
        database.bestMatch(feature, threshold)

    fun personAtVideoWorkerIndex(index: Int): EnrolledPerson? =
        database.personAtVideoWorkerIndex(index)

    fun thumbnail(person: EnrolledPerson): Bitmap? = database.thumbnail(person)

    fun galleryCandidates(
        image: Bitmap,
        matchThreshold: Float,
    ): Pair<List<GalleryFaceCandidate>, Int> {
        val prepared = CameraFrameUtils.enginePreparedImage(image)
        val detectJSON = FaceRecognitionQueue.detect(prepared, false, FaceRecognitionSDK.DETECT_LANDMARKS)
        val faces = FaceJson.parseDetect(detectJSON)
            .sortedByDescending { it.region.width() * it.region.height() }
        val candidates = faces.mapNotNull { face ->
            val crop = CameraFrameUtils.cropFace(prepared, face.region) ?: return@mapNotNull null
            val feature = extractFeature(crop)
            if (feature != null && bestMatch(feature, matchThreshold) != null) {
                return@mapNotNull null
            }
            GalleryFaceCandidate(crop, face.region)
        }
        return candidates to faces.size
    }

    fun passive2DVerdict(attribute: FaceAttribute?, threshold: Float): Passive2DVerdict =
        IdentityLiveness.passive2DVerdict(attribute, threshold)

    fun async(work: Runnable) {
        FaceRecognitionQueue.async(work)
    }

    companion object {
        @Volatile
        var engineReady: Boolean = false
            private set

        @Volatile
        private var instance: FaceRecognitionClient? = null

        @JvmStatic
        fun get(context: Context): FaceRecognitionClient {
            return instance ?: synchronized(this) {
                instance ?: FaceRecognitionClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
