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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.WaudioPreviewActivity;
import com.ecanaveras.gde.waudio.models.Template;

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
    private File wDel;
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();
    private View view;

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
        view = inflater.inflate(mLayout, parent, false);
        return view;
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

        int columnId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
        File wFile = new File(waudio.getPathTemplateMp4());

        viewHolder.waudioName.setText(waudio.getName().replace("WAUDIO-", ""));
        Bitmap bitmap = getThumbnail(mContext.getContentResolver(), cursor.getInt(columnId));
        viewHolder.thumbnail.setImageBitmap(bitmap);
        viewHolder.date.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date(wFile.lastModified())));
        /*viewHolder.btnShare.setOnClickListener(this);
        viewHolder.btnDelete.setOnClickListener(this);
        viewHolder.btnShare.setTag(cursor.getPosition());
        viewHolder.btnDelete.setTag(cursor.getPosition());*/

    }

    public void openWaudio(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
        Intent goIntent = new Intent(mContext, WaudioPreviewActivity.class);
        goIntent.putExtra("path_waudio", waudio.getPathTemplateMp4());
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
            int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
            int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
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
            int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
            int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
            File wDel = new File(waudio.getPathTemplateMp4());
            if (wDel.exists()) {
                wDel.delete();
            }
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(wDel)));
        }
        Snackbar.make(view, String.format(mContext.getResources().getString(R.string.info_contextmenu_deleted_waudio), mSelection.size()), Snackbar.LENGTH_SHORT).show();
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
