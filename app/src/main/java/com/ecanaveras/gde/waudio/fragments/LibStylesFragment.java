package com.ecanaveras.gde.waudio.fragments;


import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.CardView;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ecanaveras.gde.waudio.LoadTemplates;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.StoreActivity;
import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ecanaveras on 28/08/2017.
 */

public class LibStylesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TemplateRecyclerAdapter templateRecyclerAdapter;
    private DataFirebaseHelper mDataFirebaseHelper;
    private DatabaseReference mRef;
    private LinearLayout lyContentItemStore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lib_styles, container, false);

        //Database
        mDataFirebaseHelper = new DataFirebaseHelper();
        mRef = mDataFirebaseHelper.getDatabaseReference(DataFirebaseHelper.REF_WAUDIO_TEMPLATES);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        lyContentItemStore = (LinearLayout) view.findViewById(R.id.lyContentItemStore);
        LinearLayout lyContentStore = (LinearLayout) view.findViewById(R.id.lyContentStore);
        lyContentStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGoStore(v);
            }
        });

        prepareTemplates();
        getNewItemsStore(3);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(templateRecyclerAdapter);
        return view;
    }

    public void getNewItemsStore(int limit) {
        final List<WaudioModel> waudioModelList = new ArrayList<>();
        mRef.orderByKey().limitToLast(limit).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    System.out.println("OBJECT " + data.getValue());
                    WaudioModel wt = data.getValue(WaudioModel.class);
                    waudioModelList.add(wt);
                }
                setupViewItemsStore(waudioModelList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setupViewItemsStore(List<WaudioModel> list) {
        if (list.isEmpty()) {
            ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.GONE);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Animation bounce = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.bounce);
        for (WaudioModel waudioModel : list) {
            CardView view = (CardView) inflater.inflate(R.layout.store_item_new, null);

            ImageView img = (ImageView) view.findViewById(R.id.thumbnail);
            //TextView title = (TextView) view.findViewById(R.id.title);
            //TextView category = (TextView) view.findViewById(R.id.category);

            view.setTag(waudioModel.getName());
            Picasso.with(getActivity()).load(waudioModel.getUrlThumbnail()).resize(160, 140).into(img);
            //title.setText(waudioModel.getName().replaceAll("_", " ").split("\\.")[0]);
            //category.setText(waudioModel.getCategory());
            lyContentItemStore.addView(view);
            view.startAnimation(bounce);
        }
        ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.VISIBLE);

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
                onGoStore(null);
                break;
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(isVisible());
    }

    public void onGoStore(View view) {
        Intent intent = new Intent(getActivity(), StoreActivity.class);
        startActivity(intent);
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
