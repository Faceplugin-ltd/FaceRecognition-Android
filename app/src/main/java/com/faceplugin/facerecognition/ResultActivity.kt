package com.faceplugin.facerecognition

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_new)

        val identifyedFace = intent.getParcelableExtra("identified_face") as? Bitmap
        val enrolledFace = intent.getParcelableExtra("enrolled_face") as? Bitmap
        val identifiedName = intent.getStringExtra("identified_name")


        findViewById<ImageView>(R.id.imageEnrolled).setImageBitmap(enrolledFace)
        findViewById<ImageView>(R.id.imageIdentified).setImageBitmap(identifyedFace)
        findViewById<TextView>(R.id.textPerson).text = "ID: " + identifiedName
        /*findViewById<TextView>(R.id.textSimilarity).text = "Similarity: " + similarity
        findViewById<TextView>(R.id.textLiveness).text = "Liveness score: " + livenessScore
        findViewById<TextView>(R.id.textYaw).text = "Yaw: " + yaw
        findViewById<TextView>(R.id.textRoll).text = "Roll: " + roll
        findViewById<TextView>(R.id.textPitch).text = "Pitch: " + pitch

        if (face_quality < 0.5f) {
            val msg = String.format("Quality: Low, score = %.03f", face_quality)
            findViewById<TextView>(R.id.txtQuality).text = msg
        } else if(face_quality < 0.75f){
            val msg = String.format("Quality: Medium, score = %.03f", face_quality)
            findViewById<TextView>(R.id.txtQuality).text = msg
        } else {
            val msg = String.format("Quality: High, score = %.03f", face_quality)
            findViewById<TextView>(R.id.txtQuality).text = msg
        }

        var msg = String.format("Luminance: %.03f", face_luminance)
        findViewById<TextView>(R.id.txtLuminance).text = msg*/


        findViewById<androidx.cardview.widget.CardView>(R.id.btn_details).setOnClickListener {
            val bottomSheet = IdentifyDetailsActivity()
            val bundle = Bundle()
            bundle.putString("identified_name", identifiedName)
            bundle.putFloat("similarity", intent.getFloatExtra("similarity", 0f))
            bundle.putFloat("liveness", intent.getFloatExtra("liveness", 0f))
            bundle.putFloat("yaw", intent.getFloatExtra("yaw", 0f))
            bundle.putFloat("roll", intent.getFloatExtra("roll", 0f))
            bundle.putFloat("pitch", intent.getFloatExtra("pitch", 0f))
            bundle.putFloat("face_quality", intent.getFloatExtra("face_quality", 0f))
            bundle.putFloat("face_luminance", intent.getFloatExtra("face_luminance", 0f))
            bottomSheet.arguments = bundle

            //startActivity(intent)
            bottomSheet.show(supportFragmentManager, "IdentifyDetailsActivity")
        }
    }
}