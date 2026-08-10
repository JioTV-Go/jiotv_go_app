package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.SkySharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

class TvViewModel(application: Application) : AndroidViewModel(application) {
    private val preferenceManager = SkySharedPref.getInstance(application)
    private val appPrefs = application.getSharedPreferences("sky_jtv_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    val localPort = preferenceManager.myPrefs.jtvGoServerPort
    val basefinURL = "http://localhost:$localPort"

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())

    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())
    val filteredChannels: StateFlow<List<Channel>> = _filteredChannels.asStateFlow()

    private val _recentChannels = MutableStateFlow<List<Channel>>(emptyList())
    val recentChannels: StateFlow<List<Channel>> = _recentChannels.asStateFlow()

    private val _favoriteChannels = MutableStateFlow<List<Channel>>(emptyList())
    val favoriteChannels: StateFlow<List<Channel>> = _favoriteChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _epgData = MutableStateFlow<EpgProgram?>(null)
    val epgData: StateFlow<EpgProgram?> = _epgData.asStateFlow()

    init {
        loadRecentChannels()
        loadFavoriteChannels()
    }

    fun fetchAndFilterChannels(categoryIds: Set<Int>, languageIds: List<Int>?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val response = ChannelUtils.fetchChannels("$basefinURL/channels")
                if (response != null) {
                    _allChannels.value = response.result
                    val filtered = ChannelUtils.filterChannels(
                        channelsResponse = response,
                        categoryIds = categoryIds.takeIf { it.isNotEmpty() }?.toList(),
                        languageIds = languageIds
                    )
                    _filteredChannels.value = filtered
                }
            } catch (e: Exception) {
                Log.e("TvViewModel", "Error fetching channels", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRecentChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            val json = preferenceManager.myPrefs.recentChannels
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<Channel>>() {}.type
                val channels: List<Channel> = gson.fromJson(json, type) ?: emptyList()
                _recentChannels.value = channels
            }
        }
    }

    fun saveToRecents(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecents = _recentChannels.value.toMutableList()
            val existingIndex = currentRecents.indexOfFirst { it.channel_id == channel.channel_id }

            if (existingIndex != -1) {
                val existingChannel = currentRecents.removeAt(existingIndex)
                currentRecents.add(0, existingChannel)
            } else {
                currentRecents.add(0, channel)
                if (currentRecents.size > 25) {
                    currentRecents.removeAt(currentRecents.size - 1)
                }
            }
            preferenceManager.myPrefs.recentChannels = gson.toJson(currentRecents)
            preferenceManager.savePreferences()
            _recentChannels.value = currentRecents
        }
    }

    fun saveM3UToRecents(channel: M3UChannelExp) {
        viewModelScope.launch(Dispatchers.IO) {
            val clickedAsChannel = Channel(
                channel_id = extractChannelIdFromPlayUrl(channel.url) ?: channel.url,
                channel_url = channel.url,
                logoUrl = channel.logo ?: "",
                channel_name = channel.name,
                channelCategoryId = 0,
                channelLanguageId = 0,
                isHD = true,
            )
            saveToRecents(clickedAsChannel)
        }
    }

    

    fun loadFavoriteChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            val json = appPrefs.getString("favorites_json", "[]")
            val type = object : TypeToken<List<Channel>>() {}.type
            val channels: List<Channel> = gson.fromJson(json, type) ?: emptyList()
            _favoriteChannels.value = channels
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFavs = _favoriteChannels.value.toMutableList()
            val existingIndex = currentFavs.indexOfFirst { it.channel_id == channel.channel_id }

            if (existingIndex != -1) {
                currentFavs.removeAt(existingIndex) 
            } else {
                currentFavs.add(0, channel) 
            }

            appPrefs.edit().putString("favorites_json", gson.toJson(currentFavs)).apply()
            _favoriteChannels.value = currentFavs
        }
    }

    fun toggleFavoriteM3U(channel: M3UChannelExp) {
        val clickedAsChannel = Channel(
            channel_id = extractChannelIdFromPlayUrl(channel.url) ?: channel.url,
            channel_url = channel.url,
            logoUrl = channel.logo ?: "",
            channel_name = channel.name,
            channelCategoryId = 0,
            channelLanguageId = 0,
            isHD = true,
        )
        toggleFavorite(clickedAsChannel)
    }

    fun loadEpg(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val epgURL = "$basefinURL/epg/$channelId/0"
                val fetchedEpg = ChannelUtils.fetchEpg(epgURL)
                _epgData.value = fetchedEpg
            } catch (e: Exception) {
                _epgData.value = null
            }
        }
    }

    fun swapFavoriteChannels(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _favoriteChannels.value.toMutableList()

            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val temp = currentList[fromIndex]
                currentList[fromIndex] = currentList[toIndex]
                currentList[toIndex] = temp
                appPrefs.edit { putString("favorites_json", gson.toJson(currentList)) }

                _favoriteChannels.value = currentList
            }
        }
    }
}