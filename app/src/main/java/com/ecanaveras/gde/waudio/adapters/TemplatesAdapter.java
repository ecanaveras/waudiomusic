package com.ecanaveras.gde.waudio.adapters;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.WaudioFinalizedActivity;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.WaudioPreviewActivity;
import com.ecanaveras.gde.waudio.models.Template;
import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.listener.ItemClickListener;
import com.ecanaveras.gde.waudio.task.FindVideoThumbnail;

import java.io.File;
import java.util.List;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class TemplatesAdapter extends RecyclerView.Adapter<TemplatesAdapter.MyViewHolder> implements ItemClickListener {

    private Context mContext;
    private List<Template> templateList;
    private GeneratorWaudio generatorWaudio;

    public TemplatesAdapter(Context mContext, List<Template> templateList, GeneratorWaudio generatorWaudio) {
        this.mContext = mContext;
        this.templateList = templateList;
        this.generatorWaudio = generatorWaudio;
    }

    @Override
    public void onItemClick(View view, int position) {
        Template template = templateList.get(position);
        if (generatorWaudio != null) {
            Intent intent = null;
            switch (view.getId()) {
                case R.id.btnPreview:
                    intent = new Intent(mContext, WaudioPreviewActivity.class);
                    intent.putExtra(WaudioPreviewActivity.PATH_WAUDIO, template.getPathTemplateMp4());
                    break;
                default:
                    MainApp app = (MainApp) mContext.getApplicationContext();
                    CompareWaudio cw = new CompareWaudio(generatorWaudio.getTitle().toString(), template.getPathTemplateMp4(), generatorWaudio.getEndTime());
                    if (app.WaudioExist(cw)) {
                        showAlert(app.getCompareWaudioTmp().getPathWaudio().getAbsolutePath());
                    } else {
                        generatorWaudio.setOutFileWaudio(null);
                        generatorWaudio.generateWaudio(template.getPathTemplateMp4());
                        intent = new Intent(mContext, WaudioFinalizedActivity.class);
                    }
                    break;
            }
            if (intent != null)
                mContext.startActivity(intent);
        } else {
            Log.e(TemplatesAdapter.class.getName(), "GeneratorWaudio is null");
            Toast.makeText(mContext, mContext.getResources().getString(R.string.msgProblemGenerateWaudio), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public TemplatesAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.template_card, parent, false);
        return new MyViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(final TemplatesAdapter.MyViewHolder holder, int position) {
        Template template = templateList.get(position);
        holder.name.setText(template.getName());
        holder.category.setText(template.getCategory());
        //holder.thumbnail.setVideoPath(template.getPathTemplateMp4());
        //Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(template.getPathTemplateMp4(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
        //new FindVideoThumbnail(holder.thumbnail).execute(template);
        holder.thumbnail.setImageBitmap(template.getImg_thumbnail());
    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    public static Bitmap getThumbnail(ContentResolver cr, String path) {
        Cursor ca = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID}, MediaStore.MediaColumns.DATA + ".=?", new String[]{path}, null);
        if (ca != null && ca.moveToFirst()) {
            int id = ca.getInt(ca.getColumnIndex(MediaStore.MediaColumns._ID));
            ca.close();
            return MediaStore.Video.Thumbnails.getThumbnail(cr, id, MediaStore.Video.Thumbnails.MINI_KIND, null);
        } else {
            Log.d(TemplatesAdapter.class.getName(), "Thumbnail no found, path:" + path);
        }
        ca.close();
        return null;
    }

    public Template getTemplate(int position) {
        return templateList.get(position);
    }


    private void showAlert(final String pathFile) {
        new android.app.AlertDialog.Builder(mContext, R.style.AlertDialogCustom)
                .setTitle("Hey...")
                .setMessage(mContext.getResources().getString(R.string.msgStyleChoosed))
                .setPositiveButton(mContext.getResources().getString(R.string.alert_ok_preview), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(mContext, WaudioFinalizedActivity.class);
                        intent.putExtra("waudio", pathFile);
                        mContext.startActivity(intent);
                    }
                })
                .setNegativeButton("NO", null)
                .setCancelable(true)
                .show();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


        public ItemClickListener listener;

        public TextView name, category;
        public ImageView thumbnail;
        public ImageButton btnPreview;
        public TextView txtNext;


        public MyViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.title);
            category = (TextView) itemView.findViewById(R.id.category);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            btnPreview = (ImageButton) itemView.findViewById(R.id.btnPreview);
            txtNext = (TextView) itemView.findViewById(R.id.txtNext);

            itemView.setOnClickListener(this);
            btnPreview.setOnClickListener(this);
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(view, getAdapterPosition());
        }
    }

}
