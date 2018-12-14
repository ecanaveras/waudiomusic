package com.ecanaveras.gde.waudio.fragments;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.squareup.picasso.Picasso;

/**
 * Created by elcap on 26/09/2017.
 */

public class DownloadDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private WaudioModel waudioModel;
    private ImageView imgThumbnail;
    private VideoView videoView;

    public static DownloadDialogFragment newInstance(WaudioModel model) {
        DownloadDialogFragment fragment = new DownloadDialogFragment();
        fragment.setWaudioModel(model);
        return fragment;
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.custom_dialog_fragment_download, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        imgThumbnail = (ImageView) contentView.findViewById(R.id.thumbnail);
        TextView txtSizeWaudio = (TextView) contentView.findViewById(R.id.txtSizeWaudio);
        TextView txtCostPoints = (TextView) contentView.findViewById(R.id.txtCostPoints);
        TextView txtTitle = (TextView) contentView.findViewById(R.id.title);
        TextView txtCategory = (TextView) contentView.findViewById(R.id.category);
        ImageButton btnPreview = (ImageButton) contentView.findViewById(R.id.btnPreview);
        videoView = (VideoView) contentView.findViewById(R.id.videoView);

        //Data
        txtTitle.setText(waudioModel.getSimpleName());
        txtCategory.setText(waudioModel.getCategory());
        Picasso.with(getContext()).load(waudioModel.getUrlThumbnail()).into(imgThumbnail);
        txtSizeWaudio.setText(waudioModel.getSizeFormat());
        if (waudioModel.getValue() == null || waudioModel.getValue() == 0) {
            txtCostPoints.setText(getResources().getString(R.string.lblFree));
        } else
            txtCostPoints.setText(String.valueOf(waudioModel.getValue()));

        btnPreview.setOnClickListener(this);
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetCallback);
        }
    }

    @Override
    public void onClick(View view) {
        if (imgThumbnail != null) {
            imgThumbnail.setVisibility(View.GONE);
        }
        if (videoView != null) {
            videoView.setVisibility(View.VISIBLE);
            //MediaController controller = new MediaController(getContext());
            //controller.setAnchorView(controller);
            //videoView.setMediaController(controller);
            videoView.setVideoURI(Uri.parse(waudioModel.getPathMp4()));
            videoView.setKeepScreenOn(true);
            videoView.requestFocus();
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    imgThumbnail.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                }
            });
            videoView.start();
        }
    }

    public void setWaudioModel(WaudioModel waudioModel) {
        this.waudioModel = waudioModel;
    }
}

