package com.ecanaveras.gde.waudio;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.legacy.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;
import com.ecanaveras.gde.waudio.fragments.LibStylesFragment;
import com.ecanaveras.gde.waudio.fragments.LibWaudiosFragment;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    private static final int SHARE_WAUDIO_REQUEST = 1;

    private LibWaudiosFragment libWaudiosFragment;
    private LibStylesFragment libStylesFragment;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private long back_pressed;
    private TabLayout tabLayout;
    private Menu mainMenu;
    private AdaptadorSecciones adaptadorSecciones;
    private MainApp app;
    private FloatingActionButton newMusicW;
    //private FloatingActionButton newRecordW;
    private DataFirebaseHelper mDataFirebaseHelper;

    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
        setContentView(R.layout.activity_main);

        app = (MainApp) getApplicationContext();

        mDataFirebaseHelper = new DataFirebaseHelper();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setUserProperty("open_main_activity", String.valueOf(true));

        if (savedInstanceState == null) {
            libWaudiosFragment = new LibWaudiosFragment();
            libStylesFragment = new LibStylesFragment();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setTitleTextAppearance(getApplicationContext(), R.style.Theme_AppLib_ActionBar_TitleTextStyle);
        setSupportActionBar(toolbar);

        // Set up the ViewPager with the sections adaptadorSecciones.
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        changeTabsFont();

        newMusicW = (FloatingActionButton) findViewById(R.id.newMusicW);
        newMusicW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainIntent = new Intent(MainActivity.this, ListAudioActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(mainIntent);
            }
        });

        /*newRecordW = findViewById(R.id.newRecordW);
        newRecordW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainIntent = new Intent(MainActivity.this, RecordActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(mainIntent);
            }
        });*/

    }

    private void setupViewPager(ViewPager viewPager) {
        adaptadorSecciones = new AdaptadorSecciones(getFragmentManager());
        //adaptadorSecciones.addFragment(tracksFragment, getString(R.string.nameFragmentTracks));
        adaptadorSecciones.addFragment(libWaudiosFragment, getString(R.string.title_fragment_waudios));
        adaptadorSecciones.addFragment(libStylesFragment, getString(R.string.title_fragment_styles));
        viewPager.setAdapter(adaptadorSecciones);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position != 0) {
                    newMusicW.setVisibility(View.GONE);
                    //newRecordW.setVisibility(View.GONE);
                } else {
                    newMusicW.setVisibility(View.VISIBLE);
                    //newRecordW.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void changeTabsFont() {
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);
        int tabsCount = vg.getChildCount();
        for (int j = 0; j < tabsCount; j++) {
            ViewGroup vgTab = (ViewGroup) vg.getChildAt(j);
            int tabChildsCount = vgTab.getChildCount();
            for (int i = 0; i < tabChildsCount; i++) {
                View tabViewChild = vgTab.getChildAt(i);
                if (tabViewChild instanceof TextView) {
                    ((TextView) tabViewChild).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Dosis-SemiBold.ttf"));
                }
            }
        }
    }

    public void refreshNameTabs(int cantItems) {
        List<String> titles = new ArrayList<>();
        titles.add(getString(R.string.title_fragment_waudios));
        if (cantItems > 0)
            titles.add(getString(R.string.title_fragment_styles));
        //titles.add(getString(R.string.title_fragment_styles) + (cantItems != 0 ? " (" + cantItems + ")" : ""));
        adaptadorSecciones.setTitles(titles);
        adaptadorSecciones.notifyDataSetChanged();
        changeTabsFont();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qualify:
                goToStore();
                break;
            case R.id.action_share:
                String msg1 = getString(R.string.msgShareApp);
                String urlPS = getString(R.string.urlPlayStore);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, String.format("%s: %s", msg1, urlPS));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getText(R.string.msgShareTo)));
                break;
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToStore() {
        Uri uri = Uri.parse("market://details?id=" + this.getApplicationContext().getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.urlPlayStore))));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHARE_WAUDIO_REQUEST && resultCode == Activity.RESULT_OK) {
            app.updatePoints(25, true);
            Toasty.custom(getApplicationContext(), "+" + 25 + " " + getString(R.string.lblPoints), getDrawable(R.drawable.ic_points),getColor(R.color.colorAccent), Toast.LENGTH_SHORT, true, true).show();
            mFirebaseAnalytics.setUserProperty("shared", String.valueOf(true));
            mDataFirebaseHelper.incrementWaudioShared();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app.getMyRating()) {
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.custom_dialog_rating, null);
            AlertDialog.Builder info = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.alert_ok_rating), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            app.saveRating();
                            mDataFirebaseHelper.incrementWaudioRating();
                            goToStore();
                        }
                    }).setNegativeButton(getString(R.string.alert_cancel_rating), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            app.decrementCountWaudioCreated();
                        }
                    });
            info.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            finishAffinity();
        } else
            Toast.makeText(this, getString(R.string.msgExit), Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    /**
     * Gestiona los fragmentos y titulos de los tabs
     */
    public class AdaptadorSecciones extends FragmentStatePagerAdapter {

        private final List<Fragment> fragments = new ArrayList<>();
        private List<String> titles = new ArrayList<>();

        public AdaptadorSecciones(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }

        public void setTitles(List<String> titles) {
            this.titles = titles;
        }


    }
}
