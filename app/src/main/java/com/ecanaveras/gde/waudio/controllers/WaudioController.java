package com.ecanaveras.gde.waudio.controllers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.BuildConfig;
import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.WaudioFinalizedActivity;
import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;

import es.dmoral.toasty.Toasty;

/**
 * Created by elcap on 27/09/2017.
 */

public class WaudioController {

    private static final int SHARE_WAUDIO_REQUEST = 1;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DataFirebaseHelper mDataFirebaseHelper;

    private WaudioModel waudioModel;
    private File fileWaudio;
    private Context context;
    private MainApp app;

    public WaudioController(Context context, File fileWaudio) {
        this.fileWaudio = fileWaudio;
        this.context = context;
        app = (MainApp) context.getApplicationContext();
        initFirebase();
    }

    public WaudioController(Context context, WaudioModel waudioModel) {
        this.waudioModel = waudioModel;
        this.context = context;
        this.fileWaudio = new File(waudioModel.getPathMp4());
        app = (MainApp) context.getApplicationContext();
        initFirebase();
    }

    private void initFirebase() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mDataFirebaseHelper = new DataFirebaseHelper();
    }

    public void onDelete() {
        onDelete(false);
    }

    public void onDeleteTemplate() {
        onDelete(true);
    }

    private void onDelete(boolean isTemplate) {
        if (fileWaudio == null) {
            return;
        }
        String msg = context.getResources().getString(R.string.msgConfirmDelete);
        if (isTemplate) {
            msg = context.getResources().getString(R.string.msgConfirmTemplateDelete);
        }
        if (fileWaudio.exists()) {
            new AlertDialog.Builder(context, R.style.AlertDialogCustom)
                    .setTitle(context.getResources().getString(R.string.msgDeleteWaudio))
                    .setMessage(msg)
                    .setPositiveButton(
                            context.getResources().getString(R.string.msgYesDelete),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    if (fileWaudio.getName().toUpperCase().contains("HEADSET")) {
                                        Toasty.warning(context, context.getResources().getString(R.string.msgDenegateTemplateDelete), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (fileWaudio.delete()) {
                                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileWaudio)));
                                        CompareWaudio cw = app.getCompareWaudioTmp();
                                        if (app.WaudioExist(cw))
                                            app.removeWaudio(cw);
                                        app.reloadWaudios = true;
                                        mDataFirebaseHelper.incrementWaudioDeleted();
                                        Activity activity = (Activity) context;
                                        activity.finish();
                                    } else {
                                        Toasty.error(context, context.getResources().getString(R.string.msgErrorDeleteWaudio), Toast.LENGTH_SHORT).show();
                                    }

                                }
                            })
                    .setCancelable(true)
                    .setNegativeButton(context.getResources().getString(R.string.alert_cancel), null)
                    .show();
        }
    }


    public void onShare(Activity activity) {
        if (fileWaudio == null) {
            return;
        }
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { //Menor que ANdroid 7
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileWaudio));
        } else {
            //Android 7 o superior
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", fileWaudio));
        }
        sendIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.hastag));
        sendIntent.setType("video/*");
        if (activity != null) {
            activity.startActivityForResult(Intent.createChooser(sendIntent, context.getResources().getString(R.string.msgShareWith)), SHARE_WAUDIO_REQUEST);
        } else {
            ((Activity) context).startActivityForResult(Intent.createChooser(sendIntent, context.getResources().getString(R.string.msgShareWith)), SHARE_WAUDIO_REQUEST);
        }
    }

    public void addPoints(int points) {
        //Sumar puntos
        if (app != null) {
            app.updatePoints(points, true);
            Toasty.custom(context, "+" + points + " " + context.getResources().getString(R.string.lblPoints), context.getResources().getDrawable(R.drawable.ic_points), context.getResources().getColor(R.color.colorAccent), Toast.LENGTH_SHORT, true, true).show();
        }
    }

    public void onGoEditor() {
        if (fileWaudio == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_EDIT, Uri.fromFile(fileWaudio));
        //intent.putExtra("was_get_content_intent", mWasGetContentIntent);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setClassName("com.ecanaveras.gde.waudio", "com.ecanaveras.gde.waudio.EditorActivity");
        //startActivityForResult(intent, REQUEST_CODE_EDIT);
        context.startActivity(intent);
    }

    public void onGoFile() {
        if (fileWaudio == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(fileWaudio);
        intent.setDataAndType(uri, "video/*");
        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.msgOpenWith)));
    }


}
