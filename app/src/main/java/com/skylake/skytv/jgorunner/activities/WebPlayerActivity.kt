package com.skylake.skytv.jgorunner.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.data.SkySharedPref
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class WebPlayerActivity : ComponentActivity() {
    companion object {
        private const val TAG = "WebPlayerActivity"
        private const val DEFAULT_URL_TEMPLATE = "http://localhost:%d"
    }

    private var webView: WebView? = null
    private var loadingSpinner: ProgressBar? = null
    private var url: String? = null

    private var channelNumbers: List<String>? = null
    private var initURL: String? = null

    private var currentPlayId: String? = null
    private var currentLogoUrl: String? = null
    private var currentChannelName: String? = null

    private val recentChannels: MutableList<Channel> = ArrayList()

    private val prefManager = SkySharedPref.getInstance(this)

    private class Channel(var playId: String?, var logoUrl: String?, var channelName: String?)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_player)

        val savedPortNumber = prefManager.myPrefs.jtvGoServerPort
        val filterQ = prefManager.myPrefs.filterQ
        val filterL = prefManager.myPrefs.filterL
        val filterC = prefManager.myPrefs.filterC
//        val extraFilterUrl = "/?q=$filterQ&language=$filterL&category=$filterC" // "/?q=low&language=6&category=7"
        val extraFilterUrl = buildString {
            append("/")

            if (!filterQ.isNullOrEmpty()) append("?q=$filterQ")

            if (!filterL.isNullOrEmpty()) {
                if (isNotEmpty()) append("&")
                append("language=$filterL")
            }

            if (!filterC.isNullOrEmpty()) {
                if (isNotEmpty()) append("&")
                append("category=$filterC")
            }
        }

        url = String.format(Locale.getDefault(), DEFAULT_URL_TEMPLATE, savedPortNumber) + extraFilterUrl

        Log.d("DIX", url!!)

        Log.d(TAG, "URL: $url")

        setupBackPressedCallback()
        setupFullScreenMode()

        webView = findViewById(R.id.webview)
        loadingSpinner = findViewById(R.id.loading_spinner)

        setupWebView()
        loadUrl()
    }

    private var playerUrlCount = 0

    private fun setupBackPressedCallback() {
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView != null) {
                    val currentUrl = webView!!.url

                    if (currentUrl != null && currentUrl.contains("/player/")) {
                        playerUrlCount++
                        if (playerUrlCount >= 3) {
                            webView!!.loadUrl(initURL!!)
                        } else {
                            webView!!.goBack()
                        }
                    } else if (webView!!.canGoBack()) {
                        playerUrlCount++
                        if (playerUrlCount >= 6) {
                            finish()
                        } else {
                            webView!!.goBack()
                        }
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }


    private fun setupFullScreenMode() {
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        updateSystemUiVisibility()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemUiVisibility() // This maintains full-screen mode in both orientations
    }


    private fun updateSystemUiVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("deprecation")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView!!.webViewClient = CustomWebViewClient()
        webView!!.webChromeClient = WebChromeClient()

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.defaultTextEncodingName = "utf-8"
        webSettings.mixedContentMode = 0
        webSettings.mediaPlaybackRequiresUserGesture = false // Allow autoplay
    }

    private fun loadUrl() {
        if (url != null) {
            webView!!.loadUrl(url!!)
        }
    }

    private fun setDarkTheme() {
        if (webView != null) {
            val jsCode =
                "document.getElementsByTagName('html')[0].setAttribute('data-theme', 'dark');" +
                        "localStorage.setItem('theme', 'dark');"
            webView!!.evaluateJavascript(jsCode, null)
        }
    }

    override fun onPause() {
        super.onPause()
        webView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView!!.onResume()
//        webView!!.loadUrl(url!!)
    }


    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && webView!!.url != null && webView!!.url!!
                .contains("/player/")
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    navigateToNextChannel()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    navigateToPreviousChannel()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun navigateToNextChannel() {
        navigateChannel(1)
    }

    private fun navigateToPreviousChannel() {
        navigateChannel(-1)
    }

    private fun navigateChannel(direction: Int) {
        if (channelNumbers == null || channelNumbers!!.isEmpty()) {
            Log.d(TAG, "No channel numbers available.")
            return
        }

        Log.d(TAG, "Total channels available: " + channelNumbers!!.size)

        val currentUrl = checkNotNull(webView!!.url)
        val queryIndex = currentUrl.indexOf('?')

        val currentNumber = if (queryIndex != -1) {
            currentUrl.substring(currentUrl.lastIndexOf('/') + 1, queryIndex)
        } else {
            currentUrl.substring(currentUrl.lastIndexOf('/') + 1)
        }

        val index = channelNumbers!!.indexOf(currentNumber)

        if (index >= 0) {
            val newIndex = (index + direction + channelNumbers!!.size) % channelNumbers!!.size
            val newNumber = channelNumbers!![newIndex]
            val newUrl = if (queryIndex != -1) {
                currentUrl.replace("/$currentNumber?", "/$newNumber?")
            } else {
                currentUrl.replace("/$currentNumber", "/$newNumber")
            }

            Log.d(TAG, "Navigating to Channel: $newUrl")
            webView!!.loadUrl(newUrl)
        } else {
            Log.d(TAG, "Current number not found in channel numbers.")
        }
    }

    private inner class CustomWebViewClient : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.contains("/play/")) {
                initURL = webView!!.url
                Log.d(TAG, "Saving initURL: $initURL")

                // Extract the play ID from the URL
                val playId = if (url.matches(".*/play/([^/]+).*".toRegex())) url.replace(
                    ".*/play/([^/]+).*".toRegex(),
                    "$1"
                ) else null

                Log.d("WB", playId ?: "Play ID not found")

                // Use JavaScript to extract the channel logo and name
                view.evaluateJavascript(
                    "(function() { " +
                            "try { " +
                            "    var channelCard = document.querySelector('a[href*=\"/play/" + playId + "\"]'); " +
                            "    if (channelCard) { " +
                            "        var logoElement = channelCard.querySelector('img'); " +
                            "        var nameElement = channelCard.querySelector('span'); " +
                            "        var logoUrl = logoElement ? logoElement.getAttribute('src') : null; " +
                            "        var channelName = nameElement ? nameElement.innerText : null; " +
                            "        return JSON.stringify({playId: '" + playId + "', logoUrl: logoUrl, channelName: channelName}); " +
                            "    } else { " +
                            "        return null; " +
                            "    } " +
                            "} catch (error) { " +
                            "    return null; " +
                            "} " +
                            "})();"
                ) { result: String? ->
                    if (result != null && result != "null") {
                        try {
                            // Remove any extra quotes surrounding the JSON result
                            val jsonString =
                                result.replace("^\"|\"$".toRegex(), "").replace("\\\"", "\"")
                            val jsonResult = JSONObject(jsonString)
                            currentPlayId = jsonResult.getString("playId")
                            currentLogoUrl = jsonResult.getString("logoUrl")
                            currentChannelName = jsonResult.getString("channelName")

                            Log.d(
                               TAG,
                                "Channel Clicked: $currentChannelName (Play ID: $currentPlayId)"
                            )
                            saveRecentChannel(currentPlayId, currentLogoUrl, currentChannelName)
                        } catch (e: JSONException) {
                            Log.d(
                               TAG,
                                "JSON parsing error: " + e.message
                            )
                        }
                    } else {
                        Log.d(
                           TAG,
                            "No channel data extracted."
                        )
                    }
                }

                // Construct the new URL
                var modifiedUrl = url.replace("/play/", "/live/") + ".m3u8"

                modifiedUrl = modifiedUrl.replace("//.m3u8", ".m3u8")

                Log.d("DIX", "Modified URL for intent: $modifiedUrl")

                // Send the intent to ExoplayerActivity
                val intent = Intent(this@WebPlayerActivity, ExoplayerActivity::class.java).apply {
                    putExtra("video_url", modifiedUrl)
                    putExtra("current_play_id", playId?.substringBefore("?") ?: playId)
                    putExtra("channels_list", channelNumbers?.toTypedArray())
                }
                startActivity(intent)
                return true
            } else if (!url.contains("/play/") && !url.contains("/player/")) {
                initURL = url
                return false
            }
            return false
        }



    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            if (prefManager.myPrefs.darkMODE) {
                setDarkTheme()
            }
            loadingSpinner!!.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView, url: String) {
            loadingSpinner!!.visibility = View.GONE
            if (url.contains("/player/")) {
                Log.d(TAG, "Playing: $url")
                setupFullScreenMode()
                playVideoInFullScreen(view)
            } else {
                moveSearchInput(view)
                extractChannelNumbers()
                loadRecentChannels()
            }
        }

        fun extractChannelNumbers() {
            webView!!.evaluateJavascript(
                "Array.from(document.querySelectorAll('.card')).map(card => card.getAttribute('href').match(/\\/play\\/(\\d+)/)[1])"
            ) { it: String? ->
                var result = it
                if (!result.isNullOrEmpty()) {
                    result = result.replace("[", "").replace("]", "").replace("\"", "")
                    channelNumbers = listOf(
                        *result.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray())
                    Log.d(
                        TAG,
                        "Channel Numbers: $channelNumbers"
                    )
                }
            }
        }

        fun playVideoInFullScreen(view: WebView) {
            val width = "100vw"
            val height = "auto"
            val objectFit = "contain"

            val script = """
        javascript:(function() {
            try {
                var video = document.getElementsByTagName('video')[0];
                if (video) {
                    video.style.width = '$width';
                    video.style.height = '$height';
                    video.style.objectFit = '$objectFit';
                    video.play();
                } else {
                    console.error('No video element found');
                }
            } catch (e) {
                console.error('Error in full-screen script:', e);
            }
        })()
    """.trimIndent()

            view.evaluateJavascript(script, null)
        }




        fun moveSearchInput(view: WebView) {
            view.loadUrl(
                "javascript:(function() { " +
                        "var searchButton = document.getElementById('portexe-search-button'); " +
                        "var searchInput = document.getElementById('portexe-search-input'); " +
                        "if (searchButton && searchInput) { " +
                        "  searchButton.parentNode.insertBefore(searchInput, searchButton.nextSibling); " +
                        "} " +
                        "})()"
            )
        }
    }

    private fun injectTVChannel(channelName: String?, playId: String, logoUrl: String?) {
        val jsCode = "javascript:(function() {" +
                "console.log('Starting channel injection process...');" +
                "var channelGrid = document.querySelector('.grid.grid-cols-2');" +
                "console.log('Attempting to find the channel grid:', channelGrid);" +
                "if (channelGrid) {" +
                "  console.log('Channel grid found:', channelGrid);" +
                "  var existingChannel = document.querySelector('a[href=\"/play/" + playId + "\"]');" +
                "  console.log('Checking for existing channel with playId:', '" + playId + "');" +
                "  if (existingChannel) {" +
                "    console.log('Channel with playId ' + '" + playId + "' + ' already exists, skipping injection.');" +
                "  } else {" +
                "    console.log('Channel does not exist. Proceeding with channel injection...');" +
                "    var newChannel = document.createElement('a');" +
                "    newChannel.href = '/play/" + playId + "';" +
                "    newChannel.className = 'card border-2 border-gold shadow-lg hover:shadow-xl hover:bg-base-300 transition-all duration-200 ease-in-out scale-100 hover:scale-105';" +
                "    var cardContent = `<div class=\"flex flex-col items-center p-2 sm:p-4\">" +
                "      <img src=\"" + logoUrl + "\" loading=\"lazy\" alt=\"" + channelName + "\" class=\"h-14 w-14 sm:h-16 sm:w-16 md:h-18 md:w-18 lg:h-20 lg:w-20 rounded-full bg-gray-200\" />" +
                "      <span class=\"text-lg font-bold mt-2\">" + channelName + "</span>" +
                "      <div class=\"absolute top-2 right-2\">" +
                "        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"gold\" viewBox=\"0 -960 960 960\">" +
                "        <path d=\"M480-269 314-169q-11 7-23 6t-21-8q-9-7-14-17.5t-2-23.5l44-189-147-127q-10-9-12.5-20.5T140-571q4-11 12-18t22-9l194-17 75-178q5-12 15.5-18t21.5-6q11 0 21.5 6t15.5 18l75 178 194 17q14 2 22 9t12 18q4 11 1.5 22.5T809-528L662-401l44 189q3 13-2 23.5T690-171q-9 7-21 8t-23-6L480-269Z\"/>  " +
                "        </svg>" +
                "      </div>" +
                "    </div>`;" +
                "    newChannel.innerHTML = cardContent;" +
                "    channelGrid.insertBefore(newChannel, channelGrid.firstChild);" +
                "    console.log('Successfully injected new channel:', newChannel);" +
                "  }" +
                "} else {" +
                "  console.log('Failed to find the channel grid. Injection skipped.');" +
                "}" +
                "})()"

        webView!!.evaluateJavascript(jsCode, null)
        Log.d("ChannelInjection", "JavaScript code injected into the WebView.")
    }


    private fun saveRecentChannel(playId: String?, logoUrl: String?, channelName: String?) {
        // Load existing recent channels from preferenceManager
        val recentChannelsJson = prefManager.myPrefs.recentChannelsJson
        if (!recentChannelsJson.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(recentChannelsJson)
                recentChannels.clear()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    recentChannels.add(
                        Channel(
                            jsonObject.getString("playId"),
                            jsonObject.getString("logoUrl"),
                            jsonObject.getString("channelName")
                        )
                    )
                }
            } catch (e: JSONException) {
                Log.d(
                    TAG,
                    "Error loading recent channels: $e"
                )
            }
        }

        // Check if the channel with the given channelName already exists and remove it if found
        val iterator = recentChannels.iterator()
        while (iterator.hasNext()) {
            val channel = iterator.next()
            if (channel.channelName == channelName) {
                iterator.remove()
            }
        }

        recentChannels.add(0, Channel(playId, logoUrl, channelName))

        // Keep only the latest 5 channels
        if (recentChannels.size > 5) {
            recentChannels.removeAt(recentChannels.size - 1)
        }

        // Convert updated list to JSON array and save back to preferences
        val jsonArray = JSONArray()
        for (channel in recentChannels) {
            try {
                val jsonObject = JSONObject()
                jsonObject.put("playId", channel.playId)
                jsonObject.put("logoUrl", channel.logoUrl)
                jsonObject.put("channelName", channel.channelName)
                jsonArray.put(jsonObject)
            } catch (e: JSONException) {
                Log.d(TAG, e.toString())
            }
        }

        prefManager.myPrefs.recentChannelsJson = jsonArray.toString()
        prefManager.savePreferences()
    }

    private fun loadRecentChannels() {
        val channelData = prefManager.myPrefs.recentChannelsJson

        Log.d(
            TAG,
            "Channel Data from Shared Preferences: $channelData"
        )

        if (!channelData.isNullOrEmpty()) {
            recentChannels.clear() // Clear existing list
            try {
                val jsonArray = JSONArray(channelData)

                // Iterate in reverse
                for (i in jsonArray.length() - 1 downTo 0) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val playId = jsonObject.getString("playId")
                    val logoUrl = jsonObject.getString("logoUrl")
                    val channelName = jsonObject.getString("channelName")

                    // Log each channel's details to confirm parsing
                    Log.d(
                        TAG,
                        "Parsed Channel - Play ID: $playId, Logo URL: $logoUrl, Name: $channelName"
                    )

                    recentChannels.add(Channel(playId, logoUrl, channelName))
                }
            } catch (e: JSONException) {
                Log.e(TAG, "JSON parsing error in loadRecentChannels: " + e.message)
            }
        }

        for (channel in recentChannels) {
            val formattedPlayId = if (!channel.playId!!.endsWith("//")) {
                channel.playId + "//"
            } else {
                channel.playId
            }

            Log.d(
                TAG,
                "Injecting Channel into WebView - Name: ${channel.channelName}, Play ID: $formattedPlayId"
            )

            if (formattedPlayId != null) {
//                injectTVChannel(channel.channelName, formattedPlayId, channel.logoUrl)
            }
        }

    }
}
