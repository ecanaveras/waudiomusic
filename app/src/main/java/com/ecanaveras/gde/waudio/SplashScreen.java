package com.ecanaveras.gde.waudio;

import android.*;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;

public class SplashScreen extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2000;
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                solicitarPermisos();
            }
        }, SPLASH_TIME_OUT);
    }

    private void gotoActivity() {
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

    private void solicitarPermisos() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            gotoActivity();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                AlertDialog.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            requestPermissions();
                        } else if (i == DialogInterface.BUTTON_NEGATIVE) {
                            permissionNoGranted();
                        }
                    }
                };

                new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                        .setTitle(getResources().getString(R.string.alert_title_permitions))
                        .setMessage(Html.fromHtml(getString(R.string.message_permissions)))
                        .setPositiveButton(getResources().getString(R.string.alert_continue), onClickListener)
                        .setNegativeButton(getResources().getString(R.string.alert_cancel), onClickListener)
                        .show();
            } else {
                requestPermissions();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean bothGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (android.Manifest.permission.RECORD_AUDIO.equals(permissions[i]) || android.Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i])) {
                    bothGranted &= grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }
            if (bothGranted) {
                solicitarPermisos();
            } else {
                permissionNoGranted();
            }
        }
    }

    private void permissionNoGranted() {
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.msgDeniedPermitions), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE);
    }

}
