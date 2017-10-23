package com.ecanaveras.gde.waudio;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ListTemplateActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_list_template_new);

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
        getNewItemsStore(20);

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
            getNewItemsStore(20);
        }
        /*if (templateRecyclerAdapter != null)
            templateRecyclerAdapter.notifyDataSetChanged();*/
    }

    public void getNewItemsStore(int limit) {
        mRef.orderByKey().limitToLast(limit).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                storeWaudioModelList.clear();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    WaudioModel wt = data.getValue(WaudioModel.class);
                    storeWaudioModelList.add(wt);
                }
                List<WaudioModel> itemsShow = new ArrayList<WaudioModel>();
                for (WaudioModel store : storeWaudioModelList) {
                    boolean downloaded = false;
                    for (WaudioModel sd : sdWaudioModelList) {
                        //SI NO EXISTE EN LOS TEMPLATES SD
                        if (store.getName().equals(sd.getName())) {
                            downloaded = true;
                            break;
                        }
                    }
                    if (!downloaded)
                        itemsShow.add(store);
                    if (itemsShow.size() == 3) {
                        break;
                    }
                }
                setupViewItemsStore(itemsShow);
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
        lyContentItemStore.removeAllViewsInLayout();
        LayoutInflater inflater = LayoutInflater.from(this);
        Animation bounce = AnimationUtils.loadAnimation(this.getApplicationContext(), R.anim.bounce);
        for (WaudioModel waudioModel : list) {
            CardView view = (CardView) inflater.inflate(R.layout.store_item_new, null);

            ImageView img = (ImageView) view.findViewById(R.id.thumbnail);
            TextView title = (TextView) view.findViewById(R.id.title);
            //TextView category = (TextView) view.findViewById(R.id.category);

            Picasso.with(this).load(waudioModel.getUrlThumbnail()).resize(160, 140).into(img);
            title.setText(waudioModel.getSimpleName());
            //category.setText(waudioModel.getCategory());
            lyContentItemStore.addView(view);
            view.startAnimation(bounce);
        }
        ((LinearLayout) lyContentItemStore.getParent()).setVisibility(View.VISIBLE);
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
