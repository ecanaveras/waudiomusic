package com.ecanaveras.gde.waudio;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.adapters.TemplateRecyclerAdapter;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.fragments.DownloadDialogFragment;
import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
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
import java.util.Date;
import java.util.List;

public class StoreActivity extends AppCompatActivity implements RewardedVideoAdListener {


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
    private NotificationCompat.Builder mBuilder;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor_pref;
    private Menu menuStore;
    private int points;
    private MainApp app;

    private RewardedVideoAd mRewardedVideoAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        app = (MainApp) getApplicationContext();

        //AdMods
        MobileAds.initialize(this, MainApp.ADMOB_APP_ID);

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);

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

        updatePoints(0, false);

        loadDataTemplates();

        loadRewardedVideoAd();
    }

    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd("ca-app-pub-3940256099942544/5224354917",
                new AdRequest.Builder().build());
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void uploadInfoTemplate(String name) {
        waudioModel = null;
        templates = null;
        thumbnail = null;

        mStorage = FirebaseStorage.getInstance().getReference();
        templates = mStorage.child("templates").child(name.trim() + ".mp4");
        thumbnail = mStorage.child("thumbnails").child(name.trim() + ".png");
        //Buscar link thumbnail
        thumbnail.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                waudioModel = new WaudioModel();
                waudioModel.setUrlThumbnail(uri.toString());
                //BUscar template y guardar
                templates.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        if (waudioModel != null) {
                            waudioModel.setName(storageMetadata.getName());
                            waudioModel.setSize(storageMetadata.getSizeBytes());
                            waudioModel.setDateModified(new Date().getTime()); //Fecha de Subida
                            waudioModel.setPathMp4(storageMetadata.getDownloadUrl().toString());
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
        bottomSheetDialogFragment = new DownloadDialogFragment(item);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void onDownload(View v) {
        if (downloadItemWaudio.getValue() > points) {
            showInfoPoints();
            return;
        }
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(StoreActivity.this);
        mBuilder.setContentTitle("Waudio Store")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_noti);
        StorageReference template = mStorage.child("templates").child(downloadItemWaudio.getName());
        try {
            final File localFile = File.createTempFile(downloadItemWaudio.getName(), "", new File(getExternalFilesDir(null).getAbsolutePath()));
            template.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    localFile.renameTo(new File(getExternalFilesDir(null).getAbsolutePath() + "/" + downloadItemWaudio.getName()));
                    bottomSheetDialogFragment.dismiss();
                    MainApp app = (MainApp) getApplicationContext();
                    loadDataTemplates();
                    Toast.makeText(StoreActivity.this, downloadItemWaudio.getSimpleName() + " descargado!", Toast.LENGTH_SHORT).show();
                    updatePoints(downloadItemWaudio.getValue(), false);
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
                    mNotifyManager.notify(1000, mBuilder.build());
                    //Toast.makeText(StoreActivity.this, "Descargando " + downloadItemWaudio.getSimpleName() + taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount(), Toast.LENGTH_SHORT).show();
                    if (downloaded == 100) {
                        mNotifyManager.cancel(1000);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePoints(int valor, boolean increment) {
        points = app.updatePoints(valor, increment);
        updateViewPoints();
    }

    private void updatePointsAdmob(int valor, boolean increment, boolean isAdmob) {
        points = app.updatePoints(valor, increment, isAdmob);
        updateViewPoints();
    }

    private void showInfoPoints() {
        mDataFirebaseHelper.incrementWaudioViewPoinst();
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_points, null);
        TextView textPoints = (TextView) dialogView.findViewById(R.id.lblPoints);
        textPoints.setText(points + getResources().getString(R.string.lblPoints));
        android.support.v7.app.AlertDialog.Builder info = new android.support.v7.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(getResources().getString(R.string.alert_ok_points), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mRewardedVideoAd.isLoaded()) {
                            mRewardedVideoAd.show();
                        }
                    }
                }).setNegativeButton(getResources().getString(R.string.alert_cancel_points), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        info.show();
    }

    public void onPreview(View v) {

    }

    private void updateViewPoints() {
        if (menuStore != null) {
            MenuItem ac_points = menuStore.findItem(R.id.action_points);
            if (ac_points != null) {
                ac_points.setTitle("" + points + " P");
            }
        }
    }

    @Override
    protected void onResume() {
        mRewardedVideoAd.resume(this);
        updateViewPoints();
        super.onResume();
    }

    @Override
    public void onPause() {
        mRewardedVideoAd.pause(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mRewardedVideoAd.destroy(this);
        super.onDestroy();
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
                nameTemplate.setHint("Headset general");
                new AlertDialog.Builder(this)
                        .setTitle("Update Store")
                        .setMessage("")
                        .setView(nameTemplate)
                        .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String url = nameTemplate.getText().toString();
                                uploadInfoTemplate(url);
                            }
                        })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();

                break;
            case R.id.action_points:
                showInfoPoints();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRewardedVideoAdLoaded() {

    }

    @Override
    public void onRewardedVideoAdOpened() {
        mDataFirebaseHelper.incrementWaudioViewVideos();
    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        updatePointsAdmob(100, true, true);
        Toast.makeText(StoreActivity.this, String.format(getResources().getString(R.string.msgWindPoints), 100), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }
}
