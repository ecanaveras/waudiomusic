package com.ecanaveras.gde.waudio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;

public class SplashScreen extends AppCompatActivity {

    com.ecanaveras.gde.waudio.util.PreferenceManager preferenceManager;
    //Maximizar Screen Pantalla
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private static int SPLASH_TIME_OUT = 2000;
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                gotoActivity();
            }
        }, SPLASH_TIME_OUT);

        mContentView = findViewById(R.id.imgBG);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private boolean verificarPermisos() {
        return ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void gotoActivity() {
        preferenceManager = new com.ecanaveras.gde.waudio.util.PreferenceManager(this);
        Intent mainIntent = null;
        if (preferenceManager.FirstLaunch()) {
            mainIntent = new Intent(SplashScreen.this, LandingActivity.class);
        } else {
            if (verificarPermisos()) {
                if (foundWaudios()) {
                    mainIntent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    //TODO, mostrar un asistente para crear Waudio, Grabar, o hacer ringtone
                    mainIntent = new Intent(SplashScreen.this, ListAudioActivity.class);
                }
            } else {
                //Solicitar Permisos
                mainIntent = new Intent(SplashScreen.this, PermitionsActivity.class);
            }
        }
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(mainIntent);
        finish();
    }

    /**
     * Busca la existencia de Waudios
     *
     * @return
     */
    private Boolean foundWaudios() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + MainApp.PATH_VIDEOS);
        if (dir.exists()) {
            for (String name : dir.list(new Mp4Filter(".mp4"))) {
                File vmp4 = new File(dir.getAbsolutePath() + "/" + name);
                if (vmp4.exists()) {
                    Log.i(SplashScreen.class.getSimpleName(), "Waudios found!");
                    return true;
                }
            }
        }
        return false;
    }


    //MANEJAR EL ACTION BAR
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //mControlsView.setVisibility(View.GONE);
        //mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        //mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

}
