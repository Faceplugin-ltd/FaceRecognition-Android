package com.faceplugin.facerecognitionsdk.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.facerecognitionsdk.R

class AttributeActivity : AppCompatActivity() {
    companion object {
        @JvmField
        var faceImage: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attribute)
        val image = faceImage ?: intent.getParcelableExtra<Bitmap>(FaceBoxExtras.FACE_IMAGE)
        faceImage = null
        findViewById<LandmarkImageView>(R.id.imageFace).setContent(
            image,
            FaceBoxExtras.cropLandmarkPoints(intent),
        )
        ResultDetails.bind(findViewById<LinearLayout>(R.id.lytDetails), ResultDetails.rows(this, intent, includeMatch = false))
    }
}
