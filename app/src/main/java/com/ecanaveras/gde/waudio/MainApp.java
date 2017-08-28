package com.ecanaveras.gde.waudio;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.util.FontsOverride;
import com.ecanaveras.gde.waudio.util.Mp4Filter;

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

    private GeneratorWaudio generatorWaudio;
    private List<CompareWaudio> compareWaudios = new ArrayList<CompareWaudio>();
    private CompareWaudio compareWaudioTmp;
    private String filename;
    private SharedPreferences preferences;
    SharedPreferences.Editor editor_pref;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor_pref = preferences.edit();
        setupFonts();
        if (preferences.getBoolean("remove_assets", true)) {
            removeAssetsOld();
        }
        if (preferences.getBoolean("copy_assets", true)) {
            copyAssets();
        }
    }

    private void setupFonts() {
        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/Dosis-Light.ttf");
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Dosis-Medium.ttf");
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/Dosis-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/Dosis-SemiBold.ttf");
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
        editor_pref.putBoolean("copy_assets", false);
        editor_pref.commit();
        System.out.println("Waudio: Assets copy success");
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
            //Guardar data
            editor_pref.putBoolean("remove_assets", false);
            editor_pref.commit();
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
