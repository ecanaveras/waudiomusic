package com.ecanaveras.gde.waudio.controllers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.BuildConfig;
import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.R;
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
    private boolean isMultiplesFiles;

    /**
     * Controla las acciones de un Waudio(s)
     * @param context
     * @param fileWaudio
     * @param isMultiplesFiles Multiples Waudios?
     */
    public WaudioController(Context context, File fileWaudio, boolean isMultiplesFiles) {
        this.fileWaudio = fileWaudio;
        this.context = context;
        this.isMultiplesFiles = isMultiplesFiles;
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
        String msg = context.getString(R.string.msgConfirmDelete);
        if (isTemplate) {
            msg = context.getString(R.string.msgConfirmTemplateDelete);
        }
        if (fileWaudio.exists()) {
            if(isMultiplesFiles){
                delete();
            }else {
                new AlertDialog.Builder(context, R.style.AlertDialogCustom)
                        .setTitle(context.getString(R.string.msgDeleteWaudio))
                        .setMessage(msg)
                        .setPositiveButton(
                                context.getString(R.string.msgYesDelete),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        delete();
                                        Activity activity = (Activity) context;
                                        activity.finish();
                                    }
                                })
                        .setCancelable(true)
                        .setNegativeButton(context.getString(R.string.alert_cancel), null)
                        .show();
            }
        }
    }

    /**
     * Elimina un Waudio y notifica en un broadcast
     */
    private void delete(){
        if (fileWaudio.getName().toUpperCase().contains("PAZ ROMANCE")) {
            Toasty.warning(context, context.getString(R.string.msgDenegateTemplateDelete), Toast.LENGTH_SHORT).show();
            return;
        }
        if (fileWaudio.delete()) {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileWaudio)));
            CompareWaudio cw = app.getCompareWaudioTmp();
            if (app.WaudioExist(cw))
                app.removeWaudio(cw);
            app.reloadWaudios = true;
            mDataFirebaseHelper.incrementWaudioDeleted();
        } else {
            Toasty.error(context, context.getString(R.string.msgErrorDeleteWaudio), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Comparte un Waudio
     * @param activity
     */
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
        sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.hastag));
        sendIntent.setType("video/*");
        if (activity != null) {
            activity.startActivityForResult(Intent.createChooser(sendIntent, context.getString(R.string.msgShareWith)), SHARE_WAUDIO_REQUEST);
        } else {
            ((Activity) context).startActivityForResult(Intent.createChooser(sendIntent, context.getString(R.string.msgShareWith)), SHARE_WAUDIO_REQUEST);
        }
    }

    public void addPoints(int points) {
        //Sumar puntos
        if (app != null) {
            app.updatePoints(points, true);
            Toasty.custom(context, "+" + points + " " + context.getString(R.string.lblPoints), context.getDrawable(R.drawable.ic_points), context.getColor(R.color.colorAccent), Toast.LENGTH_SHORT, true, true).show();
        }
    }

    public void onGoEditor() {
        if (fileWaudio == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_EDIT, Uri.fromFile(fileWaudio));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setData(Uri.fromFile(fileWaudio));
        } else {
            intent.setData(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", fileWaudio));
        }
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setDataAndType(Uri.fromFile(fileWaudio), "video/mp4");
        } else {
            intent.setDataAndType(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", fileWaudio), "video/mp4");
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.msgOpenWith)));
        } else {
            Toasty.warning(context, context.getString(R.string.msgOpenWithFailded), Toast.LENGTH_SHORT).show();
        }
        /*
        // Verify it resolves
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        if (activities.size() > 0) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.msgOpenWith)));
        } else {
            Toasty.warning(context, context.getString(R.string.msgOpenWithFailded), Toast.LENGTH_SHORT).show();
        }*/
    }


}
