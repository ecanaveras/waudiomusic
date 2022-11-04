package com.ecanaveras.gde.waudio;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;
import java.sql.SQLOutput;

import es.dmoral.toasty.Toasty;

public class PermitionsActivity extends AppCompatActivity {

    com.ecanaveras.gde.waudio.util.PreferenceManager preferenceManager;
    private static final int REQUEST_CODE = 1;
    private MainApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permitions);
        app = (MainApp) getApplicationContext();
        if (app.checkPermitions())
            gotoActivity();
    }

    public void onSetupPermitions(View view) {
        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (app.checkPermitions()) {
            gotoActivity();
        } else {
            //Explica la necesidad del permiso, esto sucede cuando el usuario ha denegado anteriormente el permiso
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
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
                        .setTitle(getString(R.string.alert_title_permitions))
                        .setMessage(Html.fromHtml(getString(R.string.message_permissions)))
                        .setPositiveButton(getString(R.string.alert_continue), onClickListener)
                        .setNegativeButton(getString(R.string.alert_cancel), onClickListener)
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
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i]) || Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                        bothGranted &= grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                }
            }
            if (bothGranted) {
                gotoActivity();
            } else {
                permissionNoGranted();
            }
        }
    }

    private void permissionNoGranted() {
        Toasty.error(getApplicationContext(), getString(R.string.msgDeniedPermitions), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE);
    }

    private void gotoActivity() {
        preferenceManager = new com.ecanaveras.gde.waudio.util.PreferenceManager(this);
        Intent mainIntent = null;
        if (app.foundWaudios()) {
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
