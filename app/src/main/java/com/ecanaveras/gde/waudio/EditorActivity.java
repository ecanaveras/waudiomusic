/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ecanaveras.gde.waudio;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.editor.MarkerView;
import com.ecanaveras.gde.waudio.editor.SoundFile;
import com.ecanaveras.gde.waudio.editor.WaveformView;
import com.ecanaveras.gde.waudio.util.FileSaveDialog;
import com.ecanaveras.gde.waudio.util.SamplePlayer;
import com.ecanaveras.gde.waudio.util.SongMetadataReader;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;

/**
 * The activity for the Ringdroid main editor_pref window.  Keeps track of
 * the waveform display, current horizontal offset, marker handles,
 * start / end text boxes, and handles all of the buttons and controls.
 */
public class EditorActivity extends AppCompatActivity
        implements MarkerView.MarkerListener,
        WaveformView.WaveformListener, AudioManager.OnAudioFocusChangeListener {

    private FirebaseAnalytics mFirebaseAnalytics;

    private long mLoadingLastUpdateTime;
    private boolean mLoadingKeepGoing;
    private long mRecordingLastUpdateTime;
    private boolean mRecordingKeepGoing;
    private double mRecordingTime;
    private boolean mFinishActivity;
    private TextView mTimerTextView;
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private SoundFile mSoundFile;
    private File mFile;
    private String mFilename;
    private String mArtist;
    private String mTitle;
    private int mNewFileKind;
    private boolean mWasGetContentIntent;
    private WaveformView mWaveformView;
    private MarkerView mStartMarker;
    private MarkerView mEndMarker;
    private TextView mStartText;
    private TextView mEndText;
    private TextView mDurationText;
    private TextView mInfo;
    private String mInfoContent;
    private ImageButton mPlayButton;
    private ImageButton mRewindButton;
    private ImageButton mFfwdButton;
    private boolean mKeyDown;
    private String mCaption = "";
    private int mWidth;
    private int mMaxPos;
    private int mStartPos;
    private int mEndPos;
    private boolean mStartVisible;
    private boolean mEndVisible;
    private int mLastDisplayedStartPos;
    private int mLastDisplayedEndPos;
    private int mOffset;
    private int mOffsetGoal;
    private int mFlingVelocity;
    private int mPlayStartMsec;
    private int mPlayEndMsec;
    private Handler mHandler;
    private boolean mIsPlaying;
    private SamplePlayer mPlayer;
    private boolean mTouchDragging;
    private float mTouchStart;
    private int mTouchInitialOffset;
    private int mTouchInitialStartPos;
    private int mTouchInitialEndPos;
    private long mWaveformTouchStartMsec;
    private float mDensity;
    private int mMarkerLeftInset;
    private int mMarkerRightInset;
    private int mMarkerTopOffset;
    private int mMarkerBottomOffset;

    private Thread mLoadSoundFileThread;
    private Thread mRecordAudioThread;
    private Thread mSaveSoundFileThread;

    // Result codes
    private static final int REQUEST_CODE_CHOOSE_CONTACT = 1;

    /**
     * This is a special intent action that means "edit a sound file".
     */
    public static final String EDIT = "com.ecanaveras.gde.waudio.action.EDIT";
    private LinearLayout le;
    private TextView lblTitleAudio, lblTitleAudioLoading, lblPercent;

    private boolean max30s = true;
    private AudioManager audioManager;
    private ImageButton mBack30s;
    private ImageButton mNext30s;
    private RelativeLayout lyContentLoading, lyContentLoading2;
    private LinearLayout lyContentEditor;
    private long back_pressed = 0;
    private Thread mInfoThread;

    //
    // Public methods and protected overrides
    //

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.v("Waudio", "EditActivity OnCreate");
        super.onCreate(icicle);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setupIntent(getIntent());

        mFirebaseAnalytics.setUserProperty("open_editor", String.valueOf(true));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //cancelEdition();
        Boolean edicion = intent.getBooleanExtra("continue_edition", false);
        if (!edicion) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setClassName("com.ecanaveras.gde.waudio", "com.ecanaveras.gde.waudio.EditorActivity");
            //startActivityForResult(intent, REQUEST_CODE_EDIT);
            startActivity(intent);
            finish();
        }
    }

    private void setupIntent(Intent intent) {
        //Maneja el audio en llamadas
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mPlayer = null;
        mIsPlaying = false;

        mAlertDialog = null;
        mProgressDialog = null;

        mLoadSoundFileThread = null;
        mRecordAudioThread = null;
        mSaveSoundFileThread = null;

        Intent receivedIntent = intent;
        String receivedAction = receivedIntent.getAction();
        String receivedType = receivedIntent.getType();

        //if (receivedIntent.getData() == null) {
        //onBackPressed();
        //Toast.makeText(this, getResources().getString(R.string.msgRestart), Toast.LENGTH_SHORT).show();
        //}
        // If the Ringdroid media select activity was launched via a
        // GET_CONTENT intent, then we shouldn't display a "saved"
        // message when the user saves, we should just return whatever
        // they create.
        mWasGetContentIntent = receivedIntent.getBooleanExtra("was_get_content_intent", false);

        if (receivedIntent != null) {
            if (receivedAction.equals(Intent.ACTION_SEND) && receivedType.startsWith("audio/")) {
                Uri receivedUri = (Uri) receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                mFilename = getRealPathFromURI(receivedUri);
            } else {
                mFilename = receivedIntent.getData().toString().replaceFirst("file://", "").replaceAll("%20", " ").replaceAll("%2C", ",");
            }
        }

        mSoundFile = null;
        mKeyDown = false;

        System.out.println("FILE:" + mFilename);

        mHandler = new Handler();

        loadGui();

        mHandler.postDelayed(mTimerRunnable, 100);

        if (!mFilename.equals("record")) {
            loadFromFile();
        } else {
            recordAudio();
        }
    }


    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void closeThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            cancelEdition();
            super.onBackPressed();
            this.finish();
        } else
            Toast.makeText(this, getResources().getString(R.string.msgCancelEditor), Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();

    }

    /**
     * Called when the activity is finally destroyed.
     */
    @Override
    protected void onDestroy() {
        Log.v("Waudio", "EditActivity OnDestroy");
        cancelEdition();

        super.onDestroy();
    }

    private void cancelEdition() {
        lyContentLoading.setVisibility(View.GONE);
        mLoadingKeepGoing = false;
        mRecordingKeepGoing = false;
        mHandler.removeCallbacks(mTimerRunnable);
        closeThread(mLoadSoundFileThread);
        closeThread(mRecordAudioThread);
        closeThread(mSaveSoundFileThread);
        closeThread(mInfoThread);
        mLoadSoundFileThread = null;
        mRecordAudioThread = null;
        mSaveSoundFileThread = null;
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }

        if (mPlayer != null) {
            if (mPlayer.isPlaying() || mPlayer.isPaused()) {
                mPlayer.stop();
            }
            mPlayer.release();
            mPlayer = null;
        }

        if (audioManager != null)
            audioManager.abandonAudioFocus(this);

    }

    /**
     * Called with an Activity we started with an Intent returns.
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent dataIntent) {
        Log.v("Waudio", "EditActivity onActivityResult");
        if (requestCode == REQUEST_CODE_CHOOSE_CONTACT) {
            // The user finished saving their ringtone and they're
            // just applying it to a contact.  When they return here,
            // they're done.
            finish();
            return;
        }
    }

    /**
     * Called when the orientation changes and/or the keyboard is shown
     * or hidden.  We don't need to recreate the whole activity in this
     * case, but we do need to redo our layout somewhat.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v("Waudio", "EditActivity onConfigurationChanged");
        final int saveZoomLevel = mWaveformView.getZoomLevel();
        super.onConfigurationChanged(newConfig);

        loadGui();

        mHandler.postDelayed(new Runnable() {
            public void run() {
                mStartMarker.requestFocus();
                markerFocus(mStartMarker);
                mWaveformView.setZoomLevel(saveZoomLevel);
                mWaveformView.recomputeHeights(mDensity);

                updateDisplay();
            }
        }, 500);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            onPlay(mStartPos);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //
    // WaveformListener
    //

    /**
     * Every time we get a message that our waveform drew, see if we need to
     * animate and trigger another redraw.
     */
    public void waveformDraw() {
        mWidth = mWaveformView.getMeasuredWidth();
        if (mOffsetGoal != mOffset && !mKeyDown)
            updateDisplay();
        else if (mIsPlaying) {
            updateDisplay();
        } else if (mFlingVelocity != 0) {
            updateDisplay();
        }
    }

    public void waveformTouchStart(float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialOffset = mOffset;
        mFlingVelocity = 0;
        mWaveformTouchStartMsec = getCurrentTime();
    }

    public void waveformTouchMove(float x) {
        mOffset = trap((int) (mTouchInitialOffset + (mTouchStart - x)));
        updateDisplay();
    }

    public void waveformTouchEnd() {
        mTouchDragging = false;
        mOffsetGoal = mOffset;

        long elapsedMsec = getCurrentTime() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = mWaveformView.pixelsToMillisecs(
                        (int) (mTouchStart + mOffset));
                if (seekMsec >= mPlayStartMsec &&
                        seekMsec < mPlayEndMsec) {
                    mPlayer.seekTo(seekMsec);
                } else {
                    handlePause();
                }
            } else {
                onPlay((int) (mTouchStart + mOffset));
            }
        }
    }

    public void waveformFling(float vx) {
        mTouchDragging = false;
        mOffsetGoal = mOffset;
        mFlingVelocity = (int) (-vx);
        updateDisplay();
    }

    public void waveformZoomIn() {
        mWaveformView.zoomIn();
        mStartPos = mWaveformView.getStart();
        mEndPos = mWaveformView.getEnd();
        mMaxPos = mWaveformView.maxPos();
        mOffset = mWaveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    public void waveformZoomOut() {
        mWaveformView.zoomOut();
        mStartPos = mWaveformView.getStart();
        mEndPos = mWaveformView.getEnd();
        mMaxPos = mWaveformView.maxPos();
        mOffset = mWaveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    //
    // MarkerListener
    //

    public void markerDraw() {
    }

    public void markerTouchStart(MarkerView marker, float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialStartPos = mStartPos;
        mTouchInitialEndPos = mEndPos;
    }

    public void markerTouchMove(MarkerView marker, float x) {
        float delta = x - mTouchStart;

        if (marker == mStartMarker) {
            mStartPos = trap((int) (mTouchInitialStartPos + delta));
            mEndPos = trap((int) (mTouchInitialEndPos + delta));
        } else {
            double end30s = Double.valueOf(formatTime(mStartPos)) + 30;

            mEndPos = trap((int) (mTouchInitialEndPos + delta));
            if (mEndPos < mStartPos)
                mEndPos = mStartPos;

            //No lo deja avanzar a la derecha
            if (max30s && Double.valueOf(formatTime(mEndPos)) > end30s) {
                mEndPos = mWaveformView.secondsToPixels(end30s);
            }
        }

        updateDisplay();
    }

    public void markerTouchEnd(MarkerView marker) {
        mTouchDragging = false;
        if (marker == mStartMarker) {
            setOffsetGoalStart();
        } else {
            setOffsetGoalEnd();
        }
    }

    public void markerLeft(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos = trap(mStartPos - velocity);
            mEndPos = trap(mEndPos - (saveStart - mStartPos));
            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity);
                mEndPos = mStartPos;
            } else {
                mEndPos = trap(mEndPos - velocity);
            }

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    public void markerRight(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos += velocity;
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos;
            mEndPos += (mStartPos - saveStart);
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            mEndPos += velocity;
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    public void markerEnter(MarkerView marker) {
    }

    public void markerKeyUp() {
        mKeyDown = false;
        updateDisplay();
    }

    public void markerFocus(MarkerView marker) {
        mKeyDown = false;
        if (marker == mStartMarker) {
            setOffsetGoalStartNoUpdate();
        } else {
            setOffsetGoalEndNoUpdate();
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateDisplay();
            }
        }, 100);
    }

    //
    // Internal methods
    //

    /**
     * Called from both onCreate and onConfigurationChanged
     * (if the user switched layouts)
     */
    private void loadGui() {
        // Inflate our UI from its XML layout description.
        setContentView(R.layout.activity_editor);

        le = (LinearLayout) findViewById(R.id.layoutEditor);
        lblTitleAudioLoading = (TextView) findViewById(R.id.lblTitleAudioLoading);
        lblTitleAudio = (TextView) findViewById(R.id.lblTitleAudio);
        lblPercent = (TextView) findViewById(R.id.lblPercent);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        mMarkerLeftInset = (int) (31.5 * mDensity);
        mMarkerRightInset = (int) (31.8 * mDensity);
        mMarkerTopOffset = (int) (25 * mDensity);
        mMarkerBottomOffset = (int) (25 * mDensity);

        mStartText = (TextView) findViewById(R.id.starttext);
        mStartText.addTextChangedListener(mTextWatcher);
        mEndText = (TextView) findViewById(R.id.endtext);
        mEndText.addTextChangedListener(mTextWatcher);
        mDurationText = (TextView) findViewById(R.id.durationtext);

        lyContentLoading = (RelativeLayout) findViewById(R.id.lyContentLoading);
        lyContentLoading2 = (RelativeLayout) findViewById(R.id.lyContentLoading2);
        lyContentEditor = (LinearLayout) findViewById(R.id.lyContentEditor);

        lyContentLoading.setVisibility(View.VISIBLE);
        lyContentLoading.setVisibility(View.GONE);
        lyContentEditor.setVisibility(View.GONE);

        ((RadioGroup) findViewById(R.id.toggleGroup)).setOnCheckedChangeListener(ToggleListener);

        mNext30s = (ImageButton) findViewById(R.id.next30);
        mNext30s.setOnClickListener(mNext30sListener);
        mBack30s = (ImageButton) findViewById(R.id.back30);
        mBack30s.setOnClickListener(mBack30sListener);

        mPlayButton = (ImageButton) findViewById(R.id.play);
        mPlayButton.setOnClickListener(mPlayListener);
        mRewindButton = (ImageButton) findViewById(R.id.rew);
        mRewindButton.setOnClickListener(mRewindListener);
        mFfwdButton = (ImageButton) findViewById(R.id.ffwd);
        mFfwdButton.setOnClickListener(mFfwdListener);

        /*TextView markStartButton = (TextView) findViewById(R.id.mark_start);
        markStartButton.setOnClickListener(mMarkStartListener);
        TextView markEndButton = (TextView) findViewById(R.id.mark_end);
        markEndButton.setOnClickListener(mMarkEndListener);
        */

        enableDisableButtons();

        mWaveformView = (WaveformView) findViewById(R.id.waveform);
        mWaveformView.setListener(this);

        mInfo = (TextView) findViewById(R.id.info);
        mInfo.setText(mCaption);

        mMaxPos = 0;
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        if (mSoundFile != null && !mWaveformView.hasSoundFile()) {
            mWaveformView.setSoundFile(mSoundFile);
            mWaveformView.recomputeHeights(mDensity);
            mMaxPos = mWaveformView.maxPos();
        }

        mStartMarker = (MarkerView) findViewById(R.id.startmarker);
        mStartMarker.setListener(this);
        mStartMarker.setAlpha(1f);
        mStartMarker.setFocusable(true);
        mStartMarker.setFocusableInTouchMode(true);
        mStartVisible = true;

        mEndMarker = (MarkerView) findViewById(R.id.endmarker);
        mEndMarker.setListener(this);
        mEndMarker.setAlpha(1f);
        mEndMarker.setFocusable(true);
        mEndMarker.setFocusableInTouchMode(true);
        mEndVisible = true;

        updateDisplay();
    }

    private void loadFromFile() {
        mFile = new File(mFilename);

        SongMetadataReader metadataReader = new SongMetadataReader(
                this, mFilename);
        mTitle = metadataReader.mTitle;
        mArtist = metadataReader.mArtist;

        String titleLabel = mTitle;
        if (mArtist != null && mArtist.length() > 0) {
            titleLabel += " - " + mArtist;
        }
        //setTitle(titleLabel);
        lblTitleAudioLoading.setText(titleLabel);
        lblTitleAudio.setText(titleLabel);

        mLoadingLastUpdateTime = getCurrentTime();
        mLoadingKeepGoing = true;
        mFinishActivity = false;

        //Layout Loading
        lyContentLoading.setVisibility(View.VISIBLE);
        /*mProgressDialog = new ProgressDialog(EditorActivity.this, R.style.AlertDialogCustom);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(R.string.progress_dialog_loading);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mLoadingKeepGoing = false;
                        mFinishActivity = true;
                    }
                });
        mProgressDialog.show();*/
        //lw.setVisibility(View.VISIBLE);

        //Mantener al Usuario en espera
        mInfoThread = new Thread() {

            boolean closePercent = false;
            int seconds = 0;

            @Override
            public void run() {
                while (lyContentLoading.getVisibility() == View.VISIBLE) {
                    try {
                        Thread.sleep(1000);
                        seconds++;
                        final String info = lblPercent.getText().toString().replace("%", "").replace(",", ".");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!lblPercent.getText().toString().startsWith("Bingo")) {
                                    Double porc = Double.parseDouble(info);
                                    if (porc.intValue() > 80 && porc < 99.9 && porc.equals(Double.parseDouble(info))) {
                                        porc += 0.1;
                                        lblPercent.setText(new DecimalFormat("##.#").format(porc) + "%");
                                    } else if (porc >= 99.9 && !closePercent) {
                                        closePercent = true;
                                        Animation animationIn = AnimationUtils.loadAnimation(EditorActivity.this, R.anim.slide_in);
                                        Animation animationOut = AnimationUtils.loadAnimation(EditorActivity.this, R.anim.slide_out);
                                        lyContentLoading.startAnimation(animationOut);
                                        lyContentLoading.setVisibility(View.GONE);
                                        lyContentLoading2.startAnimation(animationIn);
                                        lyContentLoading2.setVisibility(View.VISIBLE);
                                    }
                                    //System.out.println("Info:" + info + " PORCT:" + porc);
                                }
                            }
                        });
                        if (closePercent) {
                            break;
                        }
                        //System.out.println("DEMORA: " + seconds);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        ;
        mInfoThread.start();

        final SoundFile.ProgressListener listener =
                new SoundFile.ProgressListener() {
                    public boolean reportProgress(double fractionComplete) {
                        long now = getCurrentTime();
                        Double currentPercent = 1.0;
                        if (now - mLoadingLastUpdateTime > 100) {
                            currentPercent *= fractionComplete;
                            //System.out.println("PERCENT: " + currentPercent + "%");
                            updatePercent(currentPercent);
                            //mProgressDialog.setProgress((int) (mProgressDialog.getMax() * fractionComplete));
                            mLoadingLastUpdateTime = now;
                        }
                        return mLoadingKeepGoing;
                    }
                };

        // Load the sound file in a background thread
        mLoadSoundFileThread = new

                Thread() {

                    public void run() {
                        try {
                            mSoundFile = SoundFile.create(mFile.getAbsolutePath(), listener);
                            if (mSoundFile == null) {
                                String name = mFile.getName().toLowerCase();
                                String[] components = name.split("\\.");
                                String err;
                                if (components.length < 2) {
                                    err = getResources().getString(
                                            R.string.no_extension_error);
                                } else {
                                    err = getResources().getString(
                                            R.string.bad_extension_error) + " " +
                                            components[components.length - 1];
                                }
                                final String finalErr = err;
                                Runnable runnable = new Runnable() {
                                    public void run() {
                                        showFinalAlert(new Exception(), finalErr);
                                    }
                                };
                                mHandler.post(runnable);
                                return;
                            }
                            mPlayer = new SamplePlayer(mSoundFile);
                        } catch (final Exception e) {
                            //mProgressDialog.dismiss();
                            e.printStackTrace();
                            mInfoContent = e.toString();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    mInfo.setText(mInfoContent);
                                }
                            });

                            Runnable runnable = new Runnable() {
                                public void run() {
                                    showFinalAlert(e, getResources().getText(R.string.read_error));
                                }
                            };
                            mHandler.post(runnable);
                            return;
                        }
                        //mProgressDialog.dismiss();
                        if (mLoadingKeepGoing) {
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    finishOpeningSoundFile();
                                }
                            };
                            mHandler.post(runnable);
                        } else if (mFinishActivity) {
                            EditorActivity.this.finish();
                        }
                    }

                }

        ;
        mLoadSoundFileThread.start();
    }

    private void updatePercent(final double percent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lblPercent.setText(new DecimalFormat("##.#%").format(percent));
            }
        });

    }

    private void recordAudio() {
        mFile = null;
        mTitle = null;
        mArtist = null;

        mRecordingLastUpdateTime = getCurrentTime();
        mRecordingKeepGoing = true;
        mFinishActivity = false;
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(EditorActivity.this);
        adBuilder.setTitle(getResources().getText(R.string.progress_dialog_recording));
        adBuilder.setCancelable(true);
        adBuilder.setNegativeButton(
                getResources().getText(R.string.progress_dialog_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mRecordingKeepGoing = false;
                        mFinishActivity = true;
                    }
                });
        adBuilder.setPositiveButton(
                getResources().getText(R.string.progress_dialog_stop),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mRecordingKeepGoing = false;
                    }
                });
        // TODO(nfaralli): try to use a FrameLayout and pass it to the following inflate call.
        // Using null, android:layout_width etc. may not work (hence text is at the top of view).
        // On the other hand, if the text is big enough, this is good enough.
        adBuilder.setView(getLayoutInflater().inflate(R.layout.record_audio, null));
        mAlertDialog = adBuilder.show();
        mTimerTextView = (TextView) mAlertDialog.findViewById(R.id.record_audio_timer);

        final SoundFile.ProgressListener listener =
                new SoundFile.ProgressListener() {
                    public boolean reportProgress(double elapsedTime) {
                        long now = getCurrentTime();
                        if (now - mRecordingLastUpdateTime > 5) {
                            mRecordingTime = elapsedTime;
                            // Only UI thread can update Views such as TextViews.
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    int min = (int) (mRecordingTime / 60);
                                    float sec = (float) (mRecordingTime - 60 * min);
                                    mTimerTextView.setText(String.format("%d:%05.2f", min, sec));
                                }
                            });
                            mRecordingLastUpdateTime = now;
                        }
                        return mRecordingKeepGoing;
                    }
                };

        // Record the audio stream in a background thread
        mRecordAudioThread = new Thread() {
            public void run() {
                try {
                    mSoundFile = SoundFile.record(listener);
                    if (mSoundFile == null) {
                        mAlertDialog.dismiss();
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(
                                        new Exception(),
                                        getResources().getText(R.string.record_error)
                                );
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                    mPlayer = new SamplePlayer(mSoundFile);
                } catch (final Exception e) {
                    mAlertDialog.dismiss();
                    e.printStackTrace();
                    mInfoContent = e.toString();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mInfo.setText(mInfoContent);
                        }
                    });

                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(e, getResources().getText(R.string.record_error));
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }
                mAlertDialog.dismiss();
                if (mFinishActivity) {
                    EditorActivity.this.finish();
                } else {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finishOpeningSoundFile();
                        }
                    };
                    mHandler.post(runnable);
                }
            }
        };
        mRecordAudioThread.start();
    }

    private void finishOpeningSoundFile() {
        Animation animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        Animation animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
        lyContentEditor.startAnimation(animationIn);
        lyContentEditor.setVisibility(View.VISIBLE);
        if (lyContentLoading.getVisibility() == View.VISIBLE) {
            lyContentLoading.startAnimation(animationOut);
            lyContentLoading.setVisibility(View.GONE);
        }
        if (lyContentLoading2.getVisibility() == View.VISIBLE) {
            lyContentLoading2.startAnimation(animationOut);
            lyContentLoading2.setVisibility(View.GONE);
        }

        mWaveformView.setSoundFile(mSoundFile);
        mWaveformView.recomputeHeights(mDensity);

        mMaxPos = mWaveformView.maxPos();
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        mTouchDragging = false;

        mOffset = 0;
        mOffsetGoal = 0;
        mFlingVelocity = 0;
        //resetPositions();
        if (mEndPos > mMaxPos)
            mEndPos = mMaxPos;

        /*mCaption =                mSoundFile.getFiletype() + ", " +
                        mSoundFile.getSampleRate() + " Hz, " +
                        mSoundFile.getAvgBitrateKbps() + " kbps, " +
                        formatTime(mMaxPos) + " " +
                        getResources().getString(R.string.time_seconds);
        mInfo.setText(mCaption);*/

        //UI Show/Hide
        //le.setVisibility(View.VISIBLE);


        mStartMarker.setVisibility(View.VISIBLE);
        mStartMarker.requestFocus();
        mEndMarker.setVisibility(View.VISIBLE);
        if (Double.valueOf(formatTime(mMaxPos)) < 31.0) {
            mNext30s.setVisibility(View.INVISIBLE);
            mBack30s.setVisibility(View.INVISIBLE);
        }

        mStartText.setEnabled(true);
        mEndText.setEnabled(true);

        updateDisplay();
        resetPositions();


        Toast toast = Toast.makeText(this, getResources().getString(R.string.msgChoose30seg), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private synchronized void updateDisplay() {
        if (mIsPlaying && mPlayer != null) {
            int now = mPlayer.getCurrentPosition();
            int frames = mWaveformView.millisecsToPixels(now);
            mWaveformView.setPlayback(frames);
            setOffsetGoalNoUpdate(frames - mWidth / 2);
            if (now >= mPlayEndMsec) {
                handlePause();
            }
        }

        if (!mTouchDragging) {
            int offsetDelta;

            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30;
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80;
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80;
                } else {
                    mFlingVelocity = 0;
                }

                mOffset += offsetDelta;

                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2;
                    mFlingVelocity = 0;
                }
                if (mOffset < 0) {
                    mOffset = 0;
                    mFlingVelocity = 0;
                }
                mOffsetGoal = mOffset;
            } else {
                offsetDelta = mOffsetGoal - mOffset;

                if (offsetDelta > 10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta > 0)
                    offsetDelta = 1;
                else if (offsetDelta < -10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta < 0)
                    offsetDelta = -1;
                else
                    offsetDelta = 0;

                mOffset += offsetDelta;
            }
        }

        mWaveformView.setParameters(mStartPos, mEndPos, mOffset);
        mWaveformView.invalidate();

        mStartMarker.setContentDescription(
                getResources().getText(R.string.start_marker) + " " +
                        formatTime(mStartPos));
        mEndMarker.setContentDescription(
                getResources().getText(R.string.end_marker) + " " +
                        formatTime(mEndPos));

        int startX = mStartPos - mOffset - mMarkerLeftInset;
        if (startX + mStartMarker.getWidth() >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mStartVisible = true;
                        mStartMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mStartVisible) {
                mStartMarker.setAlpha(0f);
                mStartVisible = false;
            }
            startX = 0;
        }

        int endX = mEndPos - mOffset - mEndMarker.getWidth() + mMarkerRightInset;
        if (endX + mEndMarker.getWidth() >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mEndVisible = true;
                        mEndMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mEndVisible) {
                mEndMarker.setAlpha(0f);
                mEndVisible = false;
            }
            endX = 0;
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                startX,
                mMarkerTopOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mStartMarker.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                endX,
                mWaveformView.getMeasuredHeight() - mEndMarker.getHeight() - mMarkerBottomOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mEndMarker.setLayoutParams(params);
    }

    private Runnable mTimerRunnable = new Runnable() {
        public void run() {
            if (lyContentLoading.getVisibility() == View.GONE) {
                // Updating an EditText is slow on Android.  Make sure
                // we only do the update if the text has actually changed.
                if (mStartPos != mLastDisplayedStartPos &&
                        !mStartText.hasFocus()) {
                    mStartText.setText(formatTime(mStartPos));
                    mLastDisplayedStartPos = mStartPos;
                }

                if (mEndPos != mLastDisplayedEndPos &&
                        !mEndText.hasFocus()) {
                    mEndText.setText(formatTime(mEndPos));
                    mLastDisplayedEndPos = mEndPos;
                }
                int duration = mEndPos - mStartPos;
                mDurationText.setText(formatTime(duration));
                if (duration > 0 && Double.valueOf(formatTime(duration)) > 31.0) {
                    mDurationText.setTextColor(ContextCompat.getColor(EditorActivity.this, R.color.colorTextSecondary));
                } else {
                    mDurationText.setTextColor(ContextCompat.getColor(EditorActivity.this, R.color.playback_indicator));
                }
            }

            mHandler.postDelayed(mTimerRunnable, 100);
        }
    };

    private void enableDisableButtons() {
        if (mIsPlaying) {
            mPlayButton.setImageResource(R.drawable.ic_pause);
            mPlayButton.setContentDescription(getResources().getText(R.string.stop));
        } else {
            mPlayButton.setImageResource(R.drawable.ic_play_arrow);
            mPlayButton.setContentDescription(getResources().getText(R.string.play));
        }
    }

    private void resetPositions() {
        if (new Double(formatTime(mMaxPos)) <= 30) {
            mStartPos = mWaveformView.secondsToPixels(0.0);
            mEndPos = mWaveformView.secondsToPixels(Double.valueOf(formatTime(mMaxPos)));
        } else {
            mStartPos = mWaveformView.secondsToPixels(10);
            mEndPos = mWaveformView.secondsToPixels(40.0);
        }
    }

    private int trap(int pos) {
        if (pos < 0)
            return 0;
        if (pos > mMaxPos)
            return mMaxPos;
        return pos;
    }

    private void setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
    }

    private void setOffsetGoal(int offset) {
        setOffsetGoalNoUpdate(offset);
        updateDisplay();
    }

    private void setOffsetGoalNoUpdate(int offset) {
        if (mTouchDragging) {
            return;
        }

        mOffsetGoal = offset;
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2;
        if (mOffsetGoal < 0)
            mOffsetGoal = 0;

    }

    private String formatTime(int pixels) {
        if (mWaveformView != null && mWaveformView.isInitialized()) {
            return formatDecimal(mWaveformView.pixelsToSeconds(pixels));
        } else {
            return "";
        }
    }

    private String formatDecimal(double x) {
        int xWhole = (int) x;
        int xFrac = (int) (100 * (x - xWhole) + 0.5);

        if (xFrac >= 100) {
            xWhole++; //Round up
            xFrac -= 100; //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10; //we need a fraction that is 2 digits long
            }
        }

        if (xFrac < 10)
            return xWhole + ".0" + xFrac;
        else
            return xWhole + "." + xFrac;
    }

    private synchronized void handlePause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        audioManager.abandonAudioFocus(this);
        mWaveformView.setPlayback(-1);
        mIsPlaying = false;
        enableDisableButtons();
    }

    private synchronized void onPlay(int startPosition) {
        if (mIsPlaying) {
            handlePause();
            return;
        }

        if (mPlayer == null) {
            // Not initialized yet
            return;
        }

        try {
            mPlayStartMsec = mWaveformView.pixelsToMillisecs(startPosition);
            if (startPosition < mStartPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mStartPos);
            } else if (startPosition > mEndPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mMaxPos);
            } else {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mEndPos);
            }
            mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {
                @Override
                public void onCompletion() {
                    handlePause();
                }
            });
            mIsPlaying = true;

            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mPlayer.seekTo(mPlayStartMsec);
            mPlayer.start();
            updateDisplay();
            enableDisableButtons();
        } catch (Exception e) {
            showFinalAlert(e, R.string.play_error);
            return;
        }
    }

    public void onToggle(View view) {
        ((RadioGroup) view.getParent()).check(view.getId());
        switch (view.getId()) {
            case R.id.tBtnMax30s:
                mEndPos = mWaveformView.secondsToPixels(Double.valueOf(formatTime(mStartPos)) + 30);
                max30s = true;
                break;
            case R.id.tBtnFree:
                max30s = false;
                break;
        }
        updateDisplay();
    }

    static final RadioGroup.OnCheckedChangeListener ToggleListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final RadioGroup radioGroup, final int i) {
            for (int j = 0; j < radioGroup.getChildCount(); j++) {
                final ToggleButton view = (ToggleButton) radioGroup.getChildAt(j);
                view.setChecked(view.getId() == i);
            }
        }
    };

    /**
     * Show a "final" alert dialog that will exit the activity
     * after the user clicks on the OK button.  If an exception
     * is passed, it's assumed to be an error condition, and the
     * dialog is presented as an error, and the stack trace is
     * logged.  If there's no exception, it's a success message.
     */
    private void showFinalAlert(Exception e, CharSequence message) {
        CharSequence title;
        if (e != null) {
            Log.e("Waudio", "Error: " + message);
            Log.e("Waudio", getStackTrace(e));
            title = getResources().getText(R.string.alert_title_failure);
            setResult(RESULT_CANCELED, new Intent());
        } else {
            Log.v("Waudio", "Success: " + message);
            title = getResources().getText(R.string.alert_title_success);
        }

        new AlertDialog.Builder(EditorActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        R.string.alert_ok_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                finish();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    private void showFinalAlert(Exception e, int messageResourceId) {
        showFinalAlert(e, getResources().getText(messageResourceId));
    }


    /**
     * Nueva forma de manejar la generación del Waudio
     *
     * @param view
     */
    public void onNext(View view) {
        if (mIsPlaying) {
            handlePause();
        }
        double startTime = mWaveformView.pixelsToSeconds(mStartPos);
        double endTime = mWaveformView.pixelsToSeconds(mEndPos);
        int startFrame = mWaveformView.secondsToFrames(startTime);
        int endFrame = mWaveformView.secondsToFrames(endTime);
        MainApp app = (MainApp) getApplicationContext();
        GeneratorWaudio waudio = new GeneratorWaudio(getApplicationContext(), mSoundFile, mTitle, startTime, endTime, startFrame, endFrame);
        app.setGeneratorWaudio(waudio);
        Intent intent = new Intent(EditorActivity.this, ListTemplateActivity.class);
        startActivity(intent);
    }

    private void onSave() {
        if (mIsPlaying) {
            handlePause();
        }

        final Handler handler = new Handler() {
            public void handleMessage(Message response) {
                CharSequence newTitle = (CharSequence) response.obj;
                mNewFileKind = response.arg1;
                //saveRingtone(newTitle);


            }
        };
        Message message = Message.obtain(handler);
        FileSaveDialog dlog = new FileSaveDialog(
                this, getResources(), mTitle, message);
        dlog.show();
    }

    private OnClickListener mPlayListener = new OnClickListener() {
        public void onClick(View sender) {
            onPlay(mStartPos);
        }
    };

    private OnClickListener mRewindListener = new OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                int newPos = mPlayer.getCurrentPosition() - 5000;
                if (newPos < mPlayStartMsec)
                    newPos = mPlayStartMsec;
                mPlayer.seekTo(newPos);
            } else {
                mStartMarker.requestFocus();
                markerFocus(mStartMarker);
            }
        }
    };

    private OnClickListener mFfwdListener = new OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                int newPos = 5000 + mPlayer.getCurrentPosition();
                if (newPos > mPlayEndMsec)
                    newPos = mPlayEndMsec;
                mPlayer.seekTo(newPos);
            } else {
                mEndMarker.requestFocus();
                markerFocus(mEndMarker);
            }
        }
    };

    private OnClickListener mMarkStartListener = new OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                mStartPos = mWaveformView.millisecsToPixels(
                        mPlayer.getCurrentPosition());
                updateDisplay();
            }
        }
    };

    private OnClickListener mMarkEndListener = new OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                mEndPos = mWaveformView.millisecsToPixels(
                        mPlayer.getCurrentPosition());
                updateDisplay();
                handlePause();
            }
        }
    };

    private OnClickListener mNext30sListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Double fin = Double.parseDouble(formatTime(mWaveformView.maxPos()));
            Double posStart = Double.parseDouble(formatTime(mEndPos));
            if (posStart >= fin) {
                return;
            }
            //Double dif = Double.parseDouble(formatTime(mEndPos)) - Double.parseDouble(formatTime(mStartPos));
            mStartPos = mWaveformView.secondsToPixels(posStart);
            Double posEnd = posStart + 30;
            if (posEnd > fin) {
                posEnd = fin;
            }
            mEndPos = mWaveformView.secondsToPixels(posEnd);
            //Hacer mover al WaveForm
            onPlay(mStartPos);
            handlePause();
            updateDisplay();
        }
    };

    private OnClickListener mBack30sListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Double inicio = 0.0;
            Double posStart = Double.parseDouble(formatTime(mStartPos)) - 30.0;
            if (posStart < inicio) {
                posStart = inicio;
            }
            Double dif = Double.parseDouble(formatTime(mEndPos)) - Double.parseDouble(formatTime(mStartPos));
            mStartPos = mWaveformView.secondsToPixels(posStart);
            Double posEnd = Double.parseDouble(formatTime(mEndPos)) - dif;
            if (posStart == inicio) {
                posEnd = posStart + dif;
            }
            mEndPos = mWaveformView.secondsToPixels(posEnd);
            //Hacer mover al WaveForm
            onPlay(mStartPos);
            handlePause();
            updateDisplay();
        }
    };


    private TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start,
                                      int count, int after) {
        }

        public void onTextChanged(CharSequence s,
                                  int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            if (mStartText.hasFocus()) {
                try {
                    mStartPos = mWaveformView.secondsToPixels(
                            Double.parseDouble(
                                    mStartText.getText().toString()));
                    updateDisplay();
                } catch (NumberFormatException e) {
                }
            }
            if (mEndText.hasFocus()) {
                try {
                    mEndPos = mWaveformView.secondsToPixels(
                            Double.parseDouble(
                                    mEndText.getText().toString()));
                    updateDisplay();
                } catch (NumberFormatException e) {
                }
            }
        }
    };

    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    private String getStackTrace(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
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
                if (mPlayer.isPlaying())
                    mPlayer.pause();// Pause your media player here
                break;
        }
    }
}
