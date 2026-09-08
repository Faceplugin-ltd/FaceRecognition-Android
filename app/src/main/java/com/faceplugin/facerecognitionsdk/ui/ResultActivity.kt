package com.faceplugin.facerecognitionsdk.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.facerecognitionsdk.R

class ResultActivity : AppCompatActivity() {
    companion object {
        @JvmField
        var identifiedFace: Bitmap? = null

        @JvmField
        var enrolledFace: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val identified = identifiedFace ?: intent.getParcelableExtra<Bitmap>(FaceBoxExtras.IDENTIFIED_FACE)
        identifiedFace = null
        val enrolled = enrolledFace ?: intent.getParcelableExtra<Bitmap>(FaceBoxExtras.ENROLLED_FACE)
        enrolledFace = null
        findViewById<LandmarkImageView>(R.id.imageIdentified).setContent(
            identified,
            FaceBoxExtras.cropLandmarkPoints(intent),
        )
        findViewById<ImageView>(R.id.imageEnrolled).setImageBitmap(enrolled)
        val name = intent.getStringExtra(FaceBoxExtras.IDENTIFIED_NAME).orEmpty()
        val similarity = intent.getFloatExtra(FaceBoxExtras.SIMILARITY, 0f)
        findViewById<TextView>(R.id.textPerson).text = "ID: $name"
        findViewById<TextView>(R.id.textSimilarity).text = "Similarity: $similarity"
        ResultDetails.bind(findViewById<LinearLayout>(R.id.lytDetails), ResultDetails.rows(this, intent, includeMatch = false))
    }
}
