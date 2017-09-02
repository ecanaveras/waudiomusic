package com.ecanaveras.gde.waudio.task;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.ecanaveras.gde.waudio.models.Template;

/**
 * Created by elcap on 01/09/2017.
 */

public class FindVideoThumbnail extends AsyncTask<String, Void, Bitmap> {

    private Template template;

    public FindVideoThumbnail(Template template) {
        this.template = template;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        return ThumbnailUtils.createVideoThumbnail(this.template.getPathTemplateMp4(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        this.template.setImg_thumbnail(bitmap);
    }
}
