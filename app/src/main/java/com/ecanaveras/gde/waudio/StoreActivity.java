package com.ecanaveras.gde.waudio;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class StoreActivity extends AppCompatActivity {

    public static final String REF_WAUDIO_TEMPLATES = "waudio-templates";

    private List<WaudioModel> waudioModelList = new ArrayList<WaudioModel>();
    private StorageReference mStorage;
    private ImageView imgTemplate;
    private TemplateRecyclerAdapter templateRecyclerAdapter;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mRef;

    private WaudioModel waudioModel;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        imgTemplate = (ImageView) findViewById(R.id.imgTemplate);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();//StoreActivity.TEMPLATES_DATABASE);
        mRef = mDatabaseReference.child(REF_WAUDIO_TEMPLATES);

        findInfoTemplate();

        //uploadInfoTemplate("Inglaterra_mundo.m4v");
        //uploadInfoTemplate("Juntos_Amor.m4v");
        //uploadInfoTemplate("Mix_general.mp4");
        //uploadInfoTemplate("Paz_Romance.m4v");
        //uploadInfoTemplate("Vinilo_waudio.mp4");
    }

    private void findInfoTemplate() {
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    //System.out.println("OBJECT " + data.getValue());
                    WaudioModel wt = data.getValue(WaudioModel.class);
                    waudioModelList.add(wt);
                }
                templateRecyclerAdapter = new TemplateRecyclerAdapter(StoreActivity.this, R.layout.media_style_card, waudioModelList, true);
                recyclerView.setAdapter(templateRecyclerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void uploadInfoTemplate(String name) {
        //waudioModel = null;
        final StorageReference templates = mStorage.child("templates").child(name);
        final StorageReference thumbnail = mStorage.child("thumbnails").child(name.split("\\.")[0] + ".png");
        //dowloadUri.getActiveDownloadTasks()
        thumbnail.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //picassoInstance.load(uri).into(imgTemplate);
                if (waudioModel != null) {
                    waudioModel.setUrlThumbnail(uri.toString());
                } else {
                    waudioModel = new WaudioModel(thumbnail.getName());
                    waudioModel.setCategory("DEPORTE");
                    waudioModel.setUrlThumbnail(uri.toString());
                }
                Picasso.with(StoreActivity.this).load(uri).into(imgTemplate);
                saveTemplateFirebase();
            }
        });
        templates.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (waudioModel != null) {
                    waudioModel.setPathMp4(uri.toString());
                } else {
                    waudioModel = new WaudioModel(templates.getName());
                    waudioModel.setCategory("DEPORTE");
                    waudioModel.setPathMp4(uri.toString());
                }
                saveTemplateFirebase();
            }
        });
        templates.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                if (waudioModel != null) {
                    waudioModel.setSize(storageMetadata.getSizeBytes());
                }
                saveTemplateFirebase();
            }
        });
    }

    private void saveTemplateFirebase() {
        //Subir la imagen, y el video
        //Obtener URL de imagen y video
        //Almacenar datos en la BD
        if (waudioModel == null || waudioModel.getPathMp4() == null || waudioModel.getUrlThumbnail() == null || waudioModel.getSize() == 0) {
            return;
        }
        final String wtId = mRef.push().getKey();//WaudioTemplateId
        mRef.child(wtId).setValue(waudioModel);
    }
}
