package com.ecanaveras.gde.waudio.fragments;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.SplashScreen;
import com.ecanaveras.gde.waudio.adapters.TemplateListAdapter;
import com.ecanaveras.gde.waudio.models.Template;
import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ecanaveras on 28/08/2017.
 */

public class LibWaudiosFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String PATH_VIDEOS = "/Waudio/Media/Waudio Videos/";
    private static final int EXTERNAL_CURSOR_ID = 1;

    private ListView listWaudios;
    private TemplateListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lib_waudios, container, false);
        listWaudios = (ListView) view.findViewById(R.id.listWaudios);
        if (Build.VERSION.SDK_INT >= 21)
            listWaudios.setNestedScrollingEnabled(true);
        setupListView();
        findWaudios();
        return view;
    }

    private void setupListView() {
        try {
            mAdapter = new TemplateListAdapter(
                    getActivity(),
                    // Use a template that displays a text view
                    R.layout.media_select_waudio,
                    null,
                    // Map from database columns...
                    new String[]{
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media._ID},
                    // To widget ids in the row layout...
                    new int[]{
                            R.id.txtWaudioName
                    },
                    0);

            //mAdapter.fragment = this;
            listWaudios.setAdapter(mAdapter);
            listWaudios.setItemsCanFocus(true);

        } catch (SecurityException e) {
            // No permission to retrieve audio?
            Log.e("Waudio", e.toString());

            // TODO error 1
        } catch (IllegalArgumentException e) {
            // No permission to retrieve audio?
            Log.e("Waudio", e.toString());

            // TODO error 2
        }
    }

    public void findWaudios() {
        getLoaderManager().restartLoader(EXTERNAL_CURSOR_ID, null, this);
    }

    private static final String[] EXTERNAL_COLUMNS = new String[]{
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            "\"" + MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "\""
    };

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        String[] proj = EXTERNAL_COLUMNS;
        Uri baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        StringBuffer selection = new StringBuffer(MediaStore.Video.Media.DATA + " LIKE '%.mp4'");
        selection.append("  AND _DATA LIKE ? ");
        selection.append("  AND _DATA NOT LIKE ? ");
        selectionArgs.add("%" + Environment.getExternalStorageDirectory().getPath() + PATH_VIDEOS + "%");
        selectionArgs.add("%espeak-data/scratch%");
        return new CursorLoader(getActivity(), baseUri, proj, selection.toString(), selectionArgs.toArray(new String[selectionArgs.size()]), MediaStore.Video.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        mAdapter.swapCursor(data);
        //mAdapter.changeCursor(data);
        Log.i("FindWaudios", data.getCount() + " waudios");
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }
}
