package com.ecanaveras.gde.waudio.fragments;


import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.transition.Visibility;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
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
            //listWaudios.setNestedScrollingEnabled(true);
            listWaudios.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listWaudios.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int nr = 0;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked) {
                    nr++;
                    mAdapter.setNewSelection(position, checked);
                } else {
                    nr--;
                    mAdapter.removeSelection(position);
                }
                if (mode.getMenu() != null) {
                    if (nr == 1)
                        mode.getMenu().getItem(0).setVisible(true);
                    else
                        mode.getMenu().getItem(0).setVisible(false);
                }
                mode.setTitle(nr + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                nr = 0;
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_waudio_options, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_share:
                        mAdapter.shareSelection();
                        break;
                    case R.id.action_delete:
                        new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom)
                                .setTitle(getActivity().getResources().getString(R.string.msgDeleteWaudio))
                                .setMessage(String.format(getActivity().getResources().getString(R.string.formatContextMenuDeleteWaudio), nr))
                                .setPositiveButton(
                                        getActivity().getResources().getString(R.string.msgYesDelete),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int whichButton) {
                                                mAdapter.deleteSelection();
                                            }
                                        })
                                .setCancelable(true)
                                .setNegativeButton(getActivity().getResources().getString(R.string.alert_cancel), null)
                                .show();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAdapter.clearSelection();
            }
        });
        listWaudios.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.openWaudio(position);
            }
        });

        listWaudios.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                listWaudios.setItemChecked(position, !mAdapter.isPositionChecked(position));
                return false;
            }
        });
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
            mAdapter.notifyDataSetChanged();

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
