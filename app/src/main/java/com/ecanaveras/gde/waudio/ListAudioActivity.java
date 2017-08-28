package com.ecanaveras.gde.waudio;

import android.Manifest;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MergeCursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.util.ArrayList;

public class ListAudioActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int REQUEST_CODE = 1;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ListView listAudio;
    private Button btnNext;
    private ImageButton btnPause;
    private Cursor cursor;
    private int music_colum_idx;
    private int count;
    private MediaPlayer mediaPlayer;
    private String filename;
    private SimpleCursorAdapter mAdapter;
    private long back_pressed;
    private AudioManager audioManager;
    private SearchView mFilter;
    private Cursor mInternalCursor;
    private Cursor mExternalCursor;
    //private boolean mWasGetContentIntent;

    private AdapterView.OnItemClickListener MusicGridListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            cursor = mAdapter.getCursor();
            music_colum_idx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToPosition(i);
            filename = cursor.getString(music_colum_idx);
            btnNext.setEnabled(filename != null);
            btnPause.setEnabled(filename != null);

            try {
                if (mediaPlayer != null || mediaPlayer.isPlaying()) {
                    mediaPlayer.reset();
                }
                mediaPlayer.setDataSource(filename);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setContentView(R.layout.activity_list_audio);

        //Maneja el audio en llamadas
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        //Intent intent = getIntent();
        //mWasGetContentIntent = intent.getAction().equals(                Intent.ACTION_GET_CONTENT);

        listAudio = (ListView) findViewById(R.id.listAudio);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnPause = (ImageButton) findViewById(R.id.btnPause);
        btnNext.setEnabled(false);
        btnPause.setEnabled(false);


        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.pause();
                /*Intent intent = new Intent(getApplicationContext(), EditorVideoActivity.class);
                intent.putExtra("filename", filename);
                startActivity(intent);
                */
                try {
                    MainApp app = (MainApp) getApplicationContext();
                    app.setFilename(filename);
                    Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(filename));
                    //intent.putExtra("was_get_content_intent", mWasGetContentIntent);
                    intent.setClassName("com.ecanaveras.gde.waudio", "com.ecanaveras.gde.waudio.EditorActivity");
                    //startActivityForResult(intent, REQUEST_CODE_EDIT);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("Waudio", "Couldn't start editor_pref");
                }
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.pause();
            }
        });

        setupListView();
        listAudio.setOnItemClickListener(MusicGridListener);
        mediaPlayer = new MediaPlayer();

        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            setupListView();
            findMusic(null);
            mFirebaseAnalytics.setUserProperty("open_list_audio", String.valueOf(true));
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
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
                        .setTitle("Permisos para Waudio")
                        .setMessage(Html.fromHtml(getString(R.string.message_permissions)))
                        .setPositiveButton("Continuar", onClickListener)
                        .setNegativeButton("Cancelar", onClickListener)
                        .show();
            } else {
                requestPermissions();
            }
        }
    }

    /**
     * BUscas las canciones en el dispositivo
     */
    private void findMusic(String filter) {
        mInternalCursor = null;
        mExternalCursor = null;
        Bundle args = new Bundle();
        args.putString("filter", filter);
        //getLoaderManager().restartLoader(INTERNAL_CURSOR_ID, args, this);
        getLoaderManager().restartLoader(EXTERNAL_CURSOR_ID, args, this);
        /*
        String[] proj = {MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA};
        StringBuffer selection = new StringBuffer(MediaStore.Audio.Media.IS_MUSIC + " != 0");
        if (filter != null && !filter.isEmpty()) {
            selection.append(" AND (");
            selection.append("_DATA LIKE '%" + filter + "%'");
            selection.append("  OR TITLE LIKE '%" + filter + "%'");
            selection.append("  OR ARTIST LIKE '%" + filter + "%'");
            selection.append("  OR ALBUM LIKE '%" + filter + "%'");
            selection.append(")");
        }
        String orderby = "LOWER(" + MediaStore.Audio.Media.TITLE + ") ASC";
        cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, selection.toString(), null, orderby);
        Toast msg = Toast.makeText(getApplicationContext(), "No se han encontrado audios", Toast.LENGTH_SHORT);
        */

        /*if (cursor == null) {
            msg.show();
            return;
        }
        count = cursor.getCount();
        if (count == 0) {
            msg.show();
            return;
        }*/

        //System.out.println("canciones:" + count);
        /*List<String> audios = new ArrayList<String>();

        music_colum_idx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        while (cursor.moveToNext()) {
            audios.add(cursor.getString(music_colum_idx));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, audios);
        */
        //mAdapter.swapCursor(cursor);
        //listAudio.setAdapter(adapter);//listAudio.setAdapter(new AudioAdacter(getApplicationContext()));

    }

    private void setupListView() {
        try {
            mAdapter = new SimpleCursorAdapter(
                    this,
                    // Use a template that displays a text view
                    R.layout.media_select_row,
                    null,
                    // Map from database columns...
                    new String[]{
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.SIZE,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media._ID},
                    // To widget ids in the row layout...
                    new int[]{
                            R.id.row_artist,
                            R.id.row_size,
                            R.id.row_title
                    },
                    0);

            listAudio.setAdapter(mAdapter);

            listAudio.setItemsCanFocus(true);

            // Normal click - open the editor_pref
            /*getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id) {
                    startEditor();
                }
            });
            */

            //mInternalCursor = null;
            //mExternalCursor = null;
            //getLoaderManager().initLoader(INTERNAL_CURSOR_ID, null, this);
            //getLoaderManager().initLoader(EXTERNAL_CURSOR_ID, null, this);


        } catch (SecurityException e) {
            // No permission to retrieve audio?
            Log.e("Waudio", e.toString());

            // TODO error 1
        } catch (IllegalArgumentException e) {
            // No permission to retrieve audio?
            Log.e("Waudio", e.toString());

            // TODO error 2
        }

        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                /*if (view.getId() == R.id.row_options_button) {
                    // Get the arrow ImageView and set the onClickListener to open the context menu.
                    ImageView iv = (ImageView) view;
                    iv.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            openContextMenu(v);
                        }
                    });
                    return true;
                } else
                 * /
                if (view.getId() == R.id.row_icon) {
                    //setSoundIconFromCursor((ImageView) view, cursor);
                    return true;
                }*/


                return false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), getResources().getString(R.string.msgExit), Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_audio_options, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        /*mFilter = (SearchView) menu.findItem(R.id.action_search).getActionView();*/
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                public boolean onQueryTextChange(String newText) {
                    findMusic(newText);
                    return true;
                }

                public boolean onQueryTextSubmit(String query) {
                    findMusic(query);
                    return true;
                }
            });
        }

        return true;
    }

    private static final String[] INTERNAL_COLUMNS = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC,
            "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\""
    };

    private static final String[] EXTERNAL_COLUMNS = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC,
            "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\""
    };

    private static final int INTERNAL_CURSOR_ID = 0;
    private static final int EXTERNAL_CURSOR_ID = 1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        String[] proj = {""};
        Uri baseUri = null;
        switch (id) {
            case INTERNAL_CURSOR_ID:
                proj = INTERNAL_COLUMNS;
                baseUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
                break;
            case EXTERNAL_CURSOR_ID:
                proj = EXTERNAL_COLUMNS;
                baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
        }
        StringBuffer selection = new StringBuffer(MediaStore.Audio.Media.IS_MUSIC + " != 0");
        String filter = args != null ? args.getString("filter") : null;
        if (filter != null && !filter.isEmpty()) {
            filter = "%" + filter + "%";
            selection.append(" AND (");
            selection.append("_DATA LIKE ?");
            selection.append("  OR TITLE LIKE ?");
            selection.append("  OR ARTIST LIKE ?");
            selection.append("  OR ALBUM LIKE ?");
            selection.append("  OR _DISPLAY_NAME LIKE ?");
            selection.append(")");
            selection.append("  AND _DATA NOT LIKE ? ");
            selectionArgs.add(".mp3");
            selectionArgs.add(filter);
            selectionArgs.add(filter);
            selectionArgs.add(filter);
            selectionArgs.add(filter);
            selectionArgs.add("%espeak-data/scratch%");
        }
        //String orderby = "LOWER(" + MediaStore.Audio.Media.TITLE + ") ASC";
        //cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, selection.toString(), null, orderby);

        return new CursorLoader(this, baseUri, proj, selection.length() > 0 ? selection.toString() : null, selectionArgs.toArray(new String[selectionArgs.size()]), MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case INTERNAL_CURSOR_ID:
                mInternalCursor = data;
                break;
            case EXTERNAL_CURSOR_ID:
                mExternalCursor = data;
                break;
            default:
                return;
        }

        if (mExternalCursor != null) {
            Cursor mergeCursor = new MergeCursor(new Cursor[]{mExternalCursor});
            mAdapter.swapCursor(mergeCursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //case R.id.action_update:
            //    findMusic(null);
            //    break;
            case R.id.action_qualify:
                Uri uri = Uri.parse("market://details?id=" + this.getApplicationContext().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.urlPlayStore))));
                }
                break;
            case R.id.action_share:
                String msg1 = getResources().getString(R.string.msgShareApp);
                String urlPS = getResources().getString(R.string.urlPlayStore);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, String.format("%s: %s", msg1, urlPS));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.msgShareTo)));
                break;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_qualify).setVisible(true);
        menu.findItem(R.id.action_share).setVisible(true);
        return true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean bothGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i]) || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i])) {
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

    @Override
    public void onAudioFocusChange(int focusChange) {
        /*switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //mediaPlayer.start(); // Resume your media player here
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer != null)
                    mediaPlayer.pause();// Pause your media player here
                break;
        }
        */
    }

    /**
     * Adapter para la musica
     */
    private class AudioAdacter extends BaseAdapter {

        private Context context;

        public AudioAdacter(Context applicationContext) {
            this.context = applicationContext;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView lblTrack = new TextView(context.getApplicationContext());
            String id = null;
            if (view == null) {
                music_colum_idx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                cursor.moveToPosition(i);
                id = cursor.getString(music_colum_idx);
                //music_colum_idx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                //cursor.moveToPosition(i);
                //id += " Size(KB):"+cursor.getString(music_colum_idx);
                lblTrack.setText(id);
            } else {
                lblTrack = (TextView) view;
            }

            return lblTrack;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
    }
}
