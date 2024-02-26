package com.faceplugin.facerecognition

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AttributeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attribute)

        val faceImage = intent.getParcelableExtra("face_image") as? Bitmap
        val livenessScore = intent.getFloatExtra("liveness", 0f)
        val yaw = intent.getFloatExtra("yaw", 0f)
        val roll = intent.getFloatExtra("roll", 0f)
        val pitch = intent.getFloatExtra("pitch", 0f)
        val face_quality = intent.getFloatExtra("face_quality", 0f)
        val face_luminance = intent.getFloatExtra("face_luminance", 0f)
        val left_eye_closed = intent.getFloatExtra("left_eye_closed", 0f)
        val right_eye_closed = intent.getFloatExtra("right_eye_closed", 0f)
        val face_occlusion = intent.getFloatExtra("face_occlusion", 0f)
        val mouth_opened = intent.getFloatExtra("mouth_opened", 0f)
        val age = intent.getIntExtra("age", 0)
        val gender = intent.getIntExtra("gender", 0)

        findViewById<ImageView>(R.id.imageFace).setImageBitmap(faceImage)

        if (livenessScore > SettingsActivity.getLivenessThreshold(this)) {
            val msg = String.format("Liveness: Real, score = %.03f", livenessScore)
            findViewById<TextView>(R.id.txtLiveness).text = msg
        } else {
            val msg = String.format("Liveness: Spoof, score =  %.03f", livenessScore)
            findViewById<TextView>(R.id.txtLiveness).text = msg
        }

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
        findViewById<TextView>(R.id.txtLuminance).text = msg

        msg = String.format("Angles: yaw = %.03f, roll = %.03f, pitch = %.03f", yaw, roll, pitch)
        findViewById<TextView>(R.id.txtAngles).text = msg

        if (face_occlusion > SettingsActivity.getOcclusionThreshold(this)) {
            msg = String.format("Face occluded: score = %.03f", face_occlusion)
            findViewById<TextView>(R.id.txtOcclusion).text = msg
        } else {
            msg = String.format("Face not occluded: score = %.03f", face_occlusion)
            findViewById<TextView>(R.id.txtOcclusion).text = msg
        }

        msg = String.format("Left eye closed: %b, %.03f, Right eye closed: %b, %.03f", left_eye_closed > SettingsActivity.getEyecloseThreshold(this),
            left_eye_closed, right_eye_closed > SettingsActivity.getEyecloseThreshold(this), right_eye_closed)
        findViewById<TextView>(R.id.txtEyeClosed).text = msg

        msg = String.format("Mouth opened: %b, %.03f", mouth_opened > SettingsActivity.getMouthopenThreshold(this), mouth_opened)
        findViewById<TextView>(R.id.txtMouthOpened).text = msg

        msg = String.format("Age: %d", age)
        findViewById<TextView>(R.id.txtAge).text = msg

        if(gender == 0) {
            msg = String.format("Gender: Male")
        } else {
            msg = String.format("Gender: Female")
        }
        findViewById<TextView>(R.id.txtGender).text = msg
    }
}