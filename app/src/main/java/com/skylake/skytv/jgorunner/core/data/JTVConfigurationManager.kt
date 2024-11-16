package com.skylake.skytv.jgorunner.core.data

import android.content.Context
import com.google.gson.Gson
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File

class JTVConfigurationManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: JTVConfigurationManager? = null

        // Singleton
        fun getInstance(context: Context): JTVConfigurationManager {
            return instance ?: synchronized(this) {
                instance
                    ?: JTVConfigurationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Configuration
    private val preferenceManager = SkySharedPref.getInstance(context.applicationContext)
    val jtvConfiguration = readFromJTVConfiguration()
    private val filesDir = context.filesDir

    private fun readFromJTVConfiguration(): JTVConfiguration {
        val jtvConfigLocation = preferenceManager.myPrefs.jtvConfigLocation ?: return JTVConfiguration()
        val jtvConfigFile = File(jtvConfigLocation)
        if (!jtvConfigFile.exists())
            return JTVConfiguration()

        return try {
            Gson().fromJson(jtvConfigFile.readText(), JTVConfiguration::class.java)
        } catch (e: Exception){
            JTVConfiguration()
        }
    }

    fun saveJTVConfiguration() {
        var jtvConfigLocation = preferenceManager.myPrefs.jtvConfigLocation

        if (jtvConfigLocation == null) {
            val parentDir = File(filesDir, "jiotv_go")
            jtvConfigLocation = File(parentDir, "jtv_config.json").absolutePath
            jtvConfiguration.pathPrefix = parentDir.absolutePath

            preferenceManager.myPrefs.jtvConfigLocation = jtvConfigLocation
            preferenceManager.savePreferences()
        }

        val jtvConfigFile = File(jtvConfigLocation!!)
        if (!jtvConfigFile.exists()) {
            jtvConfigFile.parentFile?.mkdirs()
            jtvConfigFile.createNewFile()
        }

        jtvConfigFile.writeText(Gson().toJson(jtvConfiguration))
    }
}