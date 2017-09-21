package com.ecanaveras.gde.waudio.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

import com.ecanaveras.gde.waudio.MainActivity;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.WaudioPreviewActivity;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.ecanaveras.gde.waudio.picasso.VideoRequestHandler;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Created by elcap on 29/08/2017.
 */

public class WaudioListAdapter extends SimpleCursorAdapter implements View.OnClickListener {

    private FirebaseAnalytics mFirebaseAnalytics;
    private DataFirebaseHelper mDataFirebaseHelper;

    private Context mContext;
    private LayoutInflater inflater;
    private int mLayout;
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();
    private VideoRequestHandler videoRequestHandler;
    private Picasso picassoInstance;

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnWaudioShare) {
            shareWaudio((Integer) v.getTag());
        }
    }

    //View
    private static class ViewHolder {
        ImageView thumbnail;
        TextView waudioName;
        TextView date;
        TextView size;
        ImageButton btnShare;
        ImageButton btnDelete;
    }

    public WaudioListAdapter(Context context, int layout, Cursor dataSet, String[] from, int[] to, int flags) {
        super(context, layout, dataSet, from, to, flags);
        this.mContext = context;
        this.mLayout = layout;
        this.inflater = LayoutInflater.from(context);
        videoRequestHandler = new VideoRequestHandler(VideoRequestHandler.MICRO_KIND);
        picassoInstance = new Picasso.Builder(context.getApplicationContext())
                .addRequestHandler(videoRequestHandler)
                .build();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(mContext);
        mDataFirebaseHelper = new DataFirebaseHelper();
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
            viewHolder.btnShare = (ImageButton) view.findViewById(R.id.btnWaudioShare);
            viewHolder.size = (TextView) view.findViewById(R.id.txtSizeWaudio);
            viewHolder.btnShare.setOnClickListener(this);

            view.setTag(viewHolder);
        }

        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        int columnDate = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
        int columnSize = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

        WaudioModel waudio = new WaudioModel(cursor.getString(columnName), cursor.getString(columnPath), cursor.getLong(columnDate), cursor.getLong(columnSize));
        viewHolder.waudioName.setText(waudio.getName().replace("WAUDIO-", ""));
        picassoInstance.load(VideoRequestHandler.SCHEME_VIDEO + ":" + waudio.getPathMp4()).into(viewHolder.thumbnail);
        viewHolder.date.setText(new SimpleDateFormat("dd/MM/yyyy").format(waudio.getDate()));
        viewHolder.size.setText(waudio.getSizeFormat());
        viewHolder.btnShare.setTag(cursor.getPosition());
    }


    public void openWaudio(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        String pathWaudio = cursor.getString(columnPath);
        Intent goIntent = new Intent(mContext, WaudioPreviewActivity.class);
        goIntent.putExtra(WaudioPreviewActivity.PATH_WAUDIO, pathWaudio);
        goIntent.getBooleanExtra(WaudioPreviewActivity.IS_WAUDIO, true);
        mContext.startActivity(goIntent);
    }

    public void shareWaudio(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        int columnName = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
        int columnPath = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        WaudioModel waudio = new WaudioModel(cursor.getString(columnName), cursor.getString(columnPath));
        ;
        if (waudio != null) {
            mFirebaseAnalytics.setUserProperty("share", String.valueOf(true));
            mDataFirebaseHelper.incrementWaudioShared();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(waudio.getPathMp4()));
            sendIntent.putExtra(Intent.EXTRA_TEXT, mContext.getResources().getString(R.string.hastag));
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("video/mp4");
            mContext.startActivity(Intent.createChooser(sendIntent, mContext.getResources().getString(R.string.msgShareWith)));
        }
    }

    public void shareSelection() {
        if (mSelection.size() > 1) {
            return;
        }

        for (Integer position : mSelection.keySet()) {
            shareWaudio(position);
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
            WaudioModel waudio = new WaudioModel(cursor.getString(columnName), cursor.getString(columnPath));
            File wDel = new File(waudio.getPathMp4());
            if (wDel.exists()) {
                wDel.delete();
            }
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(wDel)));
        }
        mDataFirebaseHelper.incrementWaudioDeleted(mSelection.size());
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
}
