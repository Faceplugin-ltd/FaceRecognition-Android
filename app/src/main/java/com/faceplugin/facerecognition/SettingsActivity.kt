package com.faceplugin.facerecognition

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.preference.*


class SettingsActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_CAMERA_LENS = "front"
        const val DEFAULT_LIVENESS_THRESHOLD = "0.7"
        const val DEFAULT_IDENTIFY_THRESHOLD = "0.8"
        const val DEFAULT_LIVENESS_LEVEL = "0"
        const val DEFAULT_YAW_THRESHOLD = "10.0"
        const val DEFAULT_ROLL_THRESHOLD = "10.0"
        const val DEFAULT_PITCH_THRESHOLD = "10.0"
        const val DEFAULT_OCCLUSION_THRESHOLD = "0.5"
        const val DEFAULT_EYECLOSE_THRESHOLD = "0.5"
        const val DEFAULT_MOUTHOPEN_THRESHOLD = "0.5"

        @JvmStatic
        fun getLivenessThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("liveness_threshold", SettingsActivity.DEFAULT_LIVENESS_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getIdentifyThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("identify_threshold", SettingsActivity.DEFAULT_IDENTIFY_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getCameraLens(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val cameraLens = sharedPreferences.getString("camera_lens", SettingsActivity.DEFAULT_CAMERA_LENS)
            if(cameraLens == "back") {
                return CameraSelector.LENS_FACING_BACK
            } else {
                return CameraSelector.LENS_FACING_FRONT
            }
        }

        @JvmStatic
        fun getLivenessLevel(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val livenessLevel = sharedPreferences.getString("liveness_level", SettingsActivity.DEFAULT_LIVENESS_LEVEL)
            if(livenessLevel == "0") {
                return 0
            } else {
                return 1
            }
        }

        @JvmStatic
        fun getYawThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("yaw_threshold", SettingsActivity.DEFAULT_YAW_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getRollThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("roll_threshold", SettingsActivity.DEFAULT_ROLL_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getPitchThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("pitch_threshold", SettingsActivity.DEFAULT_PITCH_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getOcclusionThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("occlusion_threshold", SettingsActivity.DEFAULT_OCCLUSION_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getEyecloseThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("eyeclose_threshold", SettingsActivity.DEFAULT_EYECLOSE_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getMouthopenThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("mouthopen_threshold", SettingsActivity.DEFAULT_MOUTHOPEN_THRESHOLD)!!.toFloat()
        }
    }

    lateinit var dbManager: DBManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbManager = DBManager(this)
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
            val occlusionThresholdPref = findPreference<EditTextPreference>("occlusion_threshold")
            val eyeCloseThresholdPref = findPreference<EditTextPreference>("eyeclose_threshold")
            val mouthOpenThresholdPref = findPreference<EditTextPreference>("mouthopen_threshold")
            val buttonRestorePref = findPreference<Preference>("restore_default_settings")

            livenessThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            identifyThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            yawThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 30.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            rollThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 30.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            pitchThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 30.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            occlusionThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            eyeCloseThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            mouthOpenThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            buttonRestorePref?.setOnPreferenceClickListener {

                cameraLensPref?.value = SettingsActivity.DEFAULT_CAMERA_LENS
                livenessLevelPref?.value = SettingsActivity.DEFAULT_LIVENESS_LEVEL
                livenessThresholdPref?.text = SettingsActivity.DEFAULT_LIVENESS_THRESHOLD
                identifyThresholdPref?.text = SettingsActivity.DEFAULT_IDENTIFY_THRESHOLD
                yawThresholdPref?.text = SettingsActivity.DEFAULT_YAW_THRESHOLD
                rollThresholdPref?.text = SettingsActivity.DEFAULT_ROLL_THRESHOLD
                pitchThresholdPref?.text = SettingsActivity.DEFAULT_PITCH_THRESHOLD
                occlusionThresholdPref?.text = SettingsActivity.DEFAULT_OCCLUSION_THRESHOLD
                eyeCloseThresholdPref?.text = SettingsActivity.DEFAULT_EYECLOSE_THRESHOLD
                mouthOpenThresholdPref?.text = SettingsActivity.DEFAULT_MOUTHOPEN_THRESHOLD


                Toast.makeText(activity, getString(R.string.restored_default_settings), Toast.LENGTH_LONG).show()
                true
            }

            val buttonClearPref = findPreference<Preference>("clear_all_person")
            buttonClearPref?.setOnPreferenceClickListener {
                val settingsActivity = activity as SettingsActivity
                settingsActivity.dbManager.clearDB()

                Toast.makeText(activity, getString(R.string.cleared_all_person), Toast.LENGTH_LONG).show()
                true
            }
        }
    }
}