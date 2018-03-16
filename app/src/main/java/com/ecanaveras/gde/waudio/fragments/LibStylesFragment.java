package com.ecanaveras.gde.waudio.fragments;


import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.TextView;

import com.ecanaveras.gde.waudio.LoadTemplates;
import com.ecanaveras.gde.waudio.MainActivity;
import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.StoreActivity;
import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.listener.TemplatesFileObserver;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.google.android.gms.internal.zzagr.runOnUiThread;

/**
 * Created by ecanaveras on 28/08/2017.
 */

public class LibStylesFragment extends Fragment {

    private static Random random = new Random();
    private RecyclerView recyclerView;
    private TemplateRecyclerAdapter templateRecyclerAdapter;
    private DataFirebaseHelper mDataFirebaseHelper;
    private DatabaseReference mRef;
    private LinearLayout lyContentItemStore;
    private LoadTemplates templates;
    private List<WaudioModel> storeWaudioModelList = new ArrayList<>();
    private List<WaudioModel> sdWaudioModelList = new ArrayList<>();
    private MainApp app;
    private TemplatesFileObserver observer;
    public boolean refresh = true;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lib_styles, container, false);

        //Database
        mDataFirebaseHelper = new DataFirebaseHelper();
        mRef = mDataFirebaseHelper.getDatabaseReference(DataFirebaseHelper.REF_WAUDIO_TEMPLATES);

        app = (MainApp) getActivity().getApplicationContext();

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        lyContentItemStore = (LinearLayout) view.findViewById(R.id.lyContentItemStore);
        LinearLayout lyContentStore = (LinearLayout) view.findViewById(R.id.lyContentStore);
        lyContentStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGoStore(v);
            }
        });

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        app.reloadWaudios = true;
        prepareTemplates();
        getNewItemsStore(3);

        //Si cambian los templates, actualiza el listado
        observer = new TemplatesFileObserver(getActivity().getExternalFilesDir(null).getAbsolutePath());
        observer.setFragment(this);
        observer.startWatching();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        countItemsStore();
    }

    public void getNewItemsStore(final int limit) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //
                storeWaudioModelList.clear();
                for (int i = 0; i < limit; i++) {
                    storeWaudioModelList.add(getRandomBanner(getListBannerWS()));
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        setupViewItemsStore(storeWaudioModelList);
                    }
                });

            }
        }.start();
    }

    private void setupViewItemsStore(List<WaudioModel> list) {
        if (list.isEmpty() || getActivity() == null) {
            ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.GONE);
            return;
        }
        lyContentItemStore.removeAllViewsInLayout();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Animation bounce = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.bounce);
        for (WaudioModel waudioModel : list) {
            CardView view = (CardView) inflater.inflate(R.layout.store_item_new, null);

            ImageView img = (ImageView) view.findViewById(R.id.thumbnail);
            TextView title = (TextView) view.findViewById(R.id.title);
            //TextView category = (TextView) view.findViewById(R.id.category);

            Picasso.with(getActivity()).load(waudioModel.getResourceId()).resize(160, 140).into(img);
            title.setText(waudioModel.getSimpleName());
            //category.setText(waudioModel.getCategory());
            lyContentItemStore.addView(view);
            view.startAnimation(bounce);
        }
        ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.VISIBLE);
        MainActivity activity = (MainActivity) getActivity();
        activity.refreshNameTabs(list.size());
    }

    private void countItemsStore() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_new_items, null);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (app.findNewItemStore(dataSnapshot.getChildrenCount()) > 0) {
                    AlertDialog.Builder info = new AlertDialog.Builder(getActivity())
                            .setView(dialogView)
                            .setPositiveButton(getResources().getString(R.string.alert_ok_new_styles), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onGoStore(null);
                                }
                            }).setNegativeButton("OK", null);
                    info.show();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Templates para crear Waudios
     */
    public void prepareTemplates() {
        if (!refresh) {
            return;
        }
        sdWaudioModelList.clear();
        templates = new LoadTemplates(".mp4", getActivity().getExternalFilesDir(null).getAbsolutePath());
        sdWaudioModelList = templates.getSdWaudioModelList();
        templateRecyclerAdapter = new TemplateRecyclerAdapter(getActivity(), R.layout.media_style_card, sdWaudioModelList);
        recyclerView.setAdapter(templateRecyclerAdapter);
        templateRecyclerAdapter.notifyDataSetChanged();
        refresh = false;
    }

    private List<WaudioModel> getListBannerWS() {
        ArrayList<WaudioModel> list = new ArrayList<>();
        WaudioModel w1 = new WaudioModel("Atardecer Romance", R.drawable.banner1);
        WaudioModel w2 = new WaudioModel("Ella People", R.drawable.banner2);
        WaudioModel w3 = new WaudioModel("Electric Guitar Rock", R.drawable.banner3);

        WaudioModel w4 = new WaudioModel("Dani Aventure", R.drawable.banner4);
        WaudioModel w5 = new WaudioModel("Indira Anime", R.drawable.banner5);
        WaudioModel w6 = new WaudioModel("Inglaterra mundo", R.drawable.banner6);

        WaudioModel w7 = new WaudioModel("Johan Urbano", R.drawable.banner7);
        WaudioModel w8 = new WaudioModel("Kary Amistad", R.drawable.banner8);
        WaudioModel w9 = new WaudioModel("Kelly Romance", R.drawable.banner9);

        WaudioModel w10 = new WaudioModel("Kenya Libertad", R.drawable.banner10);
        WaudioModel w11 = new WaudioModel("Kley General", R.drawable.banner11);
        WaudioModel w12 = new WaudioModel("Motorcycle Aventure", R.drawable.banner12);

        WaudioModel w13 = new WaudioModel("Paz Romance", R.drawable.banner13);
        WaudioModel w14 = new WaudioModel("Saxo General", R.drawable.banner14);
        WaudioModel w15 = new WaudioModel("Tu Y Yo Amor", R.drawable.banner15);

        WaudioModel w16 = new WaudioModel("Vallenato Colombia", R.drawable.banner16);


        list.add(w1);
        list.add(w2);
        list.add(w3);
        list.add(w4);
        list.add(w5);
        list.add(w6);
        list.add(w7);
        list.add(w8);
        list.add(w9);
        list.add(w10);
        list.add(w11);
        list.add(w12);
        list.add(w13);
        list.add(w14);
        list.add(w15);
        list.add(w16);

        return list;
    }

    public static WaudioModel getRandomBanner(List<WaudioModel> array) {
        int rnd = random.nextInt(array.size());
        return array.get(rnd);
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
                onGoStore(null);
                break;
            //case R.id.action_update_store:

        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(isVisible());
        if (refresh) {
            prepareTemplates();
            getNewItemsStore(3);
        }
    }

    public void onGoStore(View view) {
        mDataFirebaseHelper.incrementGotoStore();
        Intent intent = new Intent(getActivity(), StoreActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (observer != null)
            observer.stopWatching();
    }
}
