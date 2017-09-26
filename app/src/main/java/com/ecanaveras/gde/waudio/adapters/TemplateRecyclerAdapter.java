package com.ecanaveras.gde.waudio.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.StoreActivity;
import com.ecanaveras.gde.waudio.WaudioFinalizedActivity;
import com.ecanaveras.gde.waudio.WaudioPreviewActivity;
import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.listener.ItemClickListener;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.ecanaveras.gde.waudio.picasso.VideoRequestHandler;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class TemplateRecyclerAdapter extends RecyclerView.Adapter<TemplateRecyclerAdapter.MyViewHolder> implements ItemClickListener {

    private Context mContext;
    private List<WaudioModel> waudioModelList;
    private GeneratorWaudio generatorWaudio;
    private VideoRequestHandler videoRequestHandler;
    private Picasso picassoInstance;
    private int mLayout;
    private boolean isRemote;

    public TemplateRecyclerAdapter(Context context, int mLayout, List<WaudioModel> waudioModelList, boolean isRemote) {
        this.mLayout = mLayout;
        this.mContext = context;
        this.waudioModelList = waudioModelList;
        this.isRemote = isRemote;
    }

    public TemplateRecyclerAdapter(Context context, int mLayout, List<WaudioModel> waudioModelList) {
        this.mLayout = mLayout;
        this.mContext = context;
        this.waudioModelList = waudioModelList;
        videoRequestHandler = new VideoRequestHandler(VideoRequestHandler.MINI_KIND);
        picassoInstance = new Picasso.Builder(context.getApplicationContext())
                .addRequestHandler(videoRequestHandler)
                .build();
    }


    public TemplateRecyclerAdapter(Context context, List<WaudioModel> waudioModelList, GeneratorWaudio generatorWaudio) {
        this.mLayout = R.layout.media_template_card;
        this.mContext = context;
        this.waudioModelList = waudioModelList;
        this.generatorWaudio = generatorWaudio;
        videoRequestHandler = new VideoRequestHandler(VideoRequestHandler.MINI_KIND);
        picassoInstance = new Picasso.Builder(context.getApplicationContext())
                .addRequestHandler(videoRequestHandler)
                .build();
    }

    @Override
    public void onItemClick(View view, int position) {
        WaudioModel waudioModel = waudioModelList.get(position);
        Intent intent = null;
        //Action in Library
        if (mLayout == R.layout.media_style_card) {
            switch (view.getId()) {
                case R.id.btnDelete:
                    break;
                case R.id.btnFavorite:
                    break;
                default: //Vista previa o Store
                    if (isRemote) {
                        showDownloadDialog(waudioModel);
                    } else {
                        intent = new Intent(mContext, WaudioPreviewActivity.class);
                        intent.putExtra(WaudioPreviewActivity.PATH_WAUDIO, waudioModel.getPathMp4());
                        mContext.startActivity(intent);
                    }
                    break;
            }

        }
        //Action in Waudio Creation
        if (mLayout == R.layout.media_template_card) {
            if (generatorWaudio != null) {
                switch (view.getId()) {
                    case R.id.btnPreview:
                        intent = new Intent(mContext, WaudioPreviewActivity.class);
                        intent.putExtra(WaudioPreviewActivity.PATH_WAUDIO, waudioModel.getPathMp4());
                        break;
                    default:
                        MainApp app = (MainApp) mContext.getApplicationContext();
                        CompareWaudio cw = new CompareWaudio(generatorWaudio.getTitle().toString(), waudioModel.getPathMp4(), generatorWaudio.getEndTime());
                        if (app.WaudioExist(cw)) {
                            showAlert(app.getCompareWaudioTmp().getPathWaudio().getAbsolutePath());
                        } else {
                            generatorWaudio.setOutFileWaudio(null);
                            generatorWaudio.generateWaudio(waudioModel.getPathMp4());
                            intent = new Intent(mContext, WaudioFinalizedActivity.class);
                            intent.putExtra(WaudioFinalizedActivity.TEMPLATE_USED, waudioModel.getPathMp4());
                        }
                        break;
                }
                if (intent != null)
                    mContext.startActivity(intent);
            } else {
                Log.e(TemplateRecyclerAdapter.class.getName(), "GeneratorWaudio is null");
                Toast.makeText(mContext, mContext.getResources().getString(R.string.msgProblemGenerateWaudio), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public TemplateRecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(mLayout, parent, false);
        return new MyViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(final TemplateRecyclerAdapter.MyViewHolder holder, int position) {
        WaudioModel waudioModel = waudioModelList.get(position);
        holder.name.setText(waudioModel.getName());
        holder.category.setText(waudioModel.getCategory());
        if (isRemote) {
            Picasso.with(mContext).load(waudioModel.getUrlThumbnail()).into(holder.thumbnail);
        } else
            picassoInstance.load(VideoRequestHandler.SCHEME_VIDEO + ":" + waudioModel.getPathMp4()).into(holder.thumbnail);
    }

    @Override
    public int getItemCount() {
        return waudioModelList.size();
    }

    public WaudioModel getTemplate(int position) {
        return waudioModelList.get(position);
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

    private void showDownloadDialog(WaudioModel model) {
        StoreActivity activity = (StoreActivity) mContext;
        activity.onClicDownloadItem(model);
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
            if (btnPreview != null)
                btnPreview.setOnClickListener(this);
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(view, getAdapterPosition());
        }
    }

}
