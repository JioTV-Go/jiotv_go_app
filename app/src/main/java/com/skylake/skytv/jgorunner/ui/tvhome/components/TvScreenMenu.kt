package com.skylake.skytv.jgorunner.ui.tvhome.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import com.skylake.skytv.jgorunner.ui.tvhome.M3UChannelExp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MutableCollectionMutableState")
@Composable
fun TvScreenMenu(
    showDialog: Boolean,
    context: Context,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSelectionsMade: (
        quality: String?,
        layoutMode: String,
        categoryNames: List<String>,
        categoryIds: List<Int>,
        languageNames: List<String>,
        languageIds: List<Int>
    ) -> Unit
) {
    if (!showDialog) return

    val preferenceManager = SkySharedPref.getInstance(context)
    var showCustomUrlInputDialog by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf(preferenceManager.myPrefs.custURL ?: "") }
    var showRecentTab by remember { mutableStateOf(preferenceManager.myPrefs.showRecentTab) }
    var startTvAutomatically by remember { mutableStateOf(preferenceManager.myPrefs.startTvAutomatically) }
    var startTvAutoDelay by remember { mutableStateOf(preferenceManager.myPrefs.startTvAutoDelay) }
    var startTvAutoDelayTime by remember { mutableIntStateOf(preferenceManager.myPrefs.startTvAutoDelayTime) }
    val focusRequester = remember { FocusRequester() }
    var showProcessingDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // State for playlist selection
    var showPlaylist by remember {
        mutableStateOf(preferenceManager.myPrefs.showPLAYLIST)
    }
    var layoutMode by remember {
        mutableStateOf(preferenceManager.myPrefs.tvLayoutMode ?: "Default")
    }
    val isCardUiExperimentEnabled = remember { preferenceManager.myPrefs.cardUiExperiment }

    val layoutOptions = listOf("Default", "CardUI(TV)")
    var layoutDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCategories2 by remember {
        mutableStateOf(preferenceManager.myPrefs.lastSelectedCategoriesExp?.let {
            Gson().fromJson(it, object : TypeToken<List<String>>() {}.type)
        } ?: listOf("All")
        )
    }
    val selectedM3uCategories = remember { mutableStateOf(selectedCategories2.toMutableList()) }
    var categories by remember { mutableStateOf<List<M3UChannelExp>>(emptyList()) }

    try {
        val json = preferenceManager.myPrefs.channelListJson
        if (!json.isNullOrBlank()) {
            val type = object : TypeToken<List<M3UChannelExp>>() {}.type
            val channels: List<M3UChannelExp> = Gson().fromJson(json, type)
            categories = channels.distinctBy { it.url }
        } else {
            selectedCategories2 = listOf("All")
        }
    } catch (_: Exception) {
        selectedCategories2 = listOf("All")
    }



    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (!preferenceManager.myPrefs.customPlaylistSupport && !preferenceManager.myPrefs.showPLAYLIST) {
                    showPlaylist = true
                }

                if (isCardUiExperimentEnabled) {
                    DropdownSelection2(
                        title = "Layout",
                        options = layoutOptions,
                        selectedOption = layoutMode,
                        onOptionSelected = { selected ->
                            layoutMode = selected
                        },
                        expanded = layoutDropdownExpanded,
                        onExpandChange = { layoutDropdownExpanded = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Quality Selection ---
                var selectedQuality by remember {
                    mutableStateOf(preferenceManager.myPrefs.filterQX ?: "auto")
                }
                val qualityMap = mapOf(
                    "Auto" to "auto",
                    "High" to "high",
                    "Medium" to "medium",
                    "Low" to "low"
                )
                val qualityOptions = qualityMap.keys.toList()
                var qualityDropdownExpanded by remember { mutableStateOf(false) }
                val selectedQualityLabel =
                    qualityMap.entries.find { it.value == selectedQuality }?.key
                        ?: qualityOptions[0]

                if (showPlaylist) {
                    DropdownSelection2(
                        title = "Quality",
                        options = qualityOptions,
                        selectedOption = selectedQualityLabel,
                        onOptionSelected = { label ->
                            selectedQuality = (qualityMap[label] ?: "auto")
                        },
                        expanded = qualityDropdownExpanded,
                        onExpandChange = { qualityDropdownExpanded = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- TV Start Page Selection  ---
                val startScreenTV = mapOf(
                    "All Channels" to 0,
                    "Recent Channels" to 1
                )
                val startOptionsTV = startScreenTV.keys.toList()
                var selectedScreenTV by remember {
                    mutableIntStateOf(
                        preferenceManager.myPrefs.selectedScreenTV?.toIntOrNull() ?: 0
                    )
                }
                var screenDropdownExpanded by remember { mutableStateOf(false) }
                val selectedScreenLabel =
                    startScreenTV.entries.find { it.value == selectedScreenTV }?.key
                        ?: startOptionsTV[0]
                if (showRecentTab) {
                    DropdownSelection2(
                        title = "Select TV start page",
                        options = startOptionsTV,
                        selectedOption = selectedScreenLabel,
                        onOptionSelected = { label ->
                            selectedScreenTV = startScreenTV[label] ?: 0
                        },
                        expanded = screenDropdownExpanded,
                        onExpandChange = { screenDropdownExpanded = it }
                    )


                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- TV Remote Navigation Configuration ---
                val tvRemoteNavigationOptions = mapOf(
                    "Channel Up / Channel Down" to 0,
                    "Remote Up / Remote Down" to 1,
                    "Disable" to -1,
                )
                val tvRemoteNavigationLabels = tvRemoteNavigationOptions.keys.toList()
                var selectedTvRemoteNavOption by remember {
                    mutableIntStateOf(
                        preferenceManager.myPrefs.selectedRemoteNavTV?.toIntOrNull() ?: 0
                    )
                }
                var isTvRemoteNavDropdownExpanded by remember { mutableStateOf(false) }
                val selectedTvRemoteNavLabel = tvRemoteNavigationOptions.entries
                    .find { it.value == selectedTvRemoteNavOption }
                    ?.key ?: tvRemoteNavigationLabels[0]
                DropdownSelection2(
                    title = "Select Channel change keys",
                    options = tvRemoteNavigationLabels,
                    selectedOption = selectedTvRemoteNavLabel,
                    onOptionSelected = { label ->
                        selectedTvRemoteNavOption = tvRemoteNavigationOptions[label] ?: 0
                    },
                    expanded = isTvRemoteNavDropdownExpanded,
                    onExpandChange = { isExpanded -> isTvRemoteNavDropdownExpanded = isExpanded }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- Category Selection ---
                val categoryMap = mapOf(
                    "All Categories" to null, "Entertainment" to 5, "Movies" to 6, "Kids" to 7,
                    "Sports" to 8, "Lifestyle" to 9, "Infotainment" to 10, "News" to 12,
                    "Music" to 13, "Devotional" to 15, "Business" to 16, "Educational" to 17,
                    "Shopping" to 18, "JioDarshan" to 19
                )
                val categoryOptions = categoryMap.keys.toList()
                val selectedCategoryInts = remember {
                    mutableStateOf(
                        preferenceManager.myPrefs.filterCI?.split(",")
                            ?.mapNotNull { it.toIntOrNull() }?.toMutableList() ?: mutableListOf()
                    )
                }
                val selectedCategories = remember {
                    mutableStateOf(
                        selectedCategoryInts.value.mapNotNull { id ->
                            categoryMap.entries.find { it.value == id }?.key
                        }.toMutableList()
                    )
                }
                var showCategoryCheckboxes by remember { mutableStateOf(false) }
                var showCategoryCheckboxes2 by remember { mutableStateOf(false) }

                if (showPlaylist) {
                    Column {
                        Text(
                            text = "Select Categories",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showCategoryCheckboxes = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Categories")
                            }
                        }
                    }
                    if (showCategoryCheckboxes) {
                        Dialog(onDismissRequest = { showCategoryCheckboxes = false }) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    MultiSelectDropdown(
                                        title = "Category",
                                        options = categoryOptions,
                                        selectedOptions = selectedCategories.value,
                                        onOptionsSelected = { names ->
                                            selectedCategories.value = names.toMutableList()
                                            selectedCategoryInts.value =
                                                names.mapNotNull { categoryMap[it] }.toMutableList()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showCategoryCheckboxes = false },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Done")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Column {
                        Text(
                            text = "Select Categories",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showCategoryCheckboxes2 = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Categories")
                            }
                        }
                    }
                    if (showCategoryCheckboxes2) {
                        Dialog(onDismissRequest = { showCategoryCheckboxes2 = false }) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    MultiSelectDropdown2(
                                        title = "Category",
                                        options = categories,
                                        selectedOptions = selectedCategories2,
                                        onOptionsSelected = { newSelection ->
                                            selectedCategories2 = newSelection
                                            selectedM3uCategories.value = newSelection.toMutableList()

                                            preferenceManager.myPrefs.lastSelectedCategoriesExp =
                                                Gson().toJson(newSelection)
                                            preferenceManager.savePreferences()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showCategoryCheckboxes2 = false },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Done")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- Language Selection ---
                val languageMap = mapOf(
                    "All Languages" to null,
                    "Hindi" to 1,
                    "Marathi" to 2,
                    "Punjabi" to 3,
                    "Urdu" to 4,
                    "Bengali" to 5,
                    "English" to 6,
                    "Malayalam" to 7,
                    "Tamil" to 8,
                    "Gujarati" to 9,
                    "Odia" to 10,
                    "Telugu" to 11,
                    "Bhojpuri" to 12,
                    "Kannada" to 13,
                    "Assamese" to 14,
                    "Nepali" to 15,
                    "French" to 16,
                    "Other" to 18
                )
                val languageOptions = languageMap.keys.toList()
                val selectedLanguageInts = remember {
                    mutableStateOf(
                        preferenceManager.myPrefs.filterLI?.split(",")
                            ?.mapNotNull { it.toIntOrNull() }?.toMutableList() ?: mutableListOf()
                    )
                }
                val selectedLanguages = remember {
                    mutableStateOf(
                        selectedLanguageInts.value.mapNotNull { id ->
                            languageMap.entries.find { it.value == id }?.key
                        }.toMutableList()
                    )
                }
                var showLanguageCheckboxes by remember { mutableStateOf(false) }

                if (showPlaylist) {
                    Column {
                        Text(
                            text = "Select Languages",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showLanguageCheckboxes = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Languages")
                            }
                        }
                    }
                    if (showLanguageCheckboxes) {
                        Dialog(onDismissRequest = { showLanguageCheckboxes = false }) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    MultiSelectDropdown(
                                        title = "Language",
                                        options = languageOptions,
                                        selectedOptions = selectedLanguages.value,
                                        onOptionsSelected = { names ->
                                            selectedLanguages.value = names.toMutableList()
                                            selectedLanguageInts.value =
                                                names.mapNotNull { languageMap[it] }.toMutableList()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showLanguageCheckboxes = false },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Done")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- Experimental/Debug Section ---
                if (preferenceManager.myPrefs.customPlaylistSupport) {
                    Column {
                        Text("Add Channels", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showCustomUrlInputDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Add Custom Playlist URL")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Playlist Selection Dropdown ---
                    var playlistDropdownExpanded by remember { mutableStateOf(false) }
                    val customUrlFilename = preferenceManager.myPrefs.custURL
                        ?.substringAfterLast('/')
                        ?.takeIf { it.isNotBlank() } ?: "Custom Playlist"
                    val playlistOptions = listOf("JioTVGO", customUrlFilename)
                    val selectedPlaylistLabel = if (showPlaylist) "JioTVGO" else customUrlFilename

                    DropdownSelection2(
                        title = "Select Playlist",
                        options = playlistOptions,
                        selectedOption = selectedPlaylistLabel,
                        onOptionSelected = { label ->
                            showPlaylist = (label == "JioTVGO")
                        },
                        expanded = playlistDropdownExpanded,
                        onExpandChange = { playlistDropdownExpanded = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
///////////////////////////////
                if (showPlaylist) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = showRecentTab,
                            onCheckedChange = { checked ->
                                showRecentTab = checked
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Show Recent Channels")
                    }
                }

///////////////////////////////

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = startTvAutomatically,
                        onCheckedChange = { checked ->
                            startTvAutomatically = checked
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Auto play channel")
                }

                if (false) {
                    if (startTvAutomatically) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = startTvAutoDelay,
                                onCheckedChange = { checked ->
                                    startTvAutoDelay = checked
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Play everytime /w delay")
                        }
                    }

                    if (startTvAutoDelay) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = startTvAutoDelayTime.toFloat(),
                                onValueChange = { startTvAutoDelayTime = it.toInt() },
                                valueRange = 2f..10f,
                                steps = 3,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(focusRequester)
                                    .focusable()
                                    .onKeyEvent { event ->
                                        when (event.nativeKeyEvent.keyCode) {
                                            Key.DirectionRight.nativeKeyCode -> {
                                                startTvAutoDelayTime =
                                                    (startTvAutoDelayTime + 2).coerceAtMost(10)
                                                true
                                            }

                                            Key.DirectionLeft.nativeKeyCode -> {
                                                startTvAutoDelayTime =
                                                    (startTvAutoDelayTime - 2).coerceAtLeast(2)
                                                true
                                            }

                                            else -> false
                                        }
                                    }
                            )


                        }
                        Text(
                            text = "Delay: $startTvAutoDelayTime seconds",
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                //////////////////////////////////////


                if (showCustomUrlInputDialog) {
                    Dialog(onDismissRequest = { showCustomUrlInputDialog = false }) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Enter Playlist URL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customUrl,
                                    onValueChange = { customUrl = it },
                                    label = { Text("Playlist URL") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (customUrl.startsWith("http")) {
                                            showProcessingDialog = true
                                            showCustomUrlInputDialog = false
                                            coroutineScope.launch {
                                                preferenceManager.myPrefs.custURL = customUrl
                                                preferenceManager.savePreferences()
                                                delay(2000)
                                                showProcessingDialog = false
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Enter correct URL for playlist [m3u]",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(onClick = {
                                        showCustomUrlInputDialog = false
                                    }) { Text("Cancel") }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        showProcessingDialog = true
                                        showCustomUrlInputDialog = false
                                        coroutineScope.launch {
                                            preferenceManager.myPrefs.custURL = customUrl
                                            preferenceManager.savePreferences()
                                            delay(2000)
                                            showProcessingDialog = false
                                        }
                                    }) { Text("Save") }
                                }
                            }
                        }
                    }
                }

                if (showProcessingDialog) {
                    Log.d("DIXc", "inpro")
                    ProcessingDialogExp(
                        context = context,
                        onComplete = { channelList ->
                            Log.d(
                                "TVDialog",
                                "Loaded ${channelList.size} channels"
                            )
                        },
                        onError = { errorMessage -> Log.d("TVDialog", "Error: $errorMessage") }
                    )
                }

                // --- Action Buttons ---
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            selectedQuality = "auto"
                            selectedScreenTV = 0
                            selectedTvRemoteNavOption = 0
                            selectedCategories.value.clear()
                            selectedCategoryInts.value.clear()
                            selectedLanguages.value.clear()
                            selectedLanguageInts.value.clear()
                            showPlaylist = false // Reset playlist selection
                            layoutMode = "Default"
                            preferenceManager.apply {
                                myPrefs.selectedScreenTV = "0"
                                myPrefs.filterQX = "auto"
                                myPrefs.filterCI = ""
                                myPrefs.filterLI = ""
                                myPrefs.selectedRemoteNavTV = "0"
                                myPrefs.showPLAYLIST = false
                                myPrefs.showRecentTab = false
                                myPrefs.startTvAutomatically = false
                                myPrefs.startTvAutoDelay = false
                                myPrefs.startTvAutoDelayTime = 0
                                // myPrefs.lastSelectedCategoriesExp = ""
                                myPrefs.lastSelectedCategoryExp = "All"
                                myPrefs.tvLayoutMode = "Default"
                                

                                savePreferences()
                            }
                            onReset()
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset") }
                    Button(
                        onClick = {
                            onSelectionsMade(
                                selectedQuality,
                                layoutMode,
                                if (showPlaylist) selectedCategories.value else selectedM3uCategories.value,
                                if (showPlaylist) selectedCategoryInts.value else mutableListOf(),
                                // qualityMap[selectedQuality],
                                // selectedCategories.value,
                                // selectedCategoryInts.value,
                                selectedLanguages.value,
                                selectedLanguageInts.value
                            )
                            preferenceManager.apply {
                                myPrefs.selectedScreenTV = selectedScreenTV.toString()
                                myPrefs.selectedRemoteNavTV = selectedTvRemoteNavOption.toString()
                                myPrefs.filterQX = selectedQuality
                                myPrefs.filterCI = selectedCategoryInts.value.joinToString(",")
                                myPrefs.filterLI = selectedLanguageInts.value.joinToString(",")
                                if (myPrefs.customPlaylistSupport) {
                                    myPrefs.showPLAYLIST = showPlaylist
                                }
                                myPrefs.showRecentTab = showRecentTab
                                myPrefs.startTvAutomatically = startTvAutomatically
                                myPrefs.startTvAutoDelay = startTvAutoDelay
                                myPrefs.startTvAutoDelayTime = startTvAutoDelayTime
                                // Respect experiment toggle: if off, force Default and do not save CardUI
                                myPrefs.tvLayoutMode = if (isCardUiExperimentEnabled) layoutMode else "Default"
                                if (!showPlaylist) {
                                    myPrefs.lastSelectedCategoryExp =
                                        if (selectedM3uCategories.value.isEmpty()) "All"
                                        else selectedM3uCategories.value.joinToString(",")
                                }
                                savePreferences()
                            }
                            // Stop playback and close PiP when saving from TV screen menu
                            try {
                                PlayerCommandBus.requestStopPlayback()
                                PlayerCommandBus.requestClosePip()
                            } catch (_: Exception) { /* no-op */ }
                            onDismiss()
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun MultiSelectDropdown(
    title: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionsSelected: (List<String>) -> Unit
) {
    Column {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Column(modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val isChecked = selectedOptions.contains(option)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            val mutableSelected = selectedOptions.toMutableList()

                            when (option) {
                                "All Languages" -> {
                                    mutableSelected.clear()
                                    mutableSelected.add("All Languages")
                                }

                                else -> {
                                    if (isChecked) {
                                        mutableSelected.remove(option)
                                    } else {
                                        mutableSelected.remove("All Languages")
                                        mutableSelected.add(option)
                                    }
                                }
                            }
                            onOptionsSelected(mutableSelected)
                        }
                    )
                    Text(text = option, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}


@Composable
fun MultiSelectDropdown2(
    title: String,
    options: List<M3UChannelExp>,
    selectedOptions: List<String>,
    onOptionsSelected: (List<String>) -> Unit
) {
    val distinctCategoryNames = options.mapNotNull { it.category }.distinct()

    val sortedOptions = if (distinctCategoryNames.firstOrNull() == "All") {
        distinctCategoryNames
    } else {
        listOf("All") + distinctCategoryNames.filter { it != "All" }
    }

    Column {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Column(modifier = Modifier.fillMaxWidth()) {
            sortedOptions.forEach { categoryName ->
                val isChecked = selectedOptions.contains(categoryName)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            val mutableSelected = selectedOptions.toMutableList()

                            if (checked) {
                                if (categoryName == "All") {
                                    mutableSelected.clear()
                                    mutableSelected.add("All")
                                } else {
                                    mutableSelected.remove("All")
                                    if (!mutableSelected.contains(categoryName)) {
                                        mutableSelected.add(categoryName)
                                    }
                                }
                            } else {
                                mutableSelected.remove(categoryName)
                                if (mutableSelected.isEmpty()) {
                                    mutableSelected.add("All")
                                }
                            }

                            onOptionsSelected(mutableSelected)
                        }
                    )
                    Text(text = categoryName, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}


@Composable
fun DropdownSelection2(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    Column {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onExpandChange(true) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = selectedOption)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onOptionSelected(option)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}
