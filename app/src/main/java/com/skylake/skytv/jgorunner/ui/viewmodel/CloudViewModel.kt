package com.skylake.skytv.jgorunner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.data.model.CloudChannel
import com.skylake.skytv.jgorunner.data.model.Server
import com.skylake.skytv.jgorunner.data.repository.CloudRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CloudViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CloudRepository(application)
    private val prefManager = SkySharedPref.getInstance(application)

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    private val _channels = MutableStateFlow<List<CloudChannel>>(emptyList())
    private val _isLoadingChannels = MutableStateFlow(false)
    val isLoadingChannels: StateFlow<Boolean> = _isLoadingChannels.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _selectedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val selectedLanguages: StateFlow<Set<String>> = _selectedLanguages.asStateFlow()

    private val serverSettingsCache = mutableMapOf<String, ServerSettings>()

    private val _autoplayCountdown = MutableStateFlow<Int?>(null)
    val autoplayCountdown: StateFlow<Int?> = _autoplayCountdown.asStateFlow()

    private var countdownJob: Job? = null

    data class ServerSettings(
        val categories: Set<String>,
        val languages: Set<String>
    )

    val filteredChannels: StateFlow<List<CloudChannel>> = combine(
        _channels, _searchQuery, _selectedCategories, _selectedLanguages
    ) { channels, query, categories, languages ->
        channels.filter { channel ->
            val matchesQuery = query.isEmpty() ||
                channel.name.contains(query, ignoreCase = true) ||
                channel.group?.contains(query, ignoreCase = true) == true ||
                channel.language?.contains(query, ignoreCase = true) == true

            val matchesCategory = categories.isEmpty() || categories.contains("All") ||
                categories.contains(channel.group)

            val matchesLanguage = languages.isEmpty() || languages.contains("All") ||
                languages.contains(channel.language)

            matchesQuery && matchesCategory && matchesLanguage
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categories: StateFlow<List<String>> = _channels.combine(MutableStateFlow(Unit)) { channels, _ ->
        listOf("All") + channels.mapNotNull { it.group }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("All"))

    val languages: StateFlow<List<String>> = _channels.combine(MutableStateFlow(Unit)) { channels, _ ->
        listOf("All") + channels.mapNotNull { it.language }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("All"))

    init {
        loadServers()
        if (prefManager.myPrefs.autoStartServer) {
             startAutoplayCountdown()
        }
    }

    private fun loadServers() {
        viewModelScope.launch {
            val fetchedServers = repository.fetchServers()
            _servers.value = fetchedServers

            val lastServerName = prefManager.myPrefs.iptvAppName
            if (!lastServerName.isNullOrEmpty()) {
                val lastServer = fetchedServers.find { it.name == lastServerName }
                if (lastServer != null) {
                    selectServer(lastServer)
                }
            }
        }
    }

    fun selectServer(server: Server) {
        _selectedServer.value?.let { prev ->
            serverSettingsCache[prev.name] = ServerSettings(_selectedCategories.value, _selectedLanguages.value)
        }

        _selectedServer.value = server
        prefManager.myPrefs.iptvAppName = server.name
        prefManager.savePreferences()

        val settings = serverSettingsCache[server.name]
        _selectedCategories.value = settings?.categories ?: emptySet()
        _selectedLanguages.value = settings?.languages ?: emptySet()

        loadChannels(server.url)
        cancelAutoplayCountdown()
    }

    fun loadChannels(serverUrl: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingChannels.value = true
            _channels.value = repository.fetchChannels(serverUrl, forceRefresh)
            _isLoadingChannels.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategory(category: String) {
        val current = _selectedCategories.value.toMutableSet()
        if (category == "All") {
            current.clear()
        } else {
            if (current.contains(category)) {
                current.remove(category)
            } else {
                current.add(category)
            }
        }
        _selectedCategories.value = current
    }

    fun toggleLanguage(language: String) {
        val current = _selectedLanguages.value.toMutableSet()
        if (language == "All") {
            current.clear()
        } else {
            if (current.contains(language)) {
                current.remove(language)
            } else {
                current.add(language)
            }
        }
        _selectedLanguages.value = current
    }

    private fun startAutoplayCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 5 downTo 1) {
                _autoplayCountdown.value = i
                delay(1000)
            }
            _autoplayCountdown.value = 0
            _servers.value.firstOrNull()?.let {
                selectServer(it)
            }
            _autoplayCountdown.value = null
        }
    }

    fun cancelAutoplayCountdown() {
        countdownJob?.cancel()
        _autoplayCountdown.value = null
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        prefManager.myPrefs.autoStartServer = enabled
        prefManager.savePreferences()
        if (!enabled) cancelAutoplayCountdown()
    }

    fun isAutoplayEnabled(): Boolean {
        return prefManager.myPrefs.autoStartServer
    }
}
