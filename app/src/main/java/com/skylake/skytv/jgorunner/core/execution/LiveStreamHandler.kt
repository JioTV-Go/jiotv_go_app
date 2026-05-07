//package com.skylake.skytv.jgorunner.core.execution
//
//import android.app.Activity.CONNECTIVITY_SERVICE
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.LinkProperties
//import android.net.NetworkCapabilities
//import android.os.Build
//import android.util.Log
//import android.widget.Toast
//import com.arthenica.ffmpegkit.FFmpegKit
//import com.arthenica.ffmpegkit.ReturnCode
//import kotlinx.coroutines.*
//import java.net.Inet4Address
//
//private const val TAG = "LiveHandler-JGX"
//private const val CHROMECAST_PORT = 5349
//
//fun crosscode(
//    context: Context,
//    videoUrl: String,
//    onProcessingStart: () -> Unit,
//    onProcessingEnd: () -> Unit
//) {
//    val scope = CoroutineScope(Dispatchers.IO)
//
//    scope.launch {
//        val ip = getLocalIp(context)
//        val finalUrl = prepareUrl(videoUrl, ip)
//        val outputUrl = "http://$ip:$CHROMECAST_PORT/"
//
//        Log.d(TAG, "Output URL: $outputUrl")
//
//        startFFmpegSession(
//            context = context,
//            inputUrl = finalUrl,
//            outputUrl = outputUrl,
//            onProcessingStart = onProcessingStart,
//            onProcessingEnd = {
//                castMediaPlayer(context, outputUrl)
//                onProcessingEnd()
//            }
//        )
//    }
//}
//
//private suspend fun startFFmpegSession(
//    context: Context,
//    inputUrl: String,
//    outputUrl: String,
//    onProcessingStart: () -> Unit,
//    onProcessingEnd: () -> Unit
//) {
//    withContext(Dispatchers.Main) { onProcessingStart() }
//
//    var lastLogTime = System.currentTimeMillis()
//    var restarted = false
//
//    FFmpegKit.executeAsync(
//        "-i $inputUrl -c copy -f mp4 -movflags frag_keyframe+empty_moov -listen 1 -bsf:a aac_adtstoasc $outputUrl",
//
//        { session ->
//            when {
//                ReturnCode.isSuccess(session.returnCode) -> {
//                    Log.d(TAG, "FFmpeg SUCCESS")
//                }
//
//                ReturnCode.isCancel(session.returnCode) -> {
//                    Log.d(TAG, "FFmpeg CANCELLED")
//                }
//
//                else -> {
//                    Log.e(TAG, "FFmpeg FAILED: ${session.failStackTrace}")
//                }
//            }
//        },
//
//        { log ->
//            Log.d("FFmpeg Log", log.message)
//
//            lastLogTime = System.currentTimeMillis()
//
//            if (!restarted && log.message.contains("Address already in use", true)) {
//                restarted = true
//
//                CoroutineScope(Dispatchers.Main).launch {
//                    Toast.makeText(
//                        context,
//                        "Port in use. Restarting...",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//
//                FFmpegKit.cancel()
//
//                CoroutineScope(Dispatchers.IO).launch {
//                    delay(500)
//                    startFFmpegSession(
//                        context,
//                        inputUrl,
//                        outputUrl,
//                        onProcessingStart,
//                        onProcessingEnd
//                    )
//                }
//            }
//        },
//        {}
//    )
//
//    // Detect "stream ready"
//    while (true) {
//        delay(50)
//
//        if (System.currentTimeMillis() - lastLogTime > 3000) {
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, "Playing Live Stream", Toast.LENGTH_SHORT).show()
//                Log.d(TAG, "Stream ready → casting")
//
//                onProcessingEnd()
//            }
//            break
//        }
//    }
//}
//
//private fun prepareUrl(url: String, ip: String): String {
//    val withSuffix = if (url.endsWith(".m3u8")) url else "$url.m3u8"
//    return withSuffix.replace("localhost", ip)
//}
//
//fun getLocalIp(context: Context): String {
//    val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//
//    val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//        cm.activeNetwork
//    } else {
//        @Suppress("DEPRECATION")
//        cm.allNetworks.firstOrNull()
//    }
//
//    val capabilities = cm.getNetworkCapabilities(network)
//    val isValidNetwork = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
//            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
//
//    if (!isValidNetwork) return "0.0.0.0"
//
//    val linkProperties: LinkProperties = cm.getLinkProperties(network) ?: return "0.0.0.0"
//
//    return linkProperties.linkAddresses
//        .mapNotNull { it.address }
//        .filterIsInstance<Inet4Address>()
//        .firstOrNull()
//        ?.hostAddress ?: "0.0.0.0"
//}