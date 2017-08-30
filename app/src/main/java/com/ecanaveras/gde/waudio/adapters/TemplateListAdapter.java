package com.ecanaveras.gde.waudio.adapters;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.ecanaveras.gde.waudio.fragments.LibWaudiosFragment;
import com.ecanaveras.gde.waudio.models.Template;

import java.io.File;

/**
 * Created by elcap on 29/08/2017.
 */

public class TemplateListAdapter extends SimpleCursorAdapter implements View.OnClickListener {

    private Cursor dataSet;
    private Context mContext;
    private LayoutInflater inflater;
    private int mLayout;
    private File wDel;

    //View
    private static class ViewHolder {
        ImageView thumbnail;
        TextView waudioName;
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
    public void onClick(final View v) {
        int position = (int) v.getTag();
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));
        switch (v.getId()) {
            case R.id.btnShare:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(waudio.getPathTemplateMp4()));
                sendIntent.putExtra(Intent.EXTRA_TEXT, mContext.getResources().getString(R.string.hastag));
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType("video/mp4");
                mContext.startActivity(Intent.createChooser(sendIntent, mContext.getResources().getString(R.string.msgShareWith)));
                break;
            case R.id.btnDelete:
                wDel = new File(waudio.getPathTemplateMp4());
                //mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(wDel)));
                if (wDel.exists()) {
                    new AlertDialog.Builder(mContext, R.style.AlertDialogCustom)
                            .setTitle(mContext.getResources().getString(R.string.msgDeleteWaudio))
                            .setMessage(String.format(mContext.getResources().getString(R.string.formatDeleteWaudio), waudio.getName()))
                            .setPositiveButton(
                                    mContext.getResources().getString(R.string.msgYesDelete),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int whichButton) {
                                            if (wDel.delete()) {
                                                //fragment.findWaudios();
                                                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(wDel)));
                                                Snackbar.make(v, mContext.getResources().getString(R.string.info_deleted_waudio), Snackbar.LENGTH_SHORT).show();
                                            }


                                        }
                                    })
                            .setCancelable(true)
                            .setNegativeButton(mContext.getResources().getString(R.string.alert_cancel), null)
                            .show();
                }
                break;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            viewHolder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            viewHolder.waudioName = (TextView) view.findViewById(R.id.txtWaudioName);
            viewHolder.btnDelete = (ImageButton) view.findViewById(R.id.btnDelete);
            viewHolder.btnShare = (ImageButton) view.findViewById(R.id.btnShare);

            //result = convertView;
            view.setTag(viewHolder);
        }

        int columnId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        Template waudio = new Template(cursor.getString(columnName), cursor.getString(columnPath));

        viewHolder.waudioName.setText(waudio.getName());
        Bitmap bitmap = getThumbnail(mContext.getContentResolver(), cursor.getInt(columnId));
        viewHolder.thumbnail.setImageBitmap(bitmap);
        viewHolder.btnShare.setOnClickListener(this);
        viewHolder.btnDelete.setOnClickListener(this);
        viewHolder.btnShare.setTag(cursor.getPosition());
        viewHolder.btnDelete.setTag(cursor.getPosition());

    }

    public static Bitmap getThumbnail(ContentResolver cr, int idImage) {
        return MediaStore.Video.Thumbnails.getThumbnail(cr, idImage, MediaStore.Video.Thumbnails.MICRO_KIND, null);
    }
}
