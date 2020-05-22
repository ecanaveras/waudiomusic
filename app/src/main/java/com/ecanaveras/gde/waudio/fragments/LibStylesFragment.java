package com.ecanaveras.gde.waudio.fragments;


import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

//import static com.google.android.gms.internal.zzagr.runOnUiThread;

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
        getNewItemsStore();

        //Si cambian los templates, actualiza el listado
        observer = new TemplatesFileObserver(getActivity().getExternalFilesDir(null).getAbsolutePath());
        observer.setStylesFragment(this);
        observer.startWatching();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        countItemsStore();
    }

    /**
     * Mostrar banner con Estilos
     */
    public void getNewItemsStore() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    //
                    storeWaudioModelList.clear();
                    int cantBanner = 0;
                    while (cantBanner < 3) {
                        boolean add = true;
                        WaudioModel wTmp = getRandomBanner(MainApp.getListBannerWS());
                        for (WaudioModel w : storeWaudioModelList) {
                            if (w.getName().equals(wTmp.getName())) {
                                add = false;
                                break;
                            }
                        }
                        if (add) {
                            storeWaudioModelList.add(wTmp);
                            cantBanner++;
                        }

                    }
                    /*runOnUiThread(new Runnable() {
                        public void run() {
                            setupViewItemsStore(storeWaudioModelList);
                        }
                    });*/
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setupViewItemsStore(storeWaudioModelList);
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

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

            final ImageView img = (ImageView) view.findViewById(R.id.thumbnail);
            TextView title = (TextView) view.findViewById(R.id.title);
            //TextView category = (TextView) view.findViewById(R.id.category);
            Picasso.get().load(waudioModel.getResourceId()).resize(160, 140).into(img);
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
        final Context mContext = getActivity();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_new_items, null);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    if (app.findNewItemStore(dataSnapshot.getChildrenCount()) > 0) {
                        AlertDialog.Builder info = new AlertDialog.Builder(mContext)
                                .setView(dialogView)
                                .setPositiveButton(getString(R.string.alert_ok_new_styles), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onGoStore(null);
                                    }
                                }).setNegativeButton("OK", null);
                        info.show();
                    }
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
                //Snackbar.make(getView(), getString(R.string.msgComingSoonStore), Snackbar.LENGTH_SHORT).show();
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
            getNewItemsStore();
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
