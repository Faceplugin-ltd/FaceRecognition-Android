package com.faceplugin.faceattribute

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.faceattribute.SettingsActivity
import com.ocp.facesdk.FaceBox
import com.ocp.facesdk.FaceDetectionParam
import com.ocp.facesdk.FaceSDK
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private val SELECT_PHOTO_REQUEST_CODE = 1
        private val SELECT_ATTRIBUTE_REQUEST_CODE = 2
    }

    private lateinit var dbManager: DBManager
    private lateinit var textWarning: TextView
    private lateinit var personAdapter: PersonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textWarning = findViewById<TextView>(R.id.textWarning)


        var ret = FaceSDK.setActivation(
            "BHXyX7G02RB3wpAm3E3+87VOyDelFmVDeobU0eg5pOe6W9gX/KM1csCi18Dw9bPrq96V5OCtPz/z\n" +
                    "fu64b+PDL4tPvoRics/+JF8iNakrqSOpF6jfYig5SPZAov6ufNoxdd01MpuYLZdjpLAfxr2DWho6\n" +
                    "kiTH8vV/3IGT1ZjsLfmtllDeXE80oan4GuSYsGVj5YxwAeyUEKcAT6+wMipYUK0HXHVnKkAZp6rm\n" +
                    "owBRpquxBUIlCMcbzYjIu0mu/lMy5ZIPklaLWflaG8rMIxJgFhOQMob4dV1cJcvNUdfv4PZwnUh2\n" +
                    "1vz8s5Apn9KTsX2zPyJdF280zbsOTgljVvXe3Q=="
        )

        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(assets)
        }

        if (ret != FaceSDK.SDK_SUCCESS) {
            textWarning.setVisibility(View.VISIBLE)
            if (ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                textWarning.setText("Invalid license!")
            } else if (ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                textWarning.setText("Invalid error!")
            } else if (ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                textWarning.setText("License expired!")
            } else if (ret == FaceSDK.SDK_NO_ACTIVATED) {
                textWarning.setText("No activated!")
            } else if (ret == FaceSDK.SDK_INIT_ERROR) {
                textWarning.setText("Init error!")
            }
        }

        dbManager = DBManager(this)
        dbManager.loadPerson()

        personAdapter = PersonAdapter(this, DBManager.personList)
        val listView: ListView = findViewById<View>(R.id.listPerson) as ListView
        listView.setAdapter(personAdapter)

        findViewById<Button>(R.id.buttonEnroll).setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_PICK)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_PHOTO_REQUEST_CODE)
        }

        findViewById<Button>(R.id.buttonIdentify).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        findViewById<Button>(R.id.buttonCapture).setOnClickListener {
            startActivity(Intent(this, CaptureActivity::class.java))
        }

        findViewById<Button>(R.id.buttonAttribute).setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_PICK)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_ATTRIBUTE_REQUEST_CODE)
        }

        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.lytBrand).setOnClickListener {
            val browse = Intent(Intent.ACTION_VIEW, Uri.parse("https://faceplugin.com"))
            startActivity(browse)
        }
    }


    override fun onResume() {
        super.onResume()

        personAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, null)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face_detected), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])
                    val templates = FaceSDK.templateExtraction(bitmap, faceBoxes[0])

                    dbManager.insertPerson("Person" + Random.nextInt(10000, 20000), faceImage, templates)
                    personAdapter.notifyDataSetChanged()
                    Toast.makeText(this, getString(R.string.person_enrolled), Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        } else if (requestCode == SELECT_ATTRIBUTE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)


                val param = FaceDetectionParam()
                param.check_liveness = true
                param.check_liveness_level = SettingsActivity.getLivenessLevel(this)
                param.check_eye_closeness = true
                param.check_face_occlusion = true
                param.check_mouth_opened = true
                param.estimate_age_gender = true
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, param)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face_detected), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])

                    val intent = Intent(this, AttributeActivity::class.java)
                    intent.putExtra("face_image", faceImage)
                    intent.putExtra("yaw", faceBoxes[0].yaw)
                    intent.putExtra("roll", faceBoxes[0].roll)
                    intent.putExtra("pitch", faceBoxes[0].pitch)
                    intent.putExtra("face_quality", faceBoxes[0].face_quality)
                    intent.putExtra("face_luminance", faceBoxes[0].face_luminance)
                    intent.putExtra("liveness", faceBoxes[0].liveness)
                    intent.putExtra("left_eye_closed", faceBoxes[0].left_eye_closed)
                    intent.putExtra("right_eye_closed", faceBoxes[0].right_eye_closed)
                    intent.putExtra("face_occlusion", faceBoxes[0].face_occlusion)
                    intent.putExtra("mouth_opened", faceBoxes[0].mouth_opened)
                    intent.putExtra("age", faceBoxes[0].age)
                    intent.putExtra("gender", faceBoxes[0].gender)

                    startActivity(intent)
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        }
    }
}