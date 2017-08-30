package com.ecanaveras.gde.waudio.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.PreviewActivity;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.models.Template;
import com.ecanaveras.gde.waudio.editor.CompareWaudio;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.listener.ItemClickListener;

import java.util.List;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class TemplatesAdapter extends RecyclerView.Adapter<TemplatesAdapter.MyViewHolder> implements ItemClickListener, View.OnTouchListener {

    private Context mContext;
    private List<Template> templateList;
    private GeneratorWaudio generatorWaudio;

    final float[] historicX = {Float.NaN};
    final float[] historicY = {Float.NaN};
    final int DELTA = 50;

    public TemplatesAdapter(Context mContext, List<Template> templateList, GeneratorWaudio generatorWaudio) {
        this.mContext = mContext;
        this.templateList = templateList;
        this.generatorWaudio = generatorWaudio;
    }

    @Override
    public void onItemClick(View view, int position) {
        Template template = templateList.get(position);
        if (generatorWaudio != null) {
            MainApp app = (MainApp) mContext.getApplicationContext();
            CompareWaudio cw = new CompareWaudio(generatorWaudio.getTitle().toString(), template.getPathTemplateMp4(), generatorWaudio.getEndTime());
            if (app.WaudioExist(cw)) {
                showAlert(app.getCompareWaudioTmp().getPathWaudio().getAbsolutePath());
            } else {
                generatorWaudio.setOutFileWaudio(null);
                generatorWaudio.generateWaudio(template.getPathTemplateMp4());
                Intent intent = new Intent(mContext, PreviewActivity.class);
                mContext.startActivity(intent);
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                historicX[0] = event.getX();
                historicY[0] = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                if (event.getX() - historicX[0] < -DELTA) {
                    //FunctionDeleteRowWhenSlidingLeft();
                    return true;
                } else if (event.getX() - historicX[0] > DELTA) {
                    //FunctionDeleteRowWhenSlidingRight();
                    System.out.println("DERECHA");
                    return true;
                }
                break;

            default:
                return false;
        }
        return false;
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
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(template.getPathTemplateMp4(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
        holder.thumbnail.setImageBitmap(bitmap);

        /*MediaController controller = new MediaController(mContext);
        controller.setMediaPlayer(holder.thumbnail);
        try {
            holder.thumbnail.setMediaController(controller);
            controller.setAnchorView(holder.thumbnail);
            holder.thumbnail.requestFocus();
            holder.thumbnail.start();
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    holder.thumbnail.pause();
                }
            }, 500);


        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), e.getMessage());
        }*/
    }

    @Override
    public int getItemCount() {
        return templateList.size();
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
                        Intent intent = new Intent(mContext, PreviewActivity.class);
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
        public Button btnCrear;


        public MyViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.title);
            category = (TextView) itemView.findViewById(R.id.category);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            btnCrear = (Button) itemView.findViewById(R.id.btnCrearWaudio);

            /*thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    thumbnail.requestFocus();
                    thumbnail.start();
                }
            });*/

            btnCrear.setOnClickListener(this);
            name.setOnClickListener(this);
            category.setOnClickListener(this);
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(view, getAdapterPosition());
        }
    }

}
