package com.ecanaveras.gde.waudio.adapters;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ecanaveras.gde.waudio.R;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by ecanaveras on 30/09/2017.
 */

public class MusicListAdapter extends SimpleCursorAdapter {

    public MusicListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        TextView txtSize = (TextView) view.findViewById(R.id.row_size);

        int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
        txtSize.setText(readableFileDuration(cursor.getLong(sizeColumn)));
    }

    private static String readableFileDuration(long duration) {
        //return DateUtils.formatElapsedTime(duration);
        return String.format("%2d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }

    private static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
