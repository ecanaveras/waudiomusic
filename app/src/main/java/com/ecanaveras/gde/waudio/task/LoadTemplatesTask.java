package com.ecanaveras.gde.waudio.task;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by elcap on 02/09/2017.
 */

public class LoadTemplatesTask extends AsyncTask<String, Void, ArrayList<WaudioModel>> {

    private TemplateRecyclerAdapter recyclerAdapter;
    private GeneratorWaudio generatorWaudio;
    private Context mContext;
    private ArrayList<WaudioModel> waudioModelList = new ArrayList<>();
    private RecyclerView recyclerView;
    private LinearLayout layoutWait, layoutContent;

    /**
     * Metodo para los estilos descargados en el dispositivo
     *
     * @param mContext
     * @param adapter
     * @param view
     * @param lw
     * @param lc
     * @param generatorWaudio
     */
    public LoadTemplatesTask(Context mContext, TemplateRecyclerAdapter adapter, RecyclerView view, LinearLayout lw, LinearLayout lc, GeneratorWaudio generatorWaudio) {
        this.recyclerAdapter = adapter;
        this.generatorWaudio = generatorWaudio;
        this.mContext = mContext;
        this.recyclerView = view;
        this.layoutContent = lc;
        this.layoutWait = lw;
    }

    @Override
    protected void onPreExecute() {
        if (layoutWait != null)
            layoutWait.setVisibility(View.VISIBLE);

    }

    @Override
    protected ArrayList<WaudioModel> doInBackground(String... params) {
        if (recyclerView == null) {
            return null;
        }
        //Manejor de Archivos privados
        File dir = new File(params[0]);
        WaudioModel waudioModel;
        if (dir.exists()) {
            for (String name : dir.list(new Mp4Filter(".mp4"))) {
                boolean category = name.split("_").length > 1;
                String path = dir.getAbsolutePath() + "/" + name;
                waudioModel = new WaudioModel(name.split("_")[0], path, category ? name.split("_")[1].replace(".mp4", "") : "General");
                //new FindVideoThumbnail(waudioModel, MediaStore.Video.Thumbnails.MINI_KIND).execute();
                waudioModel.setImgThumbnail(ThumbnailUtils.createVideoThumbnail(waudioModel.getPathMp4(), MediaStore.Video.Thumbnails.MINI_KIND));
                waudioModelList.add(waudioModel);
            }
        }
        return waudioModelList;
    }

    @Override
    protected void onPostExecute(ArrayList<WaudioModel> data) {
        if (recyclerView != null) {
            recyclerAdapter = new TemplateRecyclerAdapter(mContext, data, generatorWaudio);
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
        if (layoutWait != null)
            layoutWait.setVisibility(View.GONE);
        if (layoutContent != null)
            layoutContent.setVisibility(View.VISIBLE);

    }


    /**
     * Busca el thumbnail en videos publicos en la SD
     *
     * @param cr
     * @param idImage
     * @return
     */
    public static Bitmap getThumbnail(ContentResolver cr, int idImage) {
        return MediaStore.Video.Thumbnails.getThumbnail(cr, idImage, MediaStore.Video.Thumbnails.MICRO_KIND, null);
    }
}
