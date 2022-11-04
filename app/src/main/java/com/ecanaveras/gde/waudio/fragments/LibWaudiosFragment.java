package com.ecanaveras.gde.waudio.fragments;


import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.ecanaveras.gde.waudio.listener.TemplatesFileObserver;
import com.google.android.material.snackbar.Snackbar;
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
import android.widget.LinearLayout;
import android.widget.ListView;

import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.adapters.WaudioListAdapter;

import java.util.ArrayList;

/**
 * Created by ecanaveras on 28/08/2017.
 */

public class LibWaudiosFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXTERNAL_CURSOR_ID = 1;
    private static final String PREF_ORDERBY = "orderby_waudio";

    private ListView listWaudios;
    private WaudioListAdapter mAdapter;
    private LinearLayout layoutWait, layoutContent;
    private Cursor data;
    private String orderBy;
    private String orderbyDefault = MediaStore.Video.Media.DATE_MODIFIED + " DESC";
    private String orderbyTitle = MediaStore.Video.Media.TITLE;

    private SharedPreferences preferences;
    SharedPreferences.Editor editor_pref;

    private TemplatesFileObserver observer;
    public boolean refresh = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lib_waudios, container, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor_pref = preferences.edit();

        layoutWait = (LinearLayout) view.findViewById(R.id.layoutWait);
        layoutContent = (LinearLayout) view.findViewById(R.id.layoutContent);

        listWaudios = (ListView) view.findViewById(R.id.listWaudios);
        if (Build.VERSION.SDK_INT >= 21) {
            listWaudios.setNestedScrollingEnabled(true);
        }
        setupListView();
        orderBy = preferences.getString(PREF_ORDERBY, orderbyDefault);
        findWaudios();

        //Si cambian los waudios, actualiza el listado
        System.out.println("Ruta Waudios:"+Environment.getExternalStorageDirectory().getPath() + MainApp.PATH_VIDEOS);
        observer = new TemplatesFileObserver(Environment.getExternalStorageDirectory().getPath() + MainApp.PATH_VIDEOS);
        observer.setWaudiosFragment(this);
        observer.startWatching();

        return view;
    }

    public void findWaudios() {
        if (!refresh) {
            return;
        }
        Bundle args = new Bundle();
        args.putString("orderby", orderBy);
        getLoaderManager().restartLoader(EXTERNAL_CURSOR_ID, args, this);
        this.refresh = false;
    }

    private static final String[] EXTERNAL_COLUMNS = new String[]{
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE
    };

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        String[] proj = EXTERNAL_COLUMNS;
        Uri baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        StringBuffer selection = new StringBuffer(MediaStore.Video.Media.DATA + " LIKE '%.mp4'");
        selection.append("  AND _DATA LIKE ? ");
        selection.append("  AND _DATA NOT LIKE ? ");
        selectionArgs.add("%" + Environment.getExternalStorageDirectory().getPath() + MainApp.PATH_VIDEOS + "%");
        selectionArgs.add("%espeak-data/scratch%");
        return new CursorLoader(getActivity(), baseUri, proj, selection.toString(), selectionArgs.toArray(new String[selectionArgs.size()]), args.getString("orderby"));
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_lib_waudios_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_orderby:
                String lblOrder = getString(R.string.lblOrderbyDate);
                if (orderBy.equals(orderbyDefault)) {
                    orderBy = orderbyTitle;
                    lblOrder = getString(R.string.lblOrderbyTitle);
                } else {
                    orderBy = orderbyDefault;
                }
                findWaudios();
                //Guardar la preferencia
                editor_pref.putString(PREF_ORDERBY, orderBy);
                editor_pref.commit();
                Snackbar.make(getView(), String.format(getString(R.string.formatOrderby), lblOrder), Snackbar.LENGTH_SHORT).show();
                break;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(isVisible());
        if(refresh){
            findWaudios();
        }
    }

    private void setupListView() {
        try {
            mAdapter = new WaudioListAdapter(
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

        } catch (IllegalArgumentException e) {
            // No permission to retrieve audio?
            Log.e("Waudio", e.toString());
        }

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
                                .setTitle(getActivity().getString(R.string.msgDeleteWaudio))
                                .setMessage(String.format(getActivity().getString(R.string.formatContextMenuDeleteWaudio), nr))
                                .setPositiveButton(
                                        getActivity().getString(R.string.msgYesDelete),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int whichButton) {
                                                mAdapter.deleteSelection();
                                            }
                                        })
                                .setCancelable(true)
                                .setNegativeButton(getActivity().getString(R.string.alert_cancel), null)
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
                if (mAdapter != null)
                    mAdapter.openWaudio(position);
            }
        });

        listWaudios.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mAdapter != null)
                    listWaudios.setItemChecked(position, !mAdapter.isPositionChecked(position));
                return false;
            }
        });
    }
}
