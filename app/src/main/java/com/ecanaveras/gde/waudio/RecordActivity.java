package com.ecanaveras.gde.waudio;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.cleveroad.audiovisualization.AudioVisualization;
import com.cleveroad.audiovisualization.DbmHandler;
import com.ecanaveras.gde.waudio.wave.AudioRecorder;
import com.ecanaveras.gde.waudio.wave.Complex;
import com.ecanaveras.gde.waudio.wave.FFT;

import java.io.IOException;

import es.dmoral.toasty.Toasty;


public class RecordActivity extends AppCompatActivity {

    public static final String LOG_TAG = "AudioRecordTest";
    public static final int REQUEST_RECORD_AUDIO_PERMISION = 200;
    private static String mFileName = null;

    private RecordButton mRecordBUtton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton mPlayButton = null;
    private MediaPlayer mPlayer = null;

    //Permisos
    private boolean permisionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private AudioVisualization audioVisualization;
    private AudioRecordingDbmHandler handler;
    private AudioRecorder audioRecorder;
    private Button btnRecord;
    private Button btnPlayRecord;
    private LottieAnimationView lottieRecord;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISION:
                permisionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permisionToRecordAccepted) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        mFileName = getExternalCacheDir().getAbsolutePath();
        //mFileName += "/waudiorecord";
        mFileName += "/audiorecordtest.3gp";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISION);

        audioVisualization = (AudioVisualization) findViewById(R.id.visualizer_view);

        /*SpeechRecognizerDbmHandler handler = DbmHandler.Factory.newSpeechRecognizerHandler(this);
        handler.innerRecognitionListener();
        audioVisualization.linkTo(handler);

        // set audio visualization handler. This will REPLACE previously set speech recognizer handler
        VisualizerDbmHandler vizualizerHandler = DbmHandler.Factory.newVisualizerHandler(this, 0);
        audioVisualization.linkTo(vizualizerHandler);*/

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnPlayRecord = (Button) findViewById(R.id.btnPlayRecord);
        lottieRecord = (LottieAnimationView) findViewById(R.id.lottieRecord);

        lottieRecord.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                if (!audioRecorder.isRecording()) {
                    lottieRecord.cancelAnimation();
                    lottieRecord.setProgress(0);
                }
            }
        });

        lottieRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioRecorder.isRecording()) {
                    audioRecorder.finishRecord();
                    btnRecord.setText("Record");
                    handler.stop();
                    //onRecord(false);
                } else {
                    btnRecord.setText("Stop Record");
                    audioRecorder.startRecord();
                    lottieRecord.playAnimation();
                    Toasty.custom(getApplicationContext(), "+100 puntos", getDrawable(R.drawable.ic_points),getColor(R.color.colorAccent), Toast.LENGTH_SHORT, true, true).show();
                    //onRecord(true);
                    /*new StyleableToast
                            .Builder(getApplicationContext())
                            .text("+100 Puntos")
                            .textColor(Color.WHITE)
                            .backgroundColor(getColor(R.color.colorAccent))
                            .iconStart(R.drawable.ic_points)
                            .iconEnd(R.drawable.ic_points)
                            .show();*/
                }
            }
        });

        /*LinearLayout layout = new LinearLayout(this);
        mRecordBUtton = new RecordButton(this);
        layout.addView(mRecordBUtton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        mPlayButton = new PlayButton(this);
        layout.addView(mPlayButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        //setContentView(layout);
        addContentView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                */

        audioRecorder = new AudioRecorder();
        handler = new AudioRecordingDbmHandler();
        audioRecorder.recordingCallback(handler);
        audioVisualization.linkTo(handler);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioRecorder.isRecording()) {
                    audioRecorder.finishRecord();
                    btnRecord.setText("Record");
                    handler.stop();
                    //onRecord(false);
                    if (lottieRecord.isAnimating())
                        lottieRecord.cancelAnimation();
                } else {
                    btnRecord.setText("Stop Record");
                    audioRecorder.startRecord();
                    lottieRecord.playAnimation();
                    //onRecord(true);
                }
            }
        });

        btnPlayRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayer != null && mPlayer.isPlaying()) {
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });
    }

    /*@Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // you can extract AudioVisualization interface for simplifying things
        audioVisualization = (AudioVisualization) findViewById(R.id.visualizer_view);
    }*/

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Prepared Recorder Failed");
        }

    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Prepared Record Failed: "+ e.getMessage());
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        audioVisualization.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        audioVisualization.onPause();
    }

    @Override
    protected void onDestroy() {
        audioVisualization.release();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    //INNER CLASS
    class RecordButton extends androidx.appcompat.widget.AppCompatButton {
        private boolean mStarRecording = true;

        private final OnClickListener clicker = new OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecord(mStarRecording);
                if (mStarRecording) {
                    setText("Detener");
                } else {
                    setText("Grabar");
                }
                mStarRecording = !mStarRecording;
            }
        };

        public RecordButton(Context context) {
            super(context);
            this.setText("Grabar");
            this.setOnClickListener(clicker);
        }
    }

    class PlayButton extends androidx.appcompat.widget.AppCompatButton {

        private final OnClickListener clicker = new OnClickListener() {
            public boolean mStarPlaying = true;

            @Override
            public void onClick(View view) {
                onPlay(mStarPlaying);
                if (mStarPlaying) {
                    setText("Pause");
                } else {
                    setText("Play");
                }
                mStarPlaying = !mStarPlaying;
            }
        };

        public PlayButton(Context context) {
            super(context);
            setText("Play");
            setOnClickListener(clicker);
        }
    }

    private static class AudioRecordingDbmHandler extends DbmHandler<byte[]> implements AudioRecorder.RecordingCallback {
        private static final float MAX_DB_VALUE = 170;

        private float[] dbs;
        private float[] allAmps;

        @Override
        protected void onDataReceivedImpl(byte[] bytes, int layersCount, float[] dBmArray, float[] ampsArray) {
            final int bytesPerSample = 2; // As it is 16bit PCM
            final double amplification = 100.0; // choose a number as you like
            Complex[] fft = new Complex[bytes.length / bytesPerSample];
            for (int index = 0, floatIndex = 0; index < bytes.length - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
                double sample = 0;
                for (int b = 0; b < bytesPerSample; b++) {
                    int v = bytes[index + b];
                    if (b < bytesPerSample - 1) {
                        v &= 0xFF;
                    }
                    sample += v << (b * 8);
                }
                double sample32 = amplification * (sample / 32768.0);
                fft[floatIndex] = new Complex(sample32, 0);
            }
            fft = FFT.fft(fft);
            // calculate dBs and amplitudes
            int dataSize = fft.length / 2 - 1;
            if (dbs == null || dbs.length != dataSize) {
                dbs = new float[dataSize];
            }
            if (allAmps == null || allAmps.length != dataSize) {
                allAmps = new float[dataSize];
            }

            for (int i = 0; i < dataSize; i++) {
                dbs[i] = (float) fft[i].abs();
                float k = 1;
                if (i == 0 || i == dataSize - 1) {
                    k = 2;
                }
                float re = (float) fft[2 * i].re();
                float im = (float) fft[2 * i + 1].im();
                float sqMag = re * re + im * im;
                allAmps[i] = (float) (k * Math.sqrt(sqMag) / dataSize);
            }
            int size = dbs.length / layersCount;
            for (int i = 0; i < layersCount; i++) {
                int index = (int) ((i + 0.5f) * size);
                float db = dbs[index];
                float amp = allAmps[index];
                dBmArray[i] = db > MAX_DB_VALUE ? 1 : db / MAX_DB_VALUE;
                ampsArray[i] = amp;
            }
        }

        public void stop() {
            calmDownAndStopRendering();
        }

        @Override
        public void onDataReady(byte[] data) {
            onDataReceived(data);
        }
    }
}
