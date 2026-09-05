package com.faceplugin.facerecognitionsdk.kit

import android.graphics.Bitmap
import com.faceplugin.facerecognitionsdk.FaceBox
import com.faceplugin.facerecognitionsdk.FaceDetectionParam
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean

/** Serial access to native FaceRecognitionSDK — not safe for concurrent use. */
object FaceRecognitionQueue {
    private val executor = Executors.newSingleThreadExecutor(
        ThreadFactory { r ->
            Thread(r, "facerecognitionsdk-sdk").apply { isDaemon = true }
        },
    )
    private val frameBusy = AtomicBoolean(false)

    fun async(work: Runnable) {
        executor.execute(work)
    }

    fun <T> sync(work: () -> T): T {
        if (Thread.currentThread().name == "facerecognitionsdk-sdk") {
            return work()
        }
        val future: Future<T> = executor.submit(Callable { work() })
        return future.get()
    }

    fun detect(bitmap: Bitmap, crop: Boolean, flags: Int = FaceRecognitionSDK.DETECT_ALL): String? =
        sync { FaceRecognitionSDK.detect(bitmap, crop, flags) }

    fun faceDetection(bitmap: Bitmap, param: FaceDetectionParam?): List<FaceBox> =
        sync { FaceRecognitionSDK.faceDetection(bitmap, param) }

    fun templateExtraction(bitmap: Bitmap, face: FaceBox): ByteArray? =
        sync { FaceRecognitionSDK.templateExtraction(bitmap, face) }

    fun setLandmarkMode(mode: Int): Int =
        sync { FaceRecognitionSDK.setLandmarkMode(mode) }

    fun getLandmarkMode(): Int =
        sync { FaceRecognitionSDK.getLandmarkMode() }

    fun quality(bitmap: Bitmap, crop: Boolean): String? =
        sync { FaceRecognitionSDK.quality(bitmap, crop) }

    fun extractFeature(bitmap: Bitmap): String? =
        sync { FaceRecognitionSDK.extractFeature(bitmap) }

    fun similarity(feature1: ByteArray, feature2: ByteArray): Float =
        sync { FaceRecognitionSDK.similarity(feature1, feature2) }

    fun estimatorStatus(): String? =
        sync { FaceRecognitionSDK.estimatorStatusJSON() }

    fun startVideoWorker(config: FaceRecognitionSDK.VideoWorkerConfig): Int =
        sync {
            FaceRecognitionSDK.stopVideoWorker()
            FaceRecognitionSDK.startVideoWorker(config)
        }

    /** Clears the handler immediately; stops the native worker on the SDK thread (non-blocking). */
    fun stopVideoWorker() {
        FaceRecognitionSDK.setVideoWorkerEventHandler(null)
        async {
            FaceRecognitionSDK.stopVideoWorker()
            frameBusy.set(false)
        }
    }

    fun setVideoWorkerEventHandler(handler: ((String) -> Unit)?) {
        if (handler == null) {
            FaceRecognitionSDK.setVideoWorkerEventHandler(null)
        } else {
            FaceRecognitionSDK.setVideoWorkerEventHandler { json -> handler(json) }
        }
    }

    fun syncVideoWorkerDatabase(features: List<ByteArray>, matchThreshold: Float): Int =
        sync { FaceRecognitionSDK.syncVideoWorkerDatabase(features, matchThreshold) }

    fun addVideoWorkerFrame(bitmap: Bitmap): Int {
        if (Thread.currentThread().name == "facerecognitionsdk-sdk") {
            return FaceRecognitionSDK.addVideoWorkerFrame(bitmap)
        }
        if (!frameBusy.compareAndSet(false, true)) return 0
        val copy = try {
            if (bitmap.isRecycled) null else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: Throwable) {
            null
        }
        if (copy == null) {
            frameBusy.set(false)
            return -1
        }
        async {
            try {
                FaceRecognitionSDK.addVideoWorkerFrame(copy)
            } finally {
                if (!copy.isRecycled) copy.recycle()
                frameBusy.set(false)
            }
        }
        return 0
    }

    fun addVideoWorkerFrameRgb(rgb: ByteArray, width: Int, height: Int): Int =
        FaceRecognitionSDK.addVideoWorkerFrameRgb(rgb, width, height)
}
