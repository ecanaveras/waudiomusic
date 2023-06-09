package com.ecanaveras.gde.waudio.listener;

import android.os.FileObserver;
import android.util.Log;

import com.ecanaveras.gde.waudio.ListTemplateActivity;
import com.ecanaveras.gde.waudio.fragments.LibStylesFragment;
import com.ecanaveras.gde.waudio.fragments.LibWaudiosFragment;

import java.io.File;

/**
 * Created by ecanaveras on 09/10/2017.
 */

public class TemplatesFileObserver extends FileObserver {

    static final String TAG = "TemplatesFileObserver";

    private LibStylesFragment stylesFragment;
    private LibWaudiosFragment waudiosFragment;
    private ListTemplateActivity activity;
    String rootPath;

    static final int mask = (FileObserver.CREATE |
            FileObserver.DELETE |
            FileObserver.DELETE_SELF |
            FileObserver.MODIFY |
            FileObserver.MOVED_FROM |
            FileObserver.MOVED_TO |
            FileObserver.MOVE_SELF);


    public TemplatesFileObserver(String path) {
        super(path, mask);

        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        rootPath = path;
    }

    @Override
    public void onEvent(int event, String path) {
        Log.i(TAG, " RUNNING, EVENT " + event);
        switch (event) {
            case FileObserver.CREATE:
            case FileObserver.DELETE:
            case FileObserver.DELETE_SELF:
            case FileObserver.MODIFY:
            case FileObserver.MOVED_FROM:
            case FileObserver.MOVED_TO:
            case FileObserver.MOVE_SELF:
                if (stylesFragment != null)
                    stylesFragment.refresh = true;
                if(waudiosFragment != null)
                    //TODO CORREGIR REfresh de Waudios
                    waudiosFragment.refresh = true;
                if (activity != null)
                    activity.refresh = true;
                break;
            default:
                // just ignore
                break;
        }
    }

    public void setActivity(ListTemplateActivity activity) {
        this.activity = activity;
    }

    public void setStylesFragment(LibStylesFragment stylesFragment) {
        this.stylesFragment = stylesFragment;
    }
    public void setWaudiosFragment(LibWaudiosFragment waudiosFragment) {
        this.waudiosFragment = waudiosFragment;
    }
}
