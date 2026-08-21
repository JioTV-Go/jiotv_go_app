package com.skylake.skytv.jgorunner.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import java.util.UUID

data class OmniFavoriteEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String? = null
)

class OmniFavoritesStore(private val pref: SkySharedPref) {
    private val gson = Gson()
    private val type = object : TypeToken<List<OmniFavoriteEntry>>() {}.type

    fun load(): List<OmniFavoriteEntry> {
        val json = pref.myPrefs.omniFavoritesJson ?: "[]"
        return try { gson.fromJson<List<OmniFavoriteEntry>>(json, type) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    fun save(entries: List<OmniFavoriteEntry>) {
        pref.myPrefs.omniFavoritesJson = gson.toJson(entries.distinctBy { it.id })
        pref.savePreferences()
    }

    fun add(channel: OmniChannel): Boolean {
        if (load().any { it.name == channel.name }) return false
        save(load() + OmniFavoriteEntry(name = channel.name ?: "", url = channel.url))
        return true
    }

    fun remove(id: String) { save(load().filterNot { it.id == id || it.name == id }) }
    fun isFavorite(channel: OmniChannel): Boolean = load().any { it.name == channel.name }
}
