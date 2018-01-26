package com.ecanaveras.gde.waudio;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.util.FontsOverride;
import com.ecanaveras.gde.waudio.util.Mp4Filter;
import com.google.firebase.crash.FirebaseCrash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ecanaveras on 05/08/2017.
 */

public class MainApp extends Application {

    public static final String ADMOB_APP_ID = "ca-app-pub-4587362379324712~6454573814";
    public static final String PATH_VIDEOS = "/Waudio/Media/Waudio Videos/";
    public static final String POINTS = "points";

    private GeneratorWaudio generatorWaudio;
    private List<CompareWaudio> compareWaudios = new ArrayList<CompareWaudio>();
    private CompareWaudio compareWaudioTmp;
    private String filename;
    private SharedPreferences preferences;
    SharedPreferences.Editor editor_pref;
    public boolean reloadWaudios = true;
    public boolean isFirstSearchMusic = false;
    private DataFirebaseHelper mDataFirebaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseCrash.setCrashCollectionEnabled(!BuildConfig.DEBUG);
        mDataFirebaseHelper = new DataFirebaseHelper();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor_pref = preferences.edit();
        setupFonts();
        if (preferences.getBoolean("new_instance", true)) {
            //removeAssetsOld();
            copyAssets();
            setupPoints();
        }
    }

    private void setupFonts() {
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/Dosis-Light.ttf");
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Dosis-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/Dosis-Medium.ttf");
        FontsOverride.setDefaultFont(this, "DEFAULT_BOLD", "fonts/Dosis-Bold.ttf");
    }

    private void setupPoints() {
        editor_pref.putInt(POINTS, 500);
        editor_pref.commit();
        Log.d("POINTS SETUP", "OK");
    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("Waudio", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                File outFile = new File(getExternalFilesDir(null), filename);
                if (!outFile.exists()) {
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                } //else Log.i("Waudio", outFile.getName() + " ya existe");

            } catch (FileNotFoundException e) {
                Log.e("Waudio", "Asset File Not Found: " + e.getMessage());
            } catch (IOException e) {
                Log.e("Waudio", "Failed to copy asset file: " + filename, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
        //Guardar data
        editor_pref.putBoolean("new_instance", false);
        editor_pref.commit();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void removeAssetsOld() {
        File dir = new File(getExternalFilesDir(null).getAbsolutePath());
        if (dir.exists()) {
            for (String name : dir.list(new Mp4Filter(".mp4"))) {
                File vmp4 = new File(dir.getAbsolutePath() + "/" + name);
                if (vmp4.exists()) {
                    vmp4.delete();
                }
                Log.e(MainApp.class.getSimpleName(), "Asset: " + name + " delete");
            }
        }
    }

    public GeneratorWaudio getGeneratorWaudio() {
        return generatorWaudio;
    }

    public void setGeneratorWaudio(GeneratorWaudio generatorWaudio) {
        this.generatorWaudio = generatorWaudio;
    }

    public void addNewWaudio(CompareWaudio compareWaudio) {
        compareWaudios.add(compareWaudio);
    }

    public void removeWaudio(CompareWaudio compareWaudio) {
        compareWaudios.remove(compareWaudio);
    }

    public boolean WaudioExist(CompareWaudio compareWaudio) {
        for (CompareWaudio cw : compareWaudios) {
            //System.out.println("MAINAPP " + cw.getTitle() + " " + cw.getTemplate() + " " + cw.getEntTime());
            if (cw.equals(compareWaudio)) {
                this.compareWaudioTmp = cw;
                return true;
            }
        }
        return false;
    }

    public long findNewItemStore(long cantItemStore) {
        long lastItemOnline = preferences.getLong("countItemOnline", 0);
        editor_pref.putLong("countItemOnline", cantItemStore);
        editor_pref.commit();
        return cantItemStore - lastItemOnline;
    }

    public int getCountWaudioCreated() {
        return preferences.getInt("countWaudioCreated", 0);
    }

    public void incrementCountWaudioCreated() {
        int cant = getCountWaudioCreated();
        editor_pref.putInt("countWaudioCreated", ++cant);
        editor_pref.commit();
    }

    public void decrementCountWaudioCreated() {
        editor_pref.putInt("countWaudioCreated", 2);
        editor_pref.commit();
    }

    public boolean getMyRating() {
        if (preferences.getBoolean("raiting", false)) {
            return false; //No pedir calificación;
        }
        //Pedir calificación si ha realizado mas de 5 Waudios y no a calificado
        int cant = getCountWaudioCreated();
        return cant >= 5;
    }

    public void saveRating() {
        editor_pref.putBoolean("raiting", true);
        editor_pref.commit();
    }

    public int updatePoints(int valor, boolean increment) {
        return updatePoints(valor, increment, false);
    }

    public int updatePoints(int valor, boolean increment, boolean isAdmod) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor_pref = preferences.edit();

        int point = preferences.getInt(MainApp.POINTS, 0);
        if (increment) {
            point += valor;
            if (isAdmod) {
                mDataFirebaseHelper.incrementWaudioPointsAdmob(valor);
            } else {
                mDataFirebaseHelper.incrementWaudioPoints(valor);
            }
        } else {
            point = point - valor;
            if (valor > 0)
                mDataFirebaseHelper.incrementWaudioPointsConsumed(valor);
        }
        editor_pref.putInt(MainApp.POINTS, point);
        editor_pref.commit();
        return point;
    }

    public CompareWaudio getCompareWaudioTmp() {
        return compareWaudioTmp;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
