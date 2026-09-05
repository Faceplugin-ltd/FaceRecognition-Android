package com.faceplugin.facerecognitionsdk.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.faceplugin.facerecognitionsdk.R
import com.faceplugin.facerecognitionsdk.kit.FaceRecognitionClient

class SettingsActivity : AppCompatActivity() {

    companion object {
        // Face SDK video_worker defaults (recognition yaw/pitch ±40, detector 0.67, 2D liveness 0.5).
        const val DEFAULT_CAMERA_LENS = "front"
        const val DEFAULT_LIVENESS_THRESHOLD = "0.5"
        const val DEFAULT_IDENTIFY_THRESHOLD = "0.67"
        const val DEFAULT_LIVENESS_LEVEL = "0"
        const val DEFAULT_YAW_THRESHOLD = "40.0"
        const val DEFAULT_ROLL_THRESHOLD = "40.0"
        const val DEFAULT_PITCH_THRESHOLD = "40.0"
        const val DEFAULT_EYECLOSE_THRESHOLD = "0.5"

        private const val PREFS_SCHEMA = "prefs_schema"
        private const val PREFS_SCHEMA_VW = 1

        /** Write Face SDK defaults once so capture is not stuck on the old 10° / 0.7 prefs. */
        @JvmStatic
        fun applyEngineDefaults(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getInt(PREFS_SCHEMA, 0) >= PREFS_SCHEMA_VW) return
            prefs.edit()
                .putString("camera_lens", DEFAULT_CAMERA_LENS)
                .putString("liveness_threshold", DEFAULT_LIVENESS_THRESHOLD)
                .putString("liveness_level", DEFAULT_LIVENESS_LEVEL)
                .putString("identify_threshold", DEFAULT_IDENTIFY_THRESHOLD)
                .putString("yaw_threshold", DEFAULT_YAW_THRESHOLD)
                .putString("roll_threshold", DEFAULT_ROLL_THRESHOLD)
                .putString("pitch_threshold", DEFAULT_PITCH_THRESHOLD)
                .putString("eyeclose_threshold", DEFAULT_EYECLOSE_THRESHOLD)
                .putInt(PREFS_SCHEMA, PREFS_SCHEMA_VW)
                .apply()
        }

        @JvmStatic
        fun getLivenessThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("liveness_threshold", DEFAULT_LIVENESS_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getIdentifyThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("identify_threshold", DEFAULT_IDENTIFY_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getCameraLens(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return if (sharedPreferences.getString("camera_lens", DEFAULT_CAMERA_LENS) == "back") {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        }

        @JvmStatic
        fun getLivenessLevel(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return if (sharedPreferences.getString("liveness_level", DEFAULT_LIVENESS_LEVEL) == "0") 0 else 1
        }

        @JvmStatic
        fun getYawThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("yaw_threshold", DEFAULT_YAW_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getRollThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("roll_threshold", DEFAULT_ROLL_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getPitchThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("pitch_threshold", DEFAULT_PITCH_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getEyecloseThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("eyeclose_threshold", DEFAULT_EYECLOSE_THRESHOLD)!!.toFloat()
        }

        /** Real vs spoof for Identify / Capture / overlay. Spoof labels always fail. */
        @JvmStatic
        fun livenessPassed(context: Context, score: Float, label: String?): Boolean {
            val lower = label.orEmpty().lowercase()
            if (lower.contains("spoof") || lower.contains("fake")) return false
            return score >= getLivenessThreshold(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val cameraLensPref = findPreference<ListPreference>("camera_lens")
            val livenessThresholdPref = findPreference<EditTextPreference>("liveness_threshold")
            val livenessLevelPref = findPreference<ListPreference>("liveness_level")
            val identifyThresholdPref = findPreference<EditTextPreference>("identify_threshold")
            val yawThresholdPref = findPreference<EditTextPreference>("yaw_threshold")
            val rollThresholdPref = findPreference<EditTextPreference>("roll_threshold")
            val pitchThresholdPref = findPreference<EditTextPreference>("pitch_threshold")
            val eyeCloseThresholdPref = findPreference<EditTextPreference>("eyeclose_threshold")
            val buttonRestorePref = findPreference<Preference>("restore_default_settings")

            fun rangeListener(min: Float, max: Float) = Preference.OnPreferenceChangeListener { _, newValue ->
                try {
                    val value = (newValue as String).toFloat()
                    if (value < min || value > max) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            livenessThresholdPref?.onPreferenceChangeListener = rangeListener(0f, 1f)
            identifyThresholdPref?.onPreferenceChangeListener = rangeListener(0f, 1f)
            yawThresholdPref?.onPreferenceChangeListener = rangeListener(0f, 90f)
            rollThresholdPref?.onPreferenceChangeListener = rangeListener(0f, 90f)
            pitchThresholdPref?.onPreferenceChangeListener = rangeListener(0f, 90f)
            eyeCloseThresholdPref?.onPreferenceChangeListener = rangeListener(0f, 1f)

            buttonRestorePref?.setOnPreferenceClickListener {
                cameraLensPref?.value = DEFAULT_CAMERA_LENS
                livenessLevelPref?.value = DEFAULT_LIVENESS_LEVEL
                livenessThresholdPref?.text = DEFAULT_LIVENESS_THRESHOLD
                identifyThresholdPref?.text = DEFAULT_IDENTIFY_THRESHOLD
                yawThresholdPref?.text = DEFAULT_YAW_THRESHOLD
                rollThresholdPref?.text = DEFAULT_ROLL_THRESHOLD
                pitchThresholdPref?.text = DEFAULT_PITCH_THRESHOLD
                eyeCloseThresholdPref?.text = DEFAULT_EYECLOSE_THRESHOLD
                Toast.makeText(activity, getString(R.string.restored_default_settings), Toast.LENGTH_LONG).show()
                true
            }

            findPreference<Preference>("clear_all_person")?.setOnPreferenceClickListener {
                FaceRecognitionClient.get(requireContext()).clearEnrolled()
                Toast.makeText(activity, getString(R.string.cleared_all_person), Toast.LENGTH_LONG).show()
                true
            }
        }
    }
}
