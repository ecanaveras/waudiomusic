package com.ecanaveras.gde.waudio.fragments;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.ImageView;

import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.squareup.picasso.Picasso;

/**
 * Created by elcap on 26/09/2017.
 */

public class DownloadBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private WaudioModel item;

    public DownloadBottomSheetDialogFragment(WaudioModel i) {
        this.item = i;
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
        View contentView = View.inflate(getContext(), R.layout.fragment_download_bottomsheet, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        ImageView imageView = (ImageView) contentView.findViewById(R.id.thumbnail);
        Picasso.with(getContext()).load(item.getUrlThumbnail()).into(imageView);

        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetCallback);
        }
    }
}
