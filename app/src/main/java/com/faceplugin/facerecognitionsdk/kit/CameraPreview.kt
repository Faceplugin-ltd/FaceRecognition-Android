package com.faceplugin.facerecognitionsdk.kit

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

object CameraPreview {
    const val WIDTH = 720
    const val HEIGHT = 1280

    @JvmStatic
    fun bind(
        owner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        viewFinder: PreviewView,
        lensFacing: Int,
        executor: Executor,
        analyzer: ImageAnalysis.Analyzer,
    ) {
        val rotation = viewFinder.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview = Preview.Builder()
            .setTargetResolution(Size(WIDTH, HEIGHT))
            .setTargetRotation(rotation)
            .build()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(WIDTH, HEIGHT))
            .setTargetRotation(rotation)
            .build()
        analysis.setAnalyzer(executor, analyzer)
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(owner, cameraSelector, preview, analysis)
        preview.surfaceProvider = viewFinder.surfaceProvider
    }
}
