package com.faceplugin.facerecognitionsdk.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.facerecognitionsdk.FaceBox
import com.faceplugin.facerecognitionsdk.FaceDetectionParam
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK
import com.faceplugin.facerecognitionsdk.R
import com.faceplugin.facerecognitionsdk.kit.CameraFrameUtils
import com.faceplugin.facerecognitionsdk.kit.EnrolledPerson
import com.faceplugin.facerecognitionsdk.kit.FaceRecognitionClient
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SELECT_PHOTO_REQUEST_CODE = 1
        private const val SELECT_ATTRIBUTE_REQUEST_CODE = 2
        private const val EXTRA_PROCESS_DIR = "process_dir"
        private const val EXTRA_PROCESS_FILE = "process_file"
        private const val ATTR_TAG = "FrsAttr"

        /** `FP1.…` from FacePlugin for `applicationId` com.faceplugin.facerecognitionsdk. */
        private const val LICENSE_KEY =
            "FP1.RlBMMQMAAQDJx5NBpyjuxrTSuz8MAgAASPNMFWCS7q6vchdPDy23dWUgdnJm6T5LgA4HdMSGYv1yOd+oLCfrwRrNPknfqxACqbYHDTxu1SIMVNekHZW0lBnrkLNZvHNprQnTA/8GJ0XBhcT3+kUpasAT9GMzdBT5g7gM+FV8KTT2P71rWtYOIpDszqjhTpXBh0ZkSpRHFVftBlT+W94+4gLQmSg2VF6hMoPMazski4Llav+mfpgnoRDR67tcWhUyuf+wOnOS/v3uyhoE8AqXdpJKqhB4aVEXC/ZtjnQFuJwdANDSWRtzYBYX/Yb5rhzBc0c/p71ZcbP+ye054XUniZokjA/ZrQ+r4tftcAtF69sFiU60km2q6CtRu4tbQpL34YC2ztJPaLywBlwQz0lmEwXbKHmCHOc9SzdaYDWHt7lIgkxaEYztKGDjFzP0coB4R2MzKhh9vcZQePxo8TSwbZqBLb7Sk3BpZ6NYQKbM6iEwEd6VB8of2Meqc4/IJtBP9Yp6Q4P3FoIiZ+m5+Ik/P9O/Cb12zleqNLMU5qTibURBUGB0P/LCo3ueMUhlRBGz4cmbSw/OZifKrfw96axxo1uDyCi2qYIG0mTyXpQX0KW+r/aJ+3CosIukmabvPU3M/PO8Bbnem5P9udecXHsbmfgShAXQ5mDkMIhJQaB3zPwAIl5Agp2T/YRY+11bb5jN3zF7pLGQbgVc3VnDZcwv/hnsLviKADCBhwJCAIO0bZgO621nFz0Yup29vR45IcsQ7MdRHZkMs8LQk2EGwrR6gEn0Peu9Inxc25JXYUBPBN1rjWPyGW1dE4VTWQi6AkFnsguhXcZi1rnPz5et2fWhhzHPMk5wmt2cDrS62JkV0gXYFI4E8O+b2PVFHhkBO92m/kwXy2VsUk0VAQWItd///w=="
    }

    private lateinit var textWarning: TextView
    private lateinit var textEnrolledFace: TextView
    private lateinit var personAdapter: PersonAdapter
    private lateinit var sdkActionButtons: List<View>
    private val people = ArrayList<EnrolledPerson>()
    private var sdkReady = false
    private var sdkLoading = true
    private var pendingEnrollUri: Uri? = null
    private var pendingAttrUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsActivity.applyEngineDefaults(this)
        setContentView(R.layout.activity_main)

        textWarning = findViewById(R.id.textWarning)
        textEnrolledFace = findViewById(R.id.tv_enrolledface)
        textEnrolledFace.visibility = View.INVISIBLE

        personAdapter = PersonAdapter(this, people, textEnrolledFace)
        findViewById<ListView>(R.id.listPerson).adapter = personAdapter

        sdkActionButtons = listOf(
            findViewById(R.id.ll_enroll),
            findViewById(R.id.ll_identify),
            findViewById(R.id.ll_capture),
            findViewById(R.id.ll_attribute),
        )

        findViewById<LinearLayout>(R.id.ll_enroll).setOnClickListener {
            if (!ensureReady()) return@setOnClickListener
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_PHOTO_REQUEST_CODE)
        }
        findViewById<LinearLayout>(R.id.ll_identify).setOnClickListener {
            if (!ensureReady()) return@setOnClickListener
            startActivity(Intent(this, CameraActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.ll_capture).setOnClickListener {
            if (!ensureReady()) return@setOnClickListener
            startActivity(Intent(this, CaptureActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.ll_attribute).setOnClickListener {
            if (!ensureReady()) return@setOnClickListener
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_ATTRIBUTE_REQUEST_CODE)
        }
        findViewById<LinearLayout>(R.id.ll_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.ll_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.lytBrand).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.company_website_url))))
        }

        val client = FaceRecognitionClient.get(this)
        if (client.isEngineReady) {
            applySdkReady()
        } else {
            sdkReady = false
            sdkLoading = true
            textWarning.visibility = View.VISIBLE
            textWarning.setText(R.string.sdk_loading)
            setSdkActionsEnabled(false)
            client.activate(LICENSE_KEY) { code ->
                runOnUiThread {
                    sdkLoading = false
                    if (code == FaceRecognitionSDK.SDK_SUCCESS) {
                        applySdkReady()
                    } else {
                        sdkReady = false
                        textWarning.visibility = View.VISIBLE
                        textWarning.text = when (code) {
                            FaceRecognitionSDK.SDK_LICENSE_INVALID -> getString(R.string.sdk_license_invalid)
                            FaceRecognitionSDK.SDK_LICENSE_EXPIRED -> getString(R.string.sdk_license_expired)
                            FaceRecognitionSDK.SDK_NOT_ACTIVATED -> getString(R.string.sdk_not_activated)
                            else -> getString(R.string.sdk_init_failed)
                        }
                        setSdkActionsEnabled(false)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (sdkReady) handleProcessExtras()
    }

    override fun onResume() {
        super.onResume()
        if (sdkReady) refreshPeople()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data ?: return
        if (requestCode == SELECT_PHOTO_REQUEST_CODE) {
            if (!sdkReady) {
                pendingEnrollUri = uri
                return
            }
            enrollFromGallery(uri)
        } else if (requestCode == SELECT_ATTRIBUTE_REQUEST_CODE) {
            if (!sdkReady) {
                pendingAttrUri = uri
                return
            }
            attributeFromGallery(uri)
        }
    }

    private fun applySdkReady() {
        sdkLoading = false
        sdkReady = true
        FaceRecognitionClient.get(this).loadDatabase()
        textWarning.visibility = View.GONE
        setSdkActionsEnabled(true)
        refreshPeople()
        handleProcessExtras()
        pendingEnrollUri?.let {
            pendingEnrollUri = null
            enrollFromGallery(it)
        }
        pendingAttrUri?.let {
            pendingAttrUri = null
            attributeFromGallery(it)
        }
    }

    private fun handleProcessExtras() {
        intent.getStringExtra(EXTRA_PROCESS_DIR)?.let { dir ->
            FaceRecognitionClient.get(this).async { scanAttributes(dir) }
        }
        intent.getStringExtra(EXTRA_PROCESS_FILE)?.let { path ->
            attributeFromFile(path)
        }
    }

    private fun enrollFromGallery(uri: Uri) {
        FaceRecognitionClient.get(this).async {
            try {
                val raw = Utils.getCorrectlyOrientedImage(this, uri)
                val bitmap = CameraFrameUtils.enginePreparedImage(raw)
                val client = FaceRecognitionClient.get(this)
                val faceBoxes = client.faceDetection(bitmap, null)
                if (faceBoxes.size != 1) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            if (faceBoxes.isEmpty()) R.string.no_face_detected else R.string.multiple_face_detected,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return@async
                }
                val box = faceBoxes[0]
                val faceImage = Utils.cropFace(bitmap, box)
                val templates = client.templateExtraction(bitmap, box)
                runOnUiThread {
                    if (templates == null || templates.isEmpty()) {
                        Toast.makeText(this, R.string.enroll_failed, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    client.enroll("Person" + Random.nextInt(10000, 20000), templates, faceImage)
                    refreshPeople()
                    Toast.makeText(this, R.string.person_enrolled, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(this, R.string.gallery_load_failed, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun attributeFromFile(path: String) {
        FaceRecognitionClient.get(this).async {
            try {
                val decoded = BitmapFactory.decodeFile(path)
                if (decoded == null) {
                    runOnUiThread { Toast.makeText(this, R.string.gallery_load_failed, Toast.LENGTH_SHORT).show() }
                    return@async
                }
                val bitmap = CameraFrameUtils.enginePreparedImage(decoded)
                val param = FaceDetectionParam.allAttributes()
                param.check_liveness_level = SettingsActivity.getLivenessLevel(this)
                val client = FaceRecognitionClient.get(this)
                val faceBoxes = client.faceDetection(bitmap, param)
                if (faceBoxes.size != 1) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            if (faceBoxes.isEmpty()) R.string.no_face_detected else R.string.multiple_face_detected,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return@async
                }
                val box = faceBoxes[0]
                val crop = Utils.cropFace(bitmap, box)
                Log.i(
                    ATTR_TAG,
                    "$path\tn=1\tliveness=${box.liveness}\tlabel=${box.livenessLabel}\tmask=${box.maskLabel}",
                )
                runOnUiThread { openAttributeResult(crop, bitmap, box) }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(this, R.string.gallery_load_failed, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun scanAttributes(rootPath: String) {
        val root = File(rootPath)
        val images = root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            .sortedBy { it.path }
            .toList()
        val lines = mutableListOf<String>()
        val client = FaceRecognitionClient.get(this)
        val param = FaceDetectionParam.allAttributes()
        param.check_liveness_level = SettingsActivity.getLivenessLevel(this)
        for (file in images) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                val line = "${file.absolutePath}\tDECODE_FAIL"
                lines += line
                Log.i(ATTR_TAG, line)
                continue
            }
            try {
                val boxes = client.faceDetection(bitmap, param)
                if (boxes.isEmpty()) {
                    val line = "${file.absolutePath}\tn=0"
                    lines += line
                    Log.i(ATTR_TAG, line)
                } else {
                    boxes.forEachIndexed { i, box ->
                        val line =
                            "${file.absolutePath}\tn=${boxes.size}\ti=$i\tliveness=${box.liveness}\tlabel=${box.livenessLabel}\tmask=${box.maskLabel}"
                        lines += line
                        Log.i(ATTR_TAG, line)
                    }
                }
            } catch (e: Exception) {
                val line = "${file.absolutePath}\tERROR ${e.message}"
                lines += line
                Log.e(ATTR_TAG, line, e)
            } finally {
                bitmap.recycle()
            }
        }
        val out = File(getExternalFilesDir(null), "frs_attr_results.txt")
        out.writeText(lines.joinToString("\n"))
        Log.i(ATTR_TAG, "DONE wrote ${out.absolutePath} count=${images.size}")
        runOnUiThread {
            Toast.makeText(this, "Scanned ${images.size} images", Toast.LENGTH_LONG).show()
        }
    }

    private fun attributeFromGallery(uri: Uri) {
        FaceRecognitionClient.get(this).async {
            try {
                val raw = Utils.getCorrectlyOrientedImage(this, uri)
                val bitmap = CameraFrameUtils.enginePreparedImage(raw)
                val param = FaceDetectionParam.allAttributes()
                param.check_liveness_level = SettingsActivity.getLivenessLevel(this)
                val client = FaceRecognitionClient.get(this)
                val faceBoxes = client.faceDetection(bitmap, param)
                if (faceBoxes.size != 1) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            if (faceBoxes.isEmpty()) R.string.no_face_detected else R.string.multiple_face_detected,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return@async
                }
                val box = faceBoxes[0]
                val crop = Utils.cropFace(bitmap, box)
                runOnUiThread { openAttributeResult(crop, bitmap, box) }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(this, R.string.gallery_load_failed, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun openAttributeResult(crop: Bitmap, source: Bitmap, box: FaceBox) {
        AttributeActivity.faceImage = crop
        val intent = Intent(this, AttributeActivity::class.java)
        FaceBoxExtras.putBox(intent, box)
        FaceBoxExtras.putCropLandmarks(intent, Utils.mapLandmarksToCrop(source, box, crop.width, crop.height))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.gallery_load_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshPeople() {
        people.clear()
        people.addAll(FaceRecognitionClient.get(this).enrolledPeople())
        personAdapter.notifyDataSetChanged()
        textEnrolledFace.visibility = if (people.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun setSdkActionsEnabled(enabled: Boolean) {
        for (button in sdkActionButtons) {
            button.isEnabled = enabled
            button.isClickable = enabled
            button.alpha = if (enabled) 1f else 0.45f
        }
    }

    private fun ensureReady(): Boolean {
        if (sdkReady) return true
        val message = if (sdkLoading) R.string.sdk_loading else R.string.sdk_failed
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        return false
    }
}
