package com.skylake.skytv.jgorunner.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class SkySharedPref private constructor(context: Context) {
    companion object {
        private const val PREF_NAME = "SkySharedPref"

        @Volatile
        private var instance: SkySharedPref? = null

        // Singleton
        fun getInstance(context: Context): SkySharedPref {
            return instance ?: synchronized(this) {
                instance ?: SkySharedPref(context.applicationContext).also { instance = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    val myPrefs = readFromSharedPreferences()

    fun savePreferences() {
        SharedPrefStructure::class.memberProperties.forEach { property ->
            // Access the annotation
            val annotation = property.findAnnotation<SharedPrefKey>()

            // If annotation exists, save the value in SharedPreferences
            annotation?.let { ann ->
                // Make the property accessible (in case it's private)
                property.isAccessible = true

                // Get the value of the property
                // Use the annotated key to save the value
                val value = myPrefs.let {
                    property.get(it)
                }
                if (value == null) {
                    return@let
                }
                when (value) {
                    is String -> editor.putString(ann.key, value)
                    is Boolean -> editor.putBoolean(ann.key, value)
                    is Int -> editor.putInt(ann.key, value)
                    is Float -> editor.putFloat(ann.key, value)
                    is Long -> editor.putLong(ann.key, value)
                    else -> {
                        Log.e("SkySharedPref", "Unsupported type")
                        Log.e("SkySharedPref", "Type: ${value.javaClass}")
                        throw IllegalArgumentException("Unsupported type")
                    }
                }
            }
        }
        editor.apply()
    }

    // Function to read data from SharedPreferences using annotated keys
    private fun readFromSharedPreferences(): SharedPrefStructure {
        val instance = SharedPrefStructure()

        SharedPrefStructure::class.memberProperties.forEach { property ->
            val annotation = property.findAnnotation<SharedPrefKey>()
            annotation?.let {
                property.isAccessible = true

                // Check if the key exists in SharedPreferences
                // If not, skip the property
                if (sharedPreferences.contains(it.key).not()) {
                    return@let
                }

                // Retrieve the value from SharedPreferences using the annotated key
                val value = when (property.returnType.classifier) {
                    String::class -> sharedPreferences.getString(it.key, null) as Any
                    Boolean::class -> sharedPreferences.getBoolean(it.key, false)
                    Int::class -> sharedPreferences.getInt(it.key, 0)
                    Float::class -> sharedPreferences.getFloat(it.key, 0f)
                    Long::class -> sharedPreferences.getLong(it.key, 0L)
                    else -> throw IllegalArgumentException("Unsupported type")
                }

                // Set the value to the property
                if (property is KMutableProperty<*>) {
                    property.setter.call(instance, value)
                }
            }
        }
        return instance
    }

    data class SharedPrefStructure(
        @SharedPrefKey("serve_local")
        var serveLocal: Boolean = false,

        @SharedPrefKey("auto_start_server")
        var autoStartServer: Boolean = false,

        @SharedPrefKey("auto_start_on_boot")
        var autoStartOnBoot: Boolean = false,

        @SharedPrefKey("auto_start_on_boot_foreground")
        var autoStartOnBootForeground: Boolean = false,

        @SharedPrefKey("auto_start_iptv")
        var autoStartIPTV: Boolean = false,

        @SharedPrefKey("enable_auto_update")
        var enableAutoUpdate: Boolean = true,

        @SharedPrefKey("jtv_go_port")
        var jtvGoServerPort: Int = 5350,

        @SharedPrefKey("jtv_go_binary_name")
        var jtvGoBinaryName: String? = null,

        @SharedPrefKey("jtv_binary_version")
        var jtvGoBinaryVersion: String? = "v0.0.0",

        @SharedPrefKey("jtv_config_location")
        var jtvConfigLocation: String? = null,

        @SharedPrefKey("iptv_app_name")
        var iptvAppName: String? = null,

        @SharedPrefKey("iptv_app_package_name")
        var iptvAppPackageName: String? = null,

        @SharedPrefKey("iptv_app_launch_activity")
        var iptvAppLaunchActivity: String? = null,

        @SharedPrefKey("iptv_launch_countdown")
        var iptvLaunchCountdown: Int = 5,

        @SharedPrefKey("recent_channels_json")
        var recentChannelsJson: String? = null
    )

    // Annotation class to define the key for SharedPreferences
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SharedPrefKey(val key: String)
}
