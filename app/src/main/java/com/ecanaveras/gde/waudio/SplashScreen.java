package com.ecanaveras.gde.waudio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;

public class SplashScreen extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2000;
    private static final String PATH_VIDEOS = "/Waudio/Media/Waudio Videos/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = null;
                if (foundWaudios()) {
                    mainIntent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    mainIntent = new Intent(SplashScreen.this, ListAudioActivity.class);
                }
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

    /**
     * Busca la existencia de Waudios
     *
     * @return
     */
    private Boolean foundWaudios() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + PATH_VIDEOS);
        if (dir.exists()) {
            for (String name : dir.list(new Mp4Filter(".mp4"))) {
                File vmp4 = new File(dir.getAbsolutePath() + "/" + name);
                if (vmp4.exists()) {
                    return true;
                }
                Log.i(SplashScreen.class.getSimpleName(), "Waudios found!");
            }
        }
        return false;
    }
}
