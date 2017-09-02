package com.ecanaveras.gde.waudio.picasso;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

/**
 * Created by elcap on 02/09/2017.
 */

public class VideoRequestHandler extends RequestHandler {

    public static final int MICRO_KIND = MediaStore.Video.Thumbnails.MICRO_KIND;
    public static final int MINI_KIND = MediaStore.Video.Thumbnails.MINI_KIND;
    public static final String SCHEME_VIDEO = "video";
    private int typeKind;

    public VideoRequestHandler(int typeKind) {
        this.typeKind = typeKind;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        String scheme = data.uri.getScheme();
        return (SCHEME_VIDEO.equals(scheme));
    }

    @Override
    public Result load(Request data, int arg1) throws IOException {
        Bitmap bm = ThumbnailUtils.createVideoThumbnail(data.uri.getPath(), typeKind);
        return new Result(bm, Picasso.LoadedFrom.DISK);
    }
}
