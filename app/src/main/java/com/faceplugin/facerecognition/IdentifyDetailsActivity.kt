package com.faceplugin.facerecognition

import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class IdentifyDetailsActivity : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_identify_details, container, false)

        val identifiedName = arguments?.getString("identified_name")
        val similarity = arguments?.getFloat("similarity", 0f)
        val livenessScore = arguments?.getFloat("liveness", 0f)
        val yaw = arguments?.getFloat("yaw", 0f)
        val roll = arguments?.getFloat("roll", 0f)
        val pitch = arguments?.getFloat("pitch", 0f)
        val face_quality = arguments?.getFloat("face_quality", 0f)
        val face_luminance = arguments?.getFloat("face_luminance", 0f)

        view.findViewById<TextView>(R.id.textPerson).text = "Identified: " + identifiedName
        view.findViewById<TextView>(R.id.textSimilarity).text = "Similarity: " + similarity
        view.findViewById<TextView>(R.id.textLiveness).text = "Liveness score: " + livenessScore
        view.findViewById<TextView>(R.id.textYaw).text = "Yaw: " + yaw
        view.findViewById<TextView>(R.id.textRoll).text = "Roll: " + roll
        view.findViewById<TextView>(R.id.textPitch).text = "Pitch: " + pitch

        if (face_quality != null) {
            if (face_quality < 0.5f) {
                val msg = String.format("Quality: Low, score = %.03f", face_quality)
                view.findViewById<TextView>(R.id.txtQuality).text = msg
            } else if (face_quality < 0.75f) {
                val msg = String.format("Quality: Medium, score = %.03f", face_quality)
                view.findViewById<TextView>(R.id.txtQuality).text = msg
            } else {
                val msg = String.format("Quality: High, score = %.03f", face_quality)
                view.findViewById<TextView>(R.id.txtQuality).text = msg
            }
        }

        var msg = String.format("Luminance: %.03f", face_luminance)
        view.findViewById<TextView>(R.id.txtLuminance).text = msg

        return view
    }
}