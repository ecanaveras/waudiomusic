package com.ecanaveras.gde.waudio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;

public class WaudioPreviewActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    public static String PATH_WAUDIO = "path_waudio";

    private File wFile;
    private VideoView videoView;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waudio_preview);

        //Maneja el audio en llamadas
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        videoView = (VideoView) findViewById(R.id.videoView);

        Intent intent = getIntent();
        if (intent != null) {
            wFile = new File(intent.getStringExtra(PATH_WAUDIO));
            setupWaudioVideo(wFile);
        }
    }

    private void setupWaudioVideo(File f) {
        MediaController controller = new MediaController(this);
        controller.setAnchorView(controller);
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.fromFile(f));
        videoView.setKeepScreenOn(true);
        videoView.requestFocus();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                finish();
            }
        });
        videoView.start();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (videoView.isPlaying())
            outState.putInt("pos", videoView.getCurrentPosition()); // save it here
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        videoView.seekTo(savedInstanceState.getInt("pos"));
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //mediaPlayer.start(); // Resume your media player here
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (videoView.isPlaying())
                    videoView.pause();// Pause your media player here
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
