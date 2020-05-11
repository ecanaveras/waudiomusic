package com.ecanaveras.gde.waudio;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;

import es.dmoral.toasty.Toasty;

public class PermitionsActivity extends AppCompatActivity {

    com.ecanaveras.gde.waudio.util.PreferenceManager preferenceManager;
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permitions);
        if (((MainApp) getApplicationContext()).checkPermitions())
            gotoActivity();
    }

    public void onSetupPermitions(View view) {
        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (((MainApp) getApplicationContext()).checkPermitions()) {
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
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i]) || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
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
        Toasty.error(getApplicationContext(), getResources().getString(R.string.msgDeniedPermitions), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE);
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

    private void gotoActivity() {
        preferenceManager = new com.ecanaveras.gde.waudio.util.PreferenceManager(this);
        Intent mainIntent = null;
        if (foundWaudios()) {
            mainIntent = new Intent(PermitionsActivity.this, MainActivity.class);
        } else {
            //TODO, mostrar un asistente para crear Waudio, Grabar, o hacer ringtone
            mainIntent = new Intent(PermitionsActivity.this, ListAudioActivity.class);
        }

        mainIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(mainIntent);
        finish();
    }
}
