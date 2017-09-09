package com.ecanaveras.gde.waudio;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.Random;

public class WaudioFinalizedActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

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
    private int position = 0;
    private AudioManager audioManager;
    private String templateUsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mDataFirebaseHelper = new DataFirebaseHelper();

        //Maneja el audio en llamadas
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);


        setContentView(R.layout.activity_waudio_finalized);

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
        System.out.println("checking....");
        if (intent.getStringExtra("waudio") != null) {
            pathWaudio = intent.getStringExtra("waudio");
        }
        if (pathWaudio == null && app.getGeneratorWaudio() != null && app.getGeneratorWaudio().getOutFileWaudio() != null) {
            pathWaudio = app.getGeneratorWaudio().getOutFileWaudio().getAbsolutePath();
        }

        if (pathWaudio == null) {
            handler.postDelayed(new Runnable() {//Check cada 2 segundos
                @Override
                public void run() {
                    checkWaudio();
                }
            }, 1000);
        }

        if (pathWaudio != null) {
            templateUsed = intent.getStringExtra(TEMPLATE_USED);
            loadWaudio();
            lp.setVisibility(View.VISIBLE);
            lw.setVisibility(View.GONE);
            System.out.println("Waudio in preview: " + pathWaudio);
            return;
        }

    }


    private void loadWaudio() {
        if (pathWaudio != null) {
            File f = new File(pathWaudio);
            if (f.exists()) {
                getSupportActionBar().setTitle("Waudio - " + f.getName().toLowerCase());
                //Toast.makeText(this, getResources().getString(R.string.msgWaudioSuccess), Toast.LENGTH_SHORT).show();
                MediaController controller = new MediaController(this);
                controller.setAnchorView(videoView);
                videoView.setMediaController(controller);
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
                //lblPathWaudio.setText(f.getPath().replace(f.getName(), ""));
                //lblTitleWaudio.setText(f.getName());
            } else {
                lp.setVisibility(View.GONE);
                app.removeWaudio(app.getCompareWaudioTmp());
                showBackAlert(getResources().getString(R.string.msgWaudioNoFound), getResources().getString(R.string.msgWaudioCreate), getResources().getString(R.string.msgChooseStyle), false);
            }
        }
    }

    public void onShare(View view) {
        if (pathWaudio != null) {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pathWaudio));
            sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.hastag));
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("video/mp4");
            startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.msgShareWith)));

            mFirebaseAnalytics.setUserProperty("shared", String.valueOf(true));
            mDataFirebaseHelper.incrementWaudioShared();
        }
    }

    public void onGoEditor(View view) {
        /*Intent intent = new Intent(this, EditorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        */
        Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(app.getFilename()));
        //intent.putExtra("was_get_content_intent", mWasGetContentIntent);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setClassName("com.ecanaveras.gde.waudio", "com.ecanaveras.gde.waudio.EditorActivity");
        //startActivityForResult(intent, REQUEST_CODE_EDIT);
        startActivity(intent);
    }

    public void onGoFile(View view) {
        if (pathWaudio != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(pathWaudio);
            intent.setDataAndType(uri, "video/mp4");
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.msgOpenWith)));
        }
    }

    public void onDelete(View view) {
        if (pathWaudio != null) {
            final File wDel = new File(pathWaudio);
            if (wDel.exists()) {
                new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                        .setTitle(getResources().getString(R.string.msgDeleteWaudio))
                        .setMessage(getResources().getString(R.string.msgConfirmDelete))
                        .setPositiveButton(
                                getResources().getString(R.string.msgYesDelete),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        //app.getGeneratorWaudio().setOutFileWaudio(null);
                                        if (wDel.delete()) {
                                            mDataFirebaseHelper.incrementWaudioDeleted();
                                            videoView.setVideoURI(null);
                                            CompareWaudio cw = app.getCompareWaudioTmp();
                                            if (app.WaudioExist(cw))
                                                app.removeWaudio(cw);
                                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(wDel)));
                                            finish();
                                        } else {
                                            Toast.makeText(WaudioFinalizedActivity.this, getResources().getString(R.string.msgErrorDeleteWaudio), Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                })
                        .setCancelable(true)
                        .setNegativeButton(getResources().getString(R.string.alert_cancel), null)
                        .show();
            }
        }

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

    public void onFinish(View view) {
        int[] idsMsgs = {R.string.msgFinish1,
                R.string.msgFinish2,
                R.string.msgFinish3,
                R.string.msgFinish4,
                R.string.msgFinish5,
                R.string.msgFinish6};
        Random random = new Random();
        Toast.makeText(this, getResources().getString(idsMsgs[random.nextInt(idsMsgs.length)]), Toast.LENGTH_LONG).show();
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
            showBackAlert(getResources().getString(R.string.alert_title_choose_style), getResources().getString(R.string.msgWarningStyle), getResources().getString(R.string.alert_ok_choose_style), true);
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
