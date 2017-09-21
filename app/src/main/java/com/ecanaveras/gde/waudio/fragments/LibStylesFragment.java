package com.ecanaveras.gde.waudio.fragments;


import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ecanaveras.gde.waudio.LoadTemplates;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.StoreActivity;
import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;

/**
 * Created by ecanaveras on 28/08/2017.
 */

public class LibStylesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TemplateRecyclerAdapter templateRecyclerAdapter;
    private DataFirebaseHelper mDataFirebaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lib_styles, container, false);

        //Database
        mDataFirebaseHelper = new DataFirebaseHelper();

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        prepareTemplates();

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(templateRecyclerAdapter);
        return view;
    }

    /**
     * Templates para crear Waudios
     */
    private void prepareTemplates() {
        LoadTemplates templates = new LoadTemplates(".mp4", getActivity().getExternalFilesDir(null).getAbsolutePath());
        templateRecyclerAdapter = new TemplateRecyclerAdapter(getActivity(), R.layout.media_style_card, templates.getWaudioModelList());
        templateRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_lib_styles_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_goto_store:
                //Snackbar.make(getView(), getResources().getString(R.string.msgComingSoonStore), Snackbar.LENGTH_SHORT).show();
                //mDataFirebaseHelper.incrementGotoStore()
                Intent intent = new Intent(getActivity(), StoreActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(isVisible());
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            //TODO Revisar Codigo que renderiza el RecyclerView
            int position = parent.getChildAdapterPosition(view); //item position
            int column = position % spanCount; //item column;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }

        }

    }

    private int dpTopz(int dp) {
        Resources resources = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()));
    }

}
