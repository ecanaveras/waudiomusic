package com.ecanaveras.gde.waudio.adapters;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ecanaveras.gde.waudio.MainActivity;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.WaudioPreviewActivity;
import com.ecanaveras.gde.waudio.models.Template;
import com.ecanaveras.gde.waudio.task.FindVideoThumbnail;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by elcap on 29/08/2017.
 */

public class TemplateListAdapter extends SimpleCursorAdapter {

    private Cursor dataSet;
    private Context mContext;
    private LayoutInflater inflater;
    private int mLayout;
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

    //View
    private static class ViewHolder {
        ImageView thumbnail;
        TextView waudioName;
        TextView date;
        ImageButton btnShare;
        ImageButton btnDelete;
    }

    public TemplateListAdapter(Context context, int layout, Cursor dataSet, String[] from, int[] to, int flags) {
        super(context, layout, dataSet, from, to, flags);
        this.dataSet = dataSet;
        this.mContext = context;
        this.mLayout = layout;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(mLayout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            viewHolder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            viewHolder.waudioName = (TextView) view.findViewById(R.id.txtWaudioName);
            viewHolder.date = (TextView) view.findViewById(R.id.txtDateWaudio);
            //viewHolder.btnDelete = (ImageButton) view.findViewById(R.id.btnDelete);
            //viewHolder.btnShare = (ImageButton) view.findViewById(R.id.btnShare);

            //result = convertView;
            view.setTag(viewHolder);
        }

        int columnId = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        int columnDate = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);

        Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
        new FindVideoThumbnail(waudio).execute();
        viewHolder.waudioName.setText(waudio.getName().replace("WAUDIO-", ""));
        //Bitmap bitmap = getThumbnail(mContext.getContentResolver(), cursor.getInt(columnId));
        viewHolder.thumbnail.setImageBitmap(waudio.getImg_thumbnail());
        viewHolder.date.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date(cursor.getLong(columnDate) * 1000L)));
    }

    public void openWaudio(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        String pathWaudio = cursor.getString(columnPath);
        Intent goIntent = new Intent(mContext, WaudioPreviewActivity.class);
        goIntent.putExtra(WaudioPreviewActivity.PATH_WAUDIO, pathWaudio);
        mContext.startActivity(goIntent);
    }


    public void shareSelection() {
        if (mSelection.size() > 1) {
            return;
        }
        Cursor cursor = getCursor();
        Template waudio = null;
        for (Integer position : mSelection.keySet()) {
            cursor.moveToPosition(position);
            int columnName = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
        }
        if (waudio != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(waudio.getPathTemplateMp4()));
            sendIntent.putExtra(Intent.EXTRA_TEXT, mContext.getResources().getString(R.string.hastag));
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("video/mp4");
            mContext.startActivity(Intent.createChooser(sendIntent, mContext.getResources().getString(R.string.msgShareWith)));
        }
        clearSelection();
    }

    public void deleteSelection() {
        if (mSelection.size() == 0) {
            return;
        }
        Cursor cursor = getCursor();
        for (Integer position : mSelection.keySet()) {
            cursor.moveToPosition(position);
            int columnName = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
            File wDel = new File(waudio.getPathTemplateMp4());
            if (wDel.exists()) {
                wDel.delete();
            }
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(wDel)));
        }
        Snackbar.make(((MainActivity) mContext).findViewById(android.R.id.content), String.format(mContext.getResources().getString(R.string.info_contextmenu_deleted_waudio), mSelection.size()), Snackbar.LENGTH_SHORT).show();
        clearSelection();
    }

    public boolean isPositionChecked(int position) {
        Boolean result = mSelection.get(position);
        return result == null ? false : result;
    }

    public void setNewSelection(int position, boolean value) {
        mSelection.put(position, value);
        notifyDataSetChanged();
    }

    public void removeSelection(int position) {
        mSelection.remove(position);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        mSelection = new HashMap<Integer, Boolean>();
        notifyDataSetChanged();
    }

    public static Bitmap getThumbnail(ContentResolver cr, int idImage) {
        return MediaStore.Video.Thumbnails.getThumbnail(cr, idImage, MediaStore.Video.Thumbnails.MICRO_KIND, null);
    }
}
