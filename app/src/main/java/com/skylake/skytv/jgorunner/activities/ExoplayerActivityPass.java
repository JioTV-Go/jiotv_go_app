package com.skylake.skytv.jgorunner.activities;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.skylake.skytv.jgorunner.R;
import com.skylake.skytv.jgorunner.data.SkySharedPref;

import java.util.List;
import java.util.Locale;

public class ExoplayerActivityPass extends ComponentActivity {

    private static final String TAG = "EPAP-DIX";
    private static final String DEFAULT_VIDEO_URL = "http://localhost:5350/live/143.m3u8";
    private static final int HIDE_CONTROLS_DELAY_MS = 1000;
    private static final int SHOW_CHANNEL_INFO_DURATION_MS = 3000;

    private ExoPlayer player;
    private PlayerView playerView;
    private boolean isInPipMode = false;

    private MediaSessionCompat mediaSession;

    //    private SkySharedPref skyPref;
    SkySharedPref skyPref = SkySharedPref.getInstance(this);
    private String filterQX;
    private String tv_NAV;

    private LinearLayout floatingChannelInfoLayout;
    private ImageView channelLogoImageView;
    private TextView channelNameTextView;

    private String CHANNEL_NAME_TEXT = "HANA4k";
    private String CHANNEL_LOGO_URL = "https://www.sonypicturesnetworks.com/images/logos/SET%20LOGO.png";
    private TextView channelNumberTextView;


    private List<ChannelInfo> channelList;
    private int currentChannelIndex = -1;

    private boolean isControllerActuallyVisible = false;
    private boolean playWhenReady = true;
    private String lastKnownVideoUrl;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isTelevision(Context context) {
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK_ONLY));
    }


    private Handler channelInfoHandler = new Handler(Looper.getMainLooper());
    private Runnable hideChannelInfoRunnable = () -> {
        if (floatingChannelInfoLayout != null) {
            floatingChannelInfoLayout.setVisibility(View.GONE);
        }
    };

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updatePictureInPictureParams();
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setImmersiveMode();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();



        ////////////////////////
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (isInPipMode) {
                    finish();
                } else {
                    if (playerView != null && isControllerActuallyVisible) {
                        playerView.hideController();
                    } else {
                        cleanupBeforeExit();

                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
        ////////////////////////

        channelList = intent.getParcelableArrayListExtra("channel_list_data");
        currentChannelIndex = intent.getIntExtra("current_channel_index", -1);

        String initialVideoUrl;
        String signatureFallback = intent.getStringExtra("zone");
        signatureFallback = (signatureFallback == null || signatureFallback.isEmpty()) ? "0x0" : signatureFallback;

        if (channelList != null && !channelList.isEmpty() && currentChannelIndex >= 0 && currentChannelIndex < channelList.size()) {
            ChannelInfo currentChannel = channelList.get(currentChannelIndex);
            initialVideoUrl = currentChannel.getVideoUrl();
            CHANNEL_LOGO_URL = currentChannel.getLogoUrl();
            CHANNEL_NAME_TEXT = currentChannel.getChannelName();
            Log.d(TAG, "Loaded initial channel from list: " + CHANNEL_NAME_TEXT + " at index " + currentChannelIndex);
        } else {
            Log.w(TAG, "Channel list not available or index invalid. Using fallback extras.");
            initialVideoUrl = intent.getStringExtra("video_url");
            CHANNEL_LOGO_URL = intent.getStringExtra("logo_url");
            CHANNEL_NAME_TEXT = intent.getStringExtra("ch_name");
            if (initialVideoUrl == null || initialVideoUrl.isEmpty()) {
                initialVideoUrl = DEFAULT_VIDEO_URL;
            }
            if (CHANNEL_LOGO_URL == null) CHANNEL_LOGO_URL = "";
            if (CHANNEL_NAME_TEXT == null) CHANNEL_NAME_TEXT = "Unknown Channel";
        }

        /// Saving Current Channel Data-
        skyPref.getMyPrefs().setCurrChannelName(CHANNEL_NAME_TEXT);
        skyPref.getMyPrefs().setCurrChannelLogo(CHANNEL_LOGO_URL);
        skyPref.getMyPrefs().setCurrChannelUrl(initialVideoUrl);
        skyPref.savePreferences();

        ///


        skyPref.getMyPrefs().setCurrChannelName(CHANNEL_NAME_TEXT);
        skyPref.savePreferences();

        setContentView(R.layout.activity_web_player_exo);

        playerView = findViewById(R.id.player_view);
        floatingChannelInfoLayout = findViewById(R.id.floating_channel_info_layout);
        channelLogoImageView = findViewById(R.id.channel_logo_imageview);
        channelNameTextView = findViewById(R.id.channel_name_textview);
        channelNumberTextView = findViewById(R.id.channel_number_textview);

        // Set up controller visibility listener
        playerView.setControllerVisibilityListener((PlayerControlView.VisibilityListener) visibility -> {
            isControllerActuallyVisible = (visibility == View.VISIBLE);
            Log.d(TAG, "Controller visibility changed: " + (isControllerActuallyVisible ? "Visible" : "Hidden"));
        });

        updateChannelInfoDisplay();

        skyPref = SkySharedPref.getInstance(this);
        filterQX = skyPref.getMyPrefs().getFilterQX();
        tv_NAV = skyPref.getMyPrefs().getSelectedRemoteNavTV();

        String formattedUrl = formatVideoUrl(initialVideoUrl, signatureFallback);
        Log.d(TAG, "Formatted URL for initial playback: " + formattedUrl);

        lastKnownVideoUrl = formattedUrl;
        setupPlayer(formattedUrl);
        hideControlsAfterDelay();
    }

    private void updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pipBuilder.setAutoEnterEnabled(true);
            }
            setPictureInPictureParams(pipBuilder.build());
        }
    }

    private void updateChannelInfoDisplay() {
        if (channelNameTextView != null) {
            channelNameTextView.setText(CHANNEL_NAME_TEXT);
        }
        if (channelLogoImageView != null && CHANNEL_LOGO_URL != null && !CHANNEL_LOGO_URL.isEmpty()) {
            Glide.with(this)
                    .load(CHANNEL_LOGO_URL)
                    .into(channelLogoImageView);
        } else if (channelLogoImageView != null) {
            channelLogoImageView.setImageDrawable(null);
        }

        if (channelNumberTextView != null) {
            int displayNumber = currentChannelIndex + 1;
            String numberFormatted = String.format(Locale.US, "%02d", displayNumber);
            channelNumberTextView.setText(numberFormatted);
        }
    }


    private void setImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setupPlayer(String videoUrl) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setShowNextButton(false);
            playerView.setShowPreviousButton(false);
            playerView.setControllerAutoShow(false);
            playerView.hideController();
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC);

            // 💡 Initialize MediaSession
            initMediaSession();
        }

        // Initialize media source
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        MediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)));

        player.setMediaSource(hlsMediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        // Show floating info
        showAndHideChannelInfoBox();
    }

    private void initMediaSession() {
        if (mediaSession == null) {
            mediaSession = new MediaSessionCompat(this, "playerSession");
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setMediaButtonReceiver(null); // We handle events manually

            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                    KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        int keyCode = keyEvent.getKeyCode();
                        Log.d("MediaSession", "Media key event: " + keyCode);

                        switch (keyCode) {
                            case KeyEvent.KEYCODE_MEDIA_PLAY:
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            case KeyEvent.KEYCODE_DPAD_CENTER:
                            case KeyEvent.KEYCODE_ENTER:
                                if (player.isPlaying()) {
                                    player.pause();
                                } else {
                                    player.play();
                                }
                                return true;

                            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                playPreviousChannel();
                                return true;

                            case KeyEvent.KEYCODE_CHANNEL_UP:
                            case KeyEvent.KEYCODE_DPAD_UP:
                                playNextChannel();
                                return true;
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent);
                }
            });

            mediaSession.setActive(true);
        }
    }



//    private void showAndHideChannelInfoBox() {
//        if (floatingChannelInfoLayout != null) {
//            floatingChannelInfoLayout.setVisibility(View.VISIBLE);
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if (floatingChannelInfoLayout != null) {
//                    floatingChannelInfoLayout.setVisibility(View.GONE);
//                }
//            }, SHOW_CHANNEL_INFO_DURATION_MS);
//        }
//    }




    private void showAndHideChannelInfoBox() {
        if (floatingChannelInfoLayout != null) {
            floatingChannelInfoLayout.setVisibility(View.VISIBLE);
            channelInfoHandler.removeCallbacks(hideChannelInfoRunnable);
            channelInfoHandler.postDelayed(hideChannelInfoRunnable, SHOW_CHANNEL_INFO_DURATION_MS);
        }
    }


    private String formatVideoUrl(String videoUrl, String signatureFallback) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return DEFAULT_VIDEO_URL;
        }
        String tempVideoUrl = videoUrl;
        if (tempVideoUrl.contains("q=low")) {
            tempVideoUrl = tempVideoUrl.replace("/live/", "/live/low/");
        } else if (tempVideoUrl.contains("q=high")) {
            tempVideoUrl = tempVideoUrl.replace("/live/", "/live/high/");
        } else if (tempVideoUrl.contains("q=medium")) {
            tempVideoUrl = tempVideoUrl.replace("/live/", "/live/medium/");
        }
        if ("TV".equals(signatureFallback) && !"auto".equals(filterQX)) {
            Log.d(TAG, "Exo- TV- Redirect- Quality: " + filterQX);
            switch (filterQX) {
                case "low": tempVideoUrl = tempVideoUrl.replace("/live/", "/live/low/"); break;
                case "high": tempVideoUrl = tempVideoUrl.replace("/live/", "/live/high/"); break;
                case "medium": tempVideoUrl = tempVideoUrl.replace("/live/", "/live/medium/"); break;
            }
        }
        if (tempVideoUrl.contains(".m3u8")) {
            int questionMarkIndex = tempVideoUrl.indexOf("?");
            if (questionMarkIndex != -1) {
                tempVideoUrl = tempVideoUrl.substring(0, questionMarkIndex);
            }
        }
        tempVideoUrl = tempVideoUrl.replace("//.m3u8", ".m3u8");
        return tempVideoUrl;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void hideControlsAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> { if (playerView != null) playerView.hideController(); },
                HIDE_CONTROLS_DELAY_MS
        );
    }

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint({"RestrictedApi", "NewApi"})
    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (player == null) {
            return super.dispatchKeyEvent(event);
        }

        Log.d("HAN",tv_NAV);

        if (tv_NAV == null) {
            return super.dispatchKeyEvent(event);
        }

        if ("-1".equals(tv_NAV)) {
            return super.dispatchKeyEvent(event);
        }

        if (isInPictureInPictureMode()) {
            return super.dispatchKeyEvent(event);
        }


        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (tv_NAV) {
                case "0":
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_CHANNEL_DOWN:
                            playPreviousChannel();
                            if (playerView != null) playerView.hideController();
                            return true;
                        case KeyEvent.KEYCODE_CHANNEL_UP:
                            playNextChannel();
                            if (playerView != null) playerView.hideController();
                            return true;
                    }
                    break;
                case "1":
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            playPreviousChannel();
                            if (playerView != null) playerView.hideController();
                            return true;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            playNextChannel();
                            if (playerView != null) playerView.hideController();
                            return true;
                    }
                    break;
            }
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (playerView != null) {
                        if (isControllerActuallyVisible) {
                            playerView.hideController();
                        } else {
                            playerView.showController();
                        }
                    }
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }



    private void playNextChannel() {
        if (channelList == null || channelList.isEmpty()) {
            Log.d(TAG, "Channel list empty, cannot switch next.");
            Toast.makeText(this, "No next channel.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentChannelIndex++;
        if (currentChannelIndex >= channelList.size()) {
            currentChannelIndex = 0;
        }
        Log.d(TAG, "Switching to next channel, index: " + currentChannelIndex);
        switchChannel();
    }

    private void playPreviousChannel() {
        if (channelList == null || channelList.isEmpty()) {
            Log.d(TAG, "Channel list empty, cannot switch previous.");
            Toast.makeText(this, "No previous channel.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentChannelIndex--;
        if (currentChannelIndex < 0) {
            currentChannelIndex = channelList.size() - 1;
        }
        Log.d(TAG, "Switching to previous channel, index: " + currentChannelIndex);
        switchChannel();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void switchChannel() {
        if (channelList == null || currentChannelIndex < 0 || currentChannelIndex >= channelList.size()) {
            Log.e(TAG, "Cannot switch channel, invalid list or index.");
            return;
        }

        ChannelInfo newChannel = channelList.get(currentChannelIndex);
        CHANNEL_NAME_TEXT = newChannel.getChannelName();
        CHANNEL_LOGO_URL = newChannel.getLogoUrl();

        Log.d(TAG, "Loading new channel: " + CHANNEL_NAME_TEXT + " URL: " + newChannel.getVideoUrl());

        updateChannelInfoDisplay();

        String signatureFallback = getIntent().getStringExtra("zone");
        signatureFallback = (signatureFallback == null || signatureFallback.isEmpty()) ? "0x0" : signatureFallback;
        String formattedUrl = formatVideoUrl(newChannel.getVideoUrl(), signatureFallback);

        if (playerView != null) playerView.hideController();

        if (player != null) {
            player.stop();
            lastKnownVideoUrl = formattedUrl;
            setupPlayer(formattedUrl);
        } else {
            Log.e(TAG, "Player is null, cannot switch channel.");
            lastKnownVideoUrl = formattedUrl;
            setupPlayer(formattedUrl);
        }
        if (playerView != null) playerView.hideController();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            playerView.setUseController(true);
            player.setPlayWhenReady(true);
        }
        setImmersiveMode();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updatePictureInPictureParams();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player == null) {
            setupPlayer(lastKnownVideoUrl);
            player.setPlayWhenReady(playWhenReady);
        }
    }


    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        super.onPause();
        if (!isInPictureInPictureMode() && player != null) {
            playWhenReady = player.getPlayWhenReady();
//            releasePlayer();
            cleanupBeforeExit();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStop() {
        super.onStop();
        if (!isInPictureInPictureMode() && player != null) {
//            releasePlayer();
            cleanupBeforeExit();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            Log.d(TAG, "Player released");
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isTelevision(this)) {
                // No PiP on TV
                return;
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && player != null && player.isPlaying()) {
            resetSystemUIVisibility();
            enterPipModeManually();
        }
    }

    private void enterPipModeManually() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isTelevision(this)) {
                Log.d(TAG, "Device is TV, skipping PiP mode");
                return;
            }
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio);
            enterPictureInPictureMode(pipBuilder.build());
            playerView.clearFocus();
            isInPipMode = true;
        }
    }


    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent received. Re-processing intent.");
        releasePlayer();
        setIntent(intent);

        channelList = intent.getParcelableArrayListExtra("channel_list_data");
        currentChannelIndex = intent.getIntExtra("current_channel_index", -1);

        String newVideoUrl;
        String signatureFallback = intent.getStringExtra("zone");
        signatureFallback = (signatureFallback == null || signatureFallback.isEmpty()) ? "0x0" : signatureFallback;

        if (channelList != null && !channelList.isEmpty() && currentChannelIndex >= 0 && currentChannelIndex < channelList.size()) {
            ChannelInfo currentChannel = channelList.get(currentChannelIndex);
            newVideoUrl = currentChannel.getVideoUrl();
            CHANNEL_LOGO_URL = currentChannel.getLogoUrl();
            CHANNEL_NAME_TEXT = currentChannel.getChannelName();
        } else {
            newVideoUrl = intent.getStringExtra("video_url");
            CHANNEL_LOGO_URL = intent.getStringExtra("logo_url");
            CHANNEL_NAME_TEXT = intent.getStringExtra("ch_name");
            if (newVideoUrl == null || newVideoUrl.isEmpty()) newVideoUrl = DEFAULT_VIDEO_URL;
            if (CHANNEL_LOGO_URL == null) CHANNEL_LOGO_URL = "";
            if (CHANNEL_NAME_TEXT == null) CHANNEL_NAME_TEXT = "Unknown Channel";
        }
        updateChannelInfoDisplay();
        String formattedUrl = formatVideoUrl(newVideoUrl, signatureFallback);
        setupPlayer(formattedUrl);
        hideControlsAfterDelay();
    }

    private void createPIPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isTelevision(this)) {
                Toast.makeText(this, "PiP not supported on TV devices", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (floatingChannelInfoLayout != null) {
                floatingChannelInfoLayout.setVisibility(View.GONE);
            }
            if (playerView != null && playerView.getWidth() > 0 && playerView.getHeight() > 0) {
                Rational aspectRatio = new Rational(16, 9);
                try {
                    PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
                    pipBuilder.setAspectRatio(aspectRatio);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        pipBuilder.setAutoEnterEnabled(true);
                    }
                    enterPictureInPictureMode(pipBuilder.build());
                    isInPipMode = true;
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error entering PiP mode: " + e.getMessage());
                    Toast.makeText(this, "Could not enter PiP mode.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "PlayerView not ready for PiP mode (width/height is 0).");
            }
        } else {
            Toast.makeText(this, "PiP mode is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPiPMode, newConfig);
        this.isInPipMode = isInPiPMode;
        if (isInPiPMode) {
            playerView.setUseController(false);
            if (floatingChannelInfoLayout != null) {
                floatingChannelInfoLayout.setVisibility(View.GONE);
            }
        } else {
            if (player != null) {
                player.setPlayWhenReady(false);
                player.stop();
                releasePlayer();
            }

            finish();

            playerView.setUseController(true);
            setImmersiveMode();
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveMode();
            playerView.requestFocus();
        }
    }


    private void resetSystemUIVisibility() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE
        );
    }

    private void cleanupBeforeExit() {
        resetSystemUIVisibility();


        if (channelInfoHandler != null) {
            channelInfoHandler.removeCallbacksAndMessages(null);
        }


        if (playerView != null) {
            playerView.setPlayer(null);
            playerView.setUseController(false);
        }

        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
            Log.d(TAG, "MediaSession released");
        }


        if (player != null) {
            player.release();
            player = null;
            Log.d(TAG, "Player released in cleanupBeforeExit()");
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

//    @OptIn(markerClass = UnstableApi.class)
//    @Override
//    public void onBackPressed() {
//        if (isInPipMode) {
//            finish();
//        } else {
//            if (playerView != null && isControllerActuallyVisible) {
//                playerView.hideController();
//            } else {
//                releasePlayer();
//                super.onBackPressed();
//            }
//        }
//    }


}

