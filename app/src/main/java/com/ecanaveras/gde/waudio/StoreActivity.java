package com.ecanaveras.gde.waudio;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.fragments.DownloadDialogFragment;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreActivity extends AppCompatActivity {


    private List<WaudioModel> storeWaudioModelList = new ArrayList<WaudioModel>();
    private List<WaudioModel> sdWaudioModelList = new ArrayList<>();
    private StorageReference mStorage;
    private ImageView imgTemplate;
    private TemplateRecyclerAdapter templateRecyclerAdapter;

    private DataFirebaseHelper mDataFirebaseHelper;
    private DatabaseReference mRef;

    private WaudioModel waudioModel;
    private RecyclerView recyclerView;
    private BottomSheetBehavior mBottomSheetBehavior;
    private WaudioModel downloadItemWaudio;
    private DownloadDialogFragment bottomSheetDialogFragment;
    private LoadTemplates loadTemplates;
    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setUserProperty("open_waudio_store", "true");

        //imgTemplate = (ImageView) findViewById(R.id.imgTemplate);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        View bottomSheet = findViewById(R.id.bottom_sheet);

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mStorage = FirebaseStorage.getInstance().getReference();
        mDataFirebaseHelper = new DataFirebaseHelper();
        mRef = mDataFirebaseHelper.getDatabaseReference(DataFirebaseHelper.REF_WAUDIO_TEMPLATES);


        loadDataTemplates();

        //uploadInfoTemplate("Inglaterra_mundo.m4v");
        //uploadInfoTemplate("Juntos_Amor.m4v");
        //uploadInfoTemplate("Mix general.mp4");
        //uploadInfoTemplate("Paz_Romance.m4v");
        //uploadInfoTemplate("Vinilo_waudio.mp4");
    }

    private void loadDataTemplates() {
        sdWaudioModelList.clear();
        storeWaudioModelList.clear();
        //TODO realizar tarea en un hilo
        loadTemplates = new LoadTemplates(".mp4", getExternalFilesDir(null).getAbsolutePath());
        sdWaudioModelList = loadTemplates.getSdWaudioModelList();

        findFirebaseTemplate();
    }


    private void findFirebaseTemplate() {
        mRef.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                storeWaudioModelList.clear();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    WaudioModel wt = data.getValue(WaudioModel.class);
                    storeWaudioModelList.add(wt);
                }
                for (WaudioModel sd : sdWaudioModelList) {
                    for (WaudioModel store : storeWaudioModelList) {
                        if (sd.getSimpleName().equals(store.getSimpleName())) {
                            storeWaudioModelList.remove(store);
                            break;
                        }
                    }
                }
                templateRecyclerAdapter = new TemplateRecyclerAdapter(StoreActivity.this, R.layout.media_style_card, storeWaudioModelList, true);
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
                    waudioModel = new WaudioModel();
                    waudioModel.setUrlThumbnail(uri.toString());
                }
                //Picasso.with(StoreActivity.this).load(uri).into(imgTemplate);
                saveTemplateFirebase();
            }
        });
        templates.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (waudioModel != null) {
                    waudioModel.setName(templates.getName());
                    waudioModel.setPathMp4(uri.toString());
                } else {
                    waudioModel = new WaudioModel(templates.getName());
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

    public void onClicDownloadItem(WaudioModel item) {
        downloadItemWaudio = item;
        bottomSheetDialogFragment = new DownloadDialogFragment(item);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void onDownload(View v) {
        StorageReference template = mStorage.child("templates").child(downloadItemWaudio.getName());
        try {
            //TODO Controlar el nombre del archivo en el Storage
            final File localFile = File.createTempFile(downloadItemWaudio.getSimpleName(), ".mp4", new File(getExternalFilesDir(null).getAbsolutePath()));
            template.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    localFile.renameTo(new File(getExternalFilesDir(null).getAbsolutePath() + "/" + downloadItemWaudio.getSimpleName() + ".mp4"));
                    bottomSheetDialogFragment.dismiss();
                    MainApp app = (MainApp) getApplicationContext();
                    app.reloadWaudios = true;
                    loadDataTemplates();
                    Toast.makeText(StoreActivity.this, downloadItemWaudio.getSimpleName() + " descargado!", Toast.LENGTH_SHORT).show();
                    mDataFirebaseHelper.incrementItemDownload();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                    e.printStackTrace();
                    Toast.makeText(StoreActivity.this, downloadItemWaudio.getSimpleName() + " no se ha podido descargar, intenta mas tarde!", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(StoreActivity.this, "Descargando " + downloadItemWaudio.getSimpleName() + taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPreview(View v) {

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
