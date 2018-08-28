package com.ecanaveras.gde.waudio;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.models.WaudioModel;
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

    public static final String ADMOB_APP_ID = "ca-app-pub-4587362379324712~6454573814";
    public static final String PATH_VIDEOS = "/Waudio/Media/Waudio Videos/";
    public static final String POINTS = "points";
    public static final Integer POINTS_WAUDIO_CREATED = 10;
    public static final Integer POINTS_WAUDIO_SHARED = 25;
    public static final Integer POINTS_VIDEO_VIEW = 100;

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
        mDataFirebaseHelper = new DataFirebaseHelper();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor_pref = preferences.edit();
        setupFonts();
        if (!preferences.getString("version_name", "0").equals(BuildConfig.VERSION_NAME)) {
            //removeAssetsOld();
            copyAssets();
            if (preferences.getInt(POINTS, 0) == 0) {
                setupPoints();
            }
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
        editor_pref.putString("version_name", BuildConfig.VERSION_NAME);
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

    //Banners
    public static List<WaudioModel> getListBannerWS() {
        ArrayList<WaudioModel> list = new ArrayList<>();
        WaudioModel w1 = new WaudioModel("Atardecer Romance", R.drawable.banner1);
        WaudioModel w2 = new WaudioModel("Ella People", R.drawable.banner2);
        WaudioModel w3 = new WaudioModel("Electric Guitar Rock", R.drawable.banner3);

        WaudioModel w4 = new WaudioModel("Dani Aventure", R.drawable.banner4);
        WaudioModel w5 = new WaudioModel("Indira Anime", R.drawable.banner5);
        WaudioModel w6 = new WaudioModel("Inglaterra mundo", R.drawable.banner6);

        WaudioModel w7 = new WaudioModel("Johan Urbano", R.drawable.banner7);
        WaudioModel w8 = new WaudioModel("Kary Amistad", R.drawable.banner8);
        WaudioModel w9 = new WaudioModel("Kelly Romance", R.drawable.banner9);

        WaudioModel w10 = new WaudioModel("Kenya Libertad", R.drawable.banner10);
        WaudioModel w11 = new WaudioModel("Kley General", R.drawable.banner11);
        WaudioModel w12 = new WaudioModel("Motorcycle Aventure", R.drawable.banner12);

        WaudioModel w13 = new WaudioModel("Paz Romance", R.drawable.banner13);
        WaudioModel w14 = new WaudioModel("Saxo General", R.drawable.banner14);
        WaudioModel w15 = new WaudioModel("Tu Y Yo Amor", R.drawable.banner15);

        WaudioModel w16 = new WaudioModel("Vallenato Colombia", R.drawable.banner16);
        WaudioModel w17 = new WaudioModel("Inolvidable Romance", R.drawable.banner17);
        WaudioModel w18 = new WaudioModel("Jenny General", R.drawable.banner18);

        WaudioModel w19 = new WaudioModel("Kriss Urbano", R.drawable.banner19);
        WaudioModel w20 = new WaudioModel("Lina Urbano", R.drawable.banner20);


        list.add(w1);
        list.add(w2);
        list.add(w3);
        list.add(w4);
        list.add(w5);
        list.add(w6);
        list.add(w7);
        list.add(w8);
        list.add(w9);
        list.add(w10);
        list.add(w11);
        list.add(w12);
        list.add(w13);
        list.add(w14);
        list.add(w15);
        list.add(w16);
        list.add(w17);
        list.add(w18);
        list.add(w19);
        list.add(w20);

        return list;
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
