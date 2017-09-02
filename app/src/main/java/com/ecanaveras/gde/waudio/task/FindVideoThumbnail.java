package com.ecanaveras.gde.waudio.task;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.ecanaveras.gde.waudio.models.WaudioModel;

/**
 * Created by elcap on 01/09/2017.
 */

public class FindVideoThumbnail extends AsyncTask<String, Void, Bitmap> {

    private WaudioModel waudioModel;
    private ImageView imageView;
    private int typeThumbnail;


    public FindVideoThumbnail(WaudioModel waudioModel, ImageView imageView, int typeThumbnail) {
        this.waudioModel = waudioModel;
        this.imageView = imageView;
        this.typeThumbnail = typeThumbnail;
    }

    public FindVideoThumbnail(WaudioModel waudioModel, int typeThumbnail) {
        this.waudioModel = waudioModel;
        this.typeThumbnail = typeThumbnail;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        return ThumbnailUtils.createVideoThumbnail(this.waudioModel.getPathMp4(), typeThumbnail);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        this.waudioModel.setImgThumbnail(bitmap);
        if (this.imageView != null) {
            this.imageView.setImageBitmap(bitmap);
        }
    }
}
