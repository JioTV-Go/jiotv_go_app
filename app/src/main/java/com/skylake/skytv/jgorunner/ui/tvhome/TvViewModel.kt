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
    val preferenceManager = SkySharedPref.getInstance(application)
    private val appPrefs = application.getSharedPreferences("sky_jtv_prefs", Context.MODE_PRIVATE)
    private val channelCachePrefs = application.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    val localPort = preferenceManager.myPrefs.jtvGoServerPort
    val basefinURL = "http://localhost:$localPort"

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())

    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())
    val filteredChannels: StateFlow<List<Channel>> = _filteredChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _favoriteChannels = MutableStateFlow<List<Channel>>(emptyList())
    val favoriteChannels: StateFlow<List<Channel>> = _favoriteChannels.asStateFlow()

    private val _recentChannels = MutableStateFlow<List<Channel>>(emptyList())
    val recentChannels: StateFlow<List<Channel>> = _recentChannels.asStateFlow()

    private val _epgData = MutableStateFlow<EpgProgram?>(null)
    val epgData: StateFlow<EpgProgram?> = _epgData.asStateFlow()

    private val _isEpgLoading = MutableStateFlow(false)
    val isEpgLoading: StateFlow<Boolean> = _isEpgLoading.asStateFlow()

    private val _epgError = MutableStateFlow(false)
    val epgError: StateFlow<Boolean> = _epgError.asStateFlow()

    init {
        loadFavoriteChannels()
        loadRecentChannels()
        loadChannels(forceRefresh = false)
    }

    fun loadChannels(forceRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _isError.value = false

            if (!forceRefresh) {
                val cachedJson = channelCachePrefs.getString("channels_json", null)
                if (!cachedJson.isNullOrEmpty()) {
                    try {
                        val cachedResponse = gson.fromJson(cachedJson, ChannelResponse::class.java)
                        _allChannels.value = cachedResponse.result
                        applyFilters(cachedResponse.result)
                        _isLoading.value = false // Stop loading once cache is shown
                    } catch (e: Exception) {
                        channelCachePrefs.edit { remove("channels_json") }
                    }
                }
            }

            if (forceRefresh || _allChannels.value.isEmpty()) {
                _isLoading.value = true
            }

            try {
                val response = ChannelUtils.fetchChannels("$basefinURL/channels")
                if (response != null && response.result.isNotEmpty()) {
                    _allChannels.value = response.result

                    channelCachePrefs.edit {
                        putString("channels_json", gson.toJson(response))
                    }

                    applyFilters(response.result)
                } else if (_allChannels.value.isEmpty()) {
                    _isError.value = true
                }
            } catch (e: Exception) {
                Log.e("TvViewModel", "Network error", e)
                if (_allChannels.value.isEmpty()) _isError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun applyFilters(channels: List<Channel> = _allChannels.value, newCategoryIds: Set<Int>? = null) {
        viewModelScope.launch(Dispatchers.Default) {
            // FIX: If newCategoryIds is provided but empty, make it null so the filter doesn't hide everything
            val activeCategoryIds = if (newCategoryIds != null) {
                newCategoryIds.toList().takeIf { it.isNotEmpty() }
            } else {
                preferenceManager.myPrefs.filterCI
                    ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                    ?.takeIf { it.isNotEmpty() }
            }

            val activeLanguageIds = preferenceManager.myPrefs.filterLI
                ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                ?.takeIf { it.isNotEmpty() }

            val tempResponse = ChannelResponse(result = channels)
            val filtered = ChannelUtils.filterChannels(
                tempResponse,
                categoryIds = activeCategoryIds,
                languageIds = activeLanguageIds
            )

            _filteredChannels.value = filtered
        }
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

    fun loadEpg(channelId: String?) {
        if (channelId == null) {
            _epgData.value = null
            _epgError.value = false
            _isEpgLoading.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isEpgLoading.value = true
            _epgError.value = false
            try {
                val epgURL = "$basefinURL/epg/$channelId/0"
                val fetchedEpg = ChannelUtils.fetchEpg(epgURL)
                if (fetchedEpg != null) {
                    _epgData.value = fetchedEpg
                } else {
                    _epgData.value = null
                    _epgError.value = true
                }
            } catch (e: Exception) {
                _epgData.value = null
                _epgError.value = true
            } finally {
                _isEpgLoading.value = false
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