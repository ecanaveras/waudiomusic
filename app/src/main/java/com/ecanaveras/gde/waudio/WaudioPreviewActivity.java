package com.ecanaveras.gde.waudio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.ecanaveras.gde.waudio.controllers.WaudioController;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;

public class WaudioPreviewActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    public static final String IS_TEMPLATE = "is_template";
    public static String PATH_WAUDIO = "path_waudio";
    public static String IS_WAUDIO = "is_waudio";

    private FirebaseAnalytics mFirebaseAnalytics;
    private DataFirebaseHelper mDataFirebaseHelper;

    private File wFile;
    private VideoView videoView;
    private AudioManager audioManager;
    private LinearLayout layoutButtons, layoutTemplateButtons;
    private RelativeLayout layoutWaudioButtons;
    private TextView txtTitle;
    private WaudioController waudioController;
    private boolean isTemplate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waudio_preview);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mDataFirebaseHelper = new DataFirebaseHelper();


        videoView = (VideoView) findViewById(R.id.videoView);
        txtTitle = (TextView) findViewById(R.id.title);
        layoutButtons = (LinearLayout) findViewById(R.id.layoutButtons);
        layoutWaudioButtons = (RelativeLayout) findViewById(R.id.layoutWaudioButtons);
        layoutTemplateButtons = (LinearLayout) findViewById(R.id.layoutTemplateButtons);

        //Maneja el audio en llamadas
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        Intent intent = getIntent();
        if (intent.getBooleanExtra(IS_WAUDIO, false)) {
            layoutButtons.setVisibility(View.VISIBLE);
            layoutWaudioButtons.setVisibility(View.VISIBLE);
        }
        if (intent.getBooleanExtra(IS_TEMPLATE, false)) {
            isTemplate = true;
            layoutButtons.setVisibility(View.VISIBLE);
            layoutTemplateButtons.setVisibility(View.VISIBLE);
        }

        if (intent != null) {
            wFile = new File(intent.getStringExtra(PATH_WAUDIO));
            waudioController = new WaudioController(this, wFile);
            setupWaudioVideo(wFile);
        }
    }

    private void setupWaudioVideo(File f) {
        if (f == null) {
            finish();
        }
        txtTitle.setText(f.getName().split("\\.")[0].toUpperCase());
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

    public void onShare(View view) {
        if (wFile != null) {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
            waudioController.onShare();
        }

    }

    public void onDelete(View v) {
        if (videoView.isPlaying()) {
            videoView.pause();
        }
        if (isTemplate) {
            waudioController.onDeleteTemplate();
        } else {
            waudioController.onDelete();
        }
    }

    public void onGoFile(View view) {
        waudioController.onGoFile();
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
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null)
            audioManager.abandonAudioFocus(this);
    }
}
