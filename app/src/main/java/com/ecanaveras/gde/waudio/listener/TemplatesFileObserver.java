package com.ecanaveras.gde.waudio.listener;

import android.os.FileObserver;

import com.ecanaveras.gde.waudio.fragments.LibStylesFragment;

import java.io.File;

/**
 * Created by ecanaveras on 09/10/2017.
 */

public class TemplatesFileObserver extends FileObserver {

    static final String TAG = "TemplatesFileObserver";

    private LibStylesFragment fragment;
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
        System.out.println(TAG + " RUNNING, EVENT " + event);
        switch (event) {
            case FileObserver.CREATE:
            case FileObserver.DELETE:
            case FileObserver.DELETE_SELF:
            case FileObserver.MODIFY:
            case FileObserver.MOVED_FROM:
            case FileObserver.MOVED_TO:
            case FileObserver.MOVE_SELF:
                fragment.refresh = true;
                break;
            default:
                // just ignore
                break;
        }
    }

    public void setActivity(LibStylesFragment fragment) {
        this.fragment = fragment;
    }
}
