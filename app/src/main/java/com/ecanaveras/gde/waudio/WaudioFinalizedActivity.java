package com.ecanaveras.gde.waudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ecanaveras.gde.waudio.controllers.WaudioController;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.Random;

import es.dmoral.toasty.Toasty;

public class WaudioFinalizedActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    private static final int SHARE_WAUDIO_REQUEST = 1;

    public static final String TEMPLATE_USED = "template_used";
    private FirebaseAnalytics mFirebaseAnalytics;
    private DataFirebaseHelper mDataFirebaseHelper;

    private String pathWaudio;
    private VideoView videoView;
    private LinearLayout lw;
    private LinearLayout lp;
    private MediaController mediaController;
    private MainApp app;
    private Handler handler;
    private TextView lblTitleWaudio, lblPathWaudio;
    private Intent intent;
    private boolean goBack;
    private boolean addPoints = true;
    private int position = 0;
    private AudioManager audioManager;
    private String templateUsed;
    private WaudioController waudioController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waudio_finalized);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mDataFirebaseHelper = new DataFirebaseHelper();

        //Maneja el audio en llamadas
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        app = (MainApp) getApplicationContext();
        intent = getIntent();
        handler = new Handler();

        videoView = (VideoView) findViewById(R.id.videoView);
        //lblTitleWaudio = (TextView) findViewById(R.id.lblTitleWaudio);
        //lblPathWaudio = (TextView) findViewById(R.id.lblPathWaudio);
        //mediaController = (MediaController) findViewById(R.id.mediaController);

        lp = (LinearLayout) findViewById(R.id.layoutPreview);
        lw = (LinearLayout) findViewById(R.id.layoutWait);

        //Oculta el layout principal y muestra el loading...
        lp.setVisibility(View.GONE);
        lw.setVisibility(View.VISIBLE);

        //Muestra el preview mientras comprueba que existe el waudio
        checkWaudio();
    }


    private void checkWaudio() {
        //TODO Controlar que esta busqueda no sea infinita
        Log.i("Checking W Generation", "CHECK");

        if (intent.getStringExtra("waudio") != null) {
            pathWaudio = intent.getStringExtra("waudio");
            addPoints = intent.getBooleanExtra("points", false);
        }
        if (pathWaudio == null && app.getGeneratorWaudio() != null && app.getGeneratorWaudio().getOutFileWaudio() != null) {
            pathWaudio = app.getGeneratorWaudio().getOutFileWaudio().getAbsolutePath();
        }

        if (pathWaudio == null && app.getGeneratorWaudio() == null) {
            onGoHome(null);
            return;
        }

        if (pathWaudio == null) {
            handler.postDelayed(new Runnable() {//Check cada 1 segundos
                @Override
                public void run() {
                    checkWaudio();
                }
            }, 1000);
        } else {
            templateUsed = intent.getStringExtra(TEMPLATE_USED);
            loadWaudio();
            lp.setVisibility(View.VISIBLE);
            lw.setVisibility(View.GONE);
            Log.i("Waudio in preview", pathWaudio);
            return;
        }

    }


    private void loadWaudio() {
        if (pathWaudio != null) {
            File f = new File(pathWaudio);
            waudioController = new WaudioController(this, f, false);
            if (f.exists()) {
                getSupportActionBar().setTitle("WAUDIO - " + f.getName().toUpperCase());
                MediaController controller = new MediaController(this);
                controller.setAnchorView(videoView);
                videoView.setMediaController(controller);
                videoView.setVideoURI(Uri.fromFile(f));
                videoView.setVideoURI(Uri.fromFile(f));
                videoView.setKeepScreenOn(true);
                videoView.requestFocus();
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        audioManager.requestAudioFocus(WaudioFinalizedActivity.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                        videoView.start();
                    }
                });
                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        audioManager.abandonAudioFocus(WaudioFinalizedActivity.this);
                    }
                });
                if (addPoints)
                    waudioController.addPoints(MainApp.POINTS_WAUDIO_CREATED);
                //lblPathWaudio.setText(f.getPath().replace(f.getName(), ""));
                //lblTitleWaudio.setText(f.getName());
            } else {
                lp.setVisibility(View.GONE);
                app.removeWaudio(app.getCompareWaudioTmp());
                showBackAlert(getString(R.string.msgWaudioNoFound), getString(R.string.msgWaudioCreate), getString(R.string.msgChooseStyle), false);
            }
        }
    }

    public void onShare(View view) {
        if (waudioController != null) {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
            waudioController.onShare(this);
        }
    }

    public void onGoEditor(View view) {
        Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(app.getFilename()));
        intent.putExtra("continue_edition", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setClassName("com.ecanaveras.gde.waudio", "com.ecanaveras.gde.waudio.EditorActivity");
        //startActivityForResult(intent, REQUEST_CODE_EDIT);
        startActivity(intent);
    }

    public void onGoFile(View view) {
        if (waudioController != null)
            waudioController.onGoFile();
    }

    public void onDelete(View view) {
        if (waudioController != null)
            waudioController.onDelete();
    }

    private void showBackAlert(String title, String message, String textPositive, boolean cancelable) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        textPositive,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                //app.getGeneratorWaudio().setOutFileWaudio(null);
                                finish();
                            }
                        })
                .setCancelable(cancelable);
        if (cancelable) {
            alertDialog.setNegativeButton("NO", null);
        }
        alertDialog.show();
    }

    public void onNewWaudio(View view) {
        Intent intent = new Intent(this, ListAudioActivity.class);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }

    public void onGoHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHARE_WAUDIO_REQUEST && resultCode == Activity.RESULT_OK && waudioController != null) {
            waudioController.addPoints(MainApp.POINTS_WAUDIO_SHARED);
            mFirebaseAnalytics.setUserProperty("shared", String.valueOf(true));
            mDataFirebaseHelper.incrementWaudioShared();
        }
    }

    public void onFinish(View view) {
        int[] idsMsgs = {R.string.msgFinish1,
                R.string.msgFinish2,
                R.string.msgFinish3,
                R.string.msgFinish4,
                R.string.msgFinish5,
                R.string.msgFinish6};
        Random random = new Random();
        Toasty.info(this, getString(idsMsgs[random.nextInt(idsMsgs.length)]), Toast.LENGTH_LONG).show();
        finishAffinity();
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onBackPressed() {
        if (videoView.isPlaying())
            videoView.pause();
        if (goBack) {
            super.onBackPressed();
        } else {
            finish();
            //showBackAlert(getString(R.string.alert_title_choose_style), getString(R.string.msgWarningStyle), getString(R.string.alert_ok_choose_style), true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioManager.abandonAudioFocus(this);
    }

    // When you change direction of phone, this method will be called.
    // It store the state of video (Current position)
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // Store current position.
        savedInstanceState.putInt("CurrentPosition", videoView.getCurrentPosition());
        videoView.pause();
    }


    // After rotating the phone. This method is called.
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Get saved position.
        position = savedInstanceState.getInt("CurrentPosition");
        videoView.seekTo(position);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }
}
