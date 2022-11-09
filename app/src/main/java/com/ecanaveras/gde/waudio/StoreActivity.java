package com.ecanaveras.gde.waudio;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.fragments.DownloadDialogFragment;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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
import java.util.Date;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class StoreActivity extends AppCompatActivity {

    private FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

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
    private StorageReference templates;
    private StorageReference thumbnail;
    private NotificationManager mNotifyManager;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor_pref;
    private Menu menuStore;
    private Button btnPoints;
    private int points;
    private MainApp app;

    private RewardedAd mRewardedAd;
    private boolean adsLoaded, downloading;
    private String channel;
    private NotificationCompat.Builder mBuilder;
    private ImageView imgBg;
    private CoordinatorLayout layoutContent;
    private RelativeLayout layoutOffline;
    private LinearLayout layoutwait;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        channel = "StoreActivity";

        app = (MainApp) getApplicationContext();

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setUserProperty("open_waudio_store", "true");

        //imgTemplate = (ImageView) findViewById(R.id.imgTemplate);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        View bottomSheet = findViewById(R.id.bottom_sheet);

        btnPoints = (Button) findViewById(R.id.btnPoints);

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mStorage = FirebaseStorage.getInstance().getReference();
        mDataFirebaseHelper = new DataFirebaseHelper();
        mRef = mDataFirebaseHelper.getDatabaseReference(DataFirebaseHelper.REF_WAUDIO_TEMPLATES);

        updateViewPoints();

        imgBg = (ImageView) findViewById(R.id.imgBg);
        layoutwait = (LinearLayout) findViewById(R.id.layoutWait);
        layoutContent = (CoordinatorLayout) findViewById(R.id.layoutRecycler);
        layoutOffline = (RelativeLayout) findViewById(R.id.layourOffline);

        //Controlar vista
        imgBg.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        layoutOffline.setVisibility(View.GONE);

        loadDataTemplates();
    }


    private void loadDataTemplates() {
        sdWaudioModelList.clear();
        storeWaudioModelList.clear();

        //TODO realizar tarea en un hilo
        loadTemplates = new LoadTemplates(".mp4", getExternalFilesDir(null).getAbsolutePath());
        sdWaudioModelList = loadTemplates.getSdWaudioModelList();

        findFirebaseTemplate();
    }

    private void updateDataTemplates(WaudioModel download) {
        if (download != null) {
            //Remove Style Downloaded
            for (WaudioModel store : storeWaudioModelList) {
                if (download.getName().equals(store.getName())) {
                    storeWaudioModelList.remove(store);
                    break;
                }
            }
        }
        if (templateRecyclerAdapter != null)
            templateRecyclerAdapter.notifyDataSetChanged();
    }


    private void findFirebaseTemplate() {
        mRef.orderByChild("dateModified").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                storeWaudioModelList.clear();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    WaudioModel wt = data.getValue(WaudioModel.class);
                    storeWaudioModelList.add(wt);
                }
                for (WaudioModel sd : sdWaudioModelList) {
                    for (WaudioModel store : storeWaudioModelList) {
                        if (sd.getName().equals(store.getName())) {
                            storeWaudioModelList.remove(store);
                            break;
                        }
                    }
                }
                templateRecyclerAdapter = new TemplateRecyclerAdapter(StoreActivity.this, R.layout.media_style_card, storeWaudioModelList, true);
                recyclerView.setAdapter(templateRecyclerAdapter);
                setupViews();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }

        });
        //Informa en 3 Segundos
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setupViews();
            }
        }, 5000);
    }

    private void setupViews(){
        //Controlar vistas
        layoutwait.setVisibility(View.GONE);
        if(templateRecyclerAdapter!= null && templateRecyclerAdapter.getItemCount()>0){
            layoutOffline.setVisibility(View.GONE);
            layoutContent.setVisibility(View.VISIBLE);
            imgBg.setVisibility(View.VISIBLE);
        }else{
            layoutOffline.setVisibility(View.VISIBLE);
            layoutContent.setVisibility(View.GONE);
            imgBg.setVisibility(View.GONE);
        }

    }

    private void uploadInfoTemplate(String name, final String urlThumbnail) {
        waudioModel = null;
        templates = null;
        thumbnail = null;

        mStorage = FirebaseStorage.getInstance().getReference();
        templates = mStorage.child("templates").child(name.trim() + ".mp4");
        //thumbnail = mStorage.child("thumbnails").child(name.trim() + ".png");
        //Buscar link thumbnail
        templates.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                waudioModel = new WaudioModel();
                waudioModel.setUrlThumbnail(urlThumbnail);
                waudioModel.setPathMp4(uri.toString());
                //System.out.println("Ruta de descarga: " + uri.toString());
                //BUscar template y guardar
                templates.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        if (waudioModel != null) {
                            waudioModel.setName(storageMetadata.getName());
                            waudioModel.setSize(storageMetadata.getSizeBytes());
                            waudioModel.setDateModified(new Date().getTime()); //Fecha de Subida
                            //waudioModel.setPathMp4(storageMetadata.getDownloadUrl().toString());
                        }
                        saveTemplateFirebase();
                    }
                });
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


    public void onClicDownloadItem(WaudioModel item) {
        downloadItemWaudio = item;
        bottomSheetDialogFragment = DownloadDialogFragment.newInstance(item);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void onDownload(View v) {
        if (downloading) {
            Toasty.warning(this, getString(R.string.msgDownloadInProgress), Toast.LENGTH_SHORT).show();
            return;
        }
        if (downloadItemWaudio.getValue() > app.getPoints()) {
            showInfoPoints(null);
            return;
        }
        downloading = true;
        final int notiDownloadID = 1000;
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(StoreActivity.this, channel);
        mBuilder.setContentTitle("Waudio Store")
                .setContentText(getString(R.string.msgDownloadInProgress))
                .setSmallIcon(R.drawable.ic_noti);
        StorageReference template = mStorage.child("templates").child(downloadItemWaudio.getName());
        Toasty.warning(this, getString(R.string.msgDownloadInProgress), Toast.LENGTH_SHORT).show();
        try {
            final File localFile = File.createTempFile(downloadItemWaudio.getName(), "", new File(getExternalFilesDir(null).getAbsolutePath()));
            template.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    localFile.renameTo(new File(getExternalFilesDir(null).getAbsolutePath() + "/" + downloadItemWaudio.getName()));
                    bottomSheetDialogFragment.dismiss();
                    //loadDataTemplates();
                    //Remove Template from Store
                    downloading = false;
                    updateDataTemplates(downloadItemWaudio);
                    Toasty.success(StoreActivity.this, String.format(getString(R.string.msgDownloadSuccess), downloadItemWaudio.getSimpleName()), Toast.LENGTH_SHORT).show();
                    //Disminuye los puntos (Cobra el estilo)
                    updatePoints(downloadItemWaudio.getValue(), false);
                    mDataFirebaseHelper.incrementItemDownload();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                    downloading = false;
                    crashlytics.recordException(e);
                    Toasty.error(StoreActivity.this, String.format(getString(R.string.msgDownloadTry), downloadItemWaudio.getSimpleName()), Toast.LENGTH_SHORT).show();
                    if (mNotifyManager != null)
                        mNotifyManager.cancel(1000);
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    int downloaded = 0;
                    if (taskSnapshot.getBytesTransferred() > 0)
                        downloaded = (int) ((taskSnapshot.getTotalByteCount() / taskSnapshot.getBytesTransferred()) * 100);
                    mBuilder.setProgress(100, downloaded, false);
                    // Displays the progress bar for the first time.
                    mNotifyManager.notify(notiDownloadID, mBuilder.build());
                    //Toast.makeText(StoreActivity.this, "Descargando " + downloadItemWaudio.getSimpleName() + taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount(), Toast.LENGTH_SHORT).show();
                    if (downloaded == 100) {
                        mNotifyManager.cancel(notiDownloadID);
                    }
                }
            });
        } catch (IOException e) {
            downloading = false;
            if (mNotifyManager != null) {
                mNotifyManager.cancel(notiDownloadID);
            }
            crashlytics.recordException(e);
            Toasty.error(this, getString(R.string.msgProblemDownload), Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePoints(int valor, boolean increment) {
        app.updatePoints(valor, increment);
        updateViewPoints();
    }

    public void showInfoPoints(View v) {
        mDataFirebaseHelper.incrementWaudioViewPoinst();
        Intent intent = new Intent(this, WaudioPointsActivity.class);
        startActivity(intent);
    }

    private void updateViewPoints() {
        points = app.getPoints();
        if (menuStore != null) {
            MenuItem ac_points = menuStore.findItem(R.id.action_points);
            if (ac_points != null) {
                ac_points.setTitle(String.valueOf(points));
            }
        }
        btnPoints.setText(String.format(getString(R.string.lblBtnPoints), points));
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        btnPoints.setAnimation(bounce);
        bounce.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViewPoints();
    }


    @Override
    public void onBackPressed() {
        /*if (!adsView && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            adsView = true;
        } else {
            super.onBackPressed();
        }*/
        super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_store, menu);

        this.menuStore = menu;

        if (!BuildConfig.DEBUG) {
            MenuItem updateStore = menu.findItem(R.id.action_update_store);
            if (updateStore != null) {
                updateStore.setVisible(false);
            }
        }

        updateViewPoints();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update_store:
                final EditText nameTemplate = new EditText(this);
                final EditText urlTemplate = new EditText(this);
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                nameTemplate.setHint("Headset general");
                urlTemplate.setHint("Url Dropbox");

                linearLayout.addView(nameTemplate);
                linearLayout.addView(urlTemplate);
                new AlertDialog.Builder(this)
                        .setTitle("Update Store")
                        .setMessage("")
                        .setView(linearLayout)
                        .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String name = nameTemplate.getText().toString();
                                String url = urlTemplate.getText().toString();
                                uploadInfoTemplate(name, (url != null ? url : "https://dl.dropboxusercontent.com/s/"));
                            }
                        })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();

                break;
            case R.id.action_points:
                showInfoPoints(null);
                break;
            case R.id.action_qualify:
                Uri uri = Uri.parse("market://details?id=" + this.getApplicationContext().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.urlPlayStore))));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}


