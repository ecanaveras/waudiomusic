package com.ecanaveras.gde.waudio;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.listener.TemplatesFileObserver;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//import static com.google.android.gms.internal.zzagr.runOnUiThread;

public class ListTemplateActivity extends AppCompatActivity {

    private static Random random = new Random();
    private FirebaseAnalytics mFirebaseAnalytics;

    private RecyclerView recyclerView;
    private TemplateRecyclerAdapter templateRecyclerAdapter;
    private GeneratorWaudio generatorWaudio;
    private LinearLayout layoutWait, layoutRecycler;
    private DataFirebaseHelper mDataFirebaseHelper;
    private LinearLayout lyContentItemStore;
    private List<WaudioModel> storeWaudioModelList = new ArrayList<>();
    private List<WaudioModel> sdWaudioModelList = new ArrayList<>();
    private MainApp app;
    private TemplatesFileObserver observer;
    private DatabaseReference mRef;
    public boolean refresh = true;
    private LoadTemplates templates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_template);

        //Database
        mDataFirebaseHelper = new DataFirebaseHelper();
        mRef = mDataFirebaseHelper.getDatabaseReference(DataFirebaseHelper.REF_WAUDIO_TEMPLATES);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        app = (MainApp) getApplicationContext();
        if (app.getGeneratorWaudio() != null) {
            generatorWaudio = app.getGeneratorWaudio();
        } else {
            System.out.println("OFFLINE");
            //Reiniciar el ciclo
            Intent intent = new Intent(this, ListAudioActivity.class);
            this.startActivity(intent);
            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            finishAffinity();
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        //layoutWait = (LinearLayout) findViewById(R.id.layoutWait);
        layoutRecycler = (LinearLayout) findViewById(R.id.layoutRecycler);


        prepareTemplates();
        getNewItemsStore();

        lyContentItemStore = (LinearLayout) findViewById(R.id.lyContentItemStore);
        LinearLayout lyContentStore = (LinearLayout) findViewById(R.id.lyContentStore);
        lyContentStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGoStore(v);
            }
        });
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //recyclerView.setAdapter(templateRecyclerAdapter);

        //Si cambian los templates, actualiza el listado
        observer = new TemplatesFileObserver(this.getExternalFilesDir(null).getAbsolutePath());
        observer.setActivity(this);
        observer.startWatching();

        mFirebaseAnalytics.setUserProperty("open_list_template", String.valueOf(true));
    }

    /**
     * Templates para crear Waudios
     */
    public void prepareTemplates() {
        if (!refresh) {
            return;
        }
        sdWaudioModelList.clear();
        templates = new LoadTemplates(".mp4", getExternalFilesDir(null).getAbsolutePath());
        sdWaudioModelList = templates.getSdWaudioModelList();
        templateRecyclerAdapter = new TemplateRecyclerAdapter(this, sdWaudioModelList, generatorWaudio);
        recyclerView.setAdapter(templateRecyclerAdapter);
        templateRecyclerAdapter.notifyDataSetChanged();
        refresh = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refresh) {
            prepareTemplates();
            getNewItemsStore();
        }
        /*if (templateRecyclerAdapter != null)
            templateRecyclerAdapter.notifyDataSetChanged();*/
    }

    public void getNewItemsStore() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("getNewItemsStore", e.getMessage());
                }
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
                runOnUiThread(new Runnable() {
                    public void run() {
                        setupViewItemsStore(storeWaudioModelList);
                    }
                });

            }
        }.start();
    }

    private void setupViewItemsStore(List<WaudioModel> list) {
        if (list.isEmpty()) {
            ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.GONE);
            return;
        }
        lyContentItemStore.removeAllViewsInLayout();
        LayoutInflater inflater = LayoutInflater.from(this);
        Animation bounce = AnimationUtils.loadAnimation(this.getApplicationContext(), R.anim.bounce);
        for (WaudioModel waudioModel : list) {
            CardView view = (CardView) inflater.inflate(R.layout.store_item_new, null);

            ImageView img = (ImageView) view.findViewById(R.id.thumbnail);
            TextView title = (TextView) view.findViewById(R.id.title);
            //TextView category = (TextView) view.findViewById(R.id.category);

            Picasso.get().load(waudioModel.getResourceId()).resize(160, 140).into(img);
            title.setText(waudioModel.getSimpleName());
            //category.setText(waudioModel.getCategory());
            lyContentItemStore.addView(view);
            view.startAnimation(bounce);
        }
        ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.VISIBLE);
    }

    public static WaudioModel getRandomBanner(List<WaudioModel> array) {
        int rnd = random.nextInt(array.size());
        return array.get(rnd);
    }

    public void onGoStore(View view) {
        mDataFirebaseHelper.incrementGotoStore();
        Intent intent = new Intent(this, StoreActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (observer != null)
            observer.stopWatching();
    }

}
