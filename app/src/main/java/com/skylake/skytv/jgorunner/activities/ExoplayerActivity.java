package com.skylake.skytv.jgorunner.activities;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.skylake.skytv.jgorunner.R;

import java.util.Arrays;
import java.util.List;

public class ExoplayerActivity extends ComponentActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private boolean isInPipMode = false;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_web_player_exo);

        playerView = findViewById(R.id.player_view);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        Intent intent = getIntent();
        String videoUrl = intent.getStringExtra("video_url");
        String current_play_id = intent.getStringExtra("current_play_id");
        String[] channelNumbersArray = getIntent().getStringArrayExtra("channels_list");
        List<String> channelNumbers = channelNumbersArray != null ? Arrays.asList(channelNumbersArray) : null;
        // For future usage


        assert videoUrl != null;
        if (videoUrl.isEmpty()) {
            videoUrl = "http://localhost:5350/live/143.m3u8";
        }

        String formattedUrl = formatVideoUrl(videoUrl);

        Log.d("DIX", formattedUrl);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();

        MediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(Uri.parse(formattedUrl)));

        player.setMediaSource(hlsMediaSource);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        player.prepare();
        player.setPlayWhenReady(true);

        hideControlsAfterDelay();
    }

    private String formatVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }

        // Replace the "/live/" segment and adjust based on quality
        if (videoUrl.contains("q=low")) {
            videoUrl = videoUrl.replace("/live/", "/live/low/");
        } else if (videoUrl.contains("q=high")) {
            videoUrl = videoUrl.replace("/live/", "/live/high/");
        } else if (videoUrl.contains("q=medium")) {
            videoUrl = videoUrl.replace("/live/", "/live/medium/");
        }

        // Remove query parameters if URL contains ".m3u8" *Intentionally removed .m3u8
        if (videoUrl.contains(".m3u8")) {
            int questionMarkIndex = videoUrl.indexOf("?");
            if (questionMarkIndex != -1) {
                videoUrl = videoUrl.substring(0, questionMarkIndex);
            }
        }

        // Fix potential issue with "//.m3u8"
        videoUrl = videoUrl.replace("//.m3u8", ".m3u8");

        return videoUrl;
    }


    @OptIn(markerClass = UnstableApi.class)
    private void hideControlsAfterDelay() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> playerView.hideController(), 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!isInPipMode) {
            createPIPMode();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        finish();
        startActivity(intent);

    }

    private void createPIPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
            pipBuilder.setAspectRatio(aspectRatio);

            enterPictureInPictureMode(pipBuilder.build());
            isInPipMode = true;
        } else {
            Toast.makeText(this, "PiP mode is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPiPMode, newConfig);
        isInPipMode = isInPiPMode;
    }

}
