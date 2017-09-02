package com.ecanaveras.gde.waudio;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ecanaveras.gde.waudio.fragments.LibStylesFragment;
import com.ecanaveras.gde.waudio.fragments.LibWaudiosFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LibWaudiosFragment libWaudiosFragment;
    private LibStylesFragment libStylesFragment;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private long back_pressed;
    private TabLayout tabLayout;
    private Menu mainMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            libWaudiosFragment = new LibWaudiosFragment();
            libStylesFragment = new LibStylesFragment();
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setTitleTextAppearance(getApplicationContext(), R.style.Theme_AppLib_ActionBar_TitleTextStyle);
        setSupportActionBar(toolbar);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        changeTabsFont();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainIntent = new Intent(MainActivity.this, ListAudioActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(mainIntent);
            }
        });

    }

    private void setupViewPager(ViewPager viewPager) {
        final AdaptadorSecciones adapter = new AdaptadorSecciones(getFragmentManager());
        //adapter.addFragment(tracksFragment, getString(R.string.nameFragmentTracks));
        adapter.addFragment(libWaudiosFragment, getString(R.string.title_fragment_waudios));
        adapter.addFragment(libStylesFragment, getString(R.string.title_fragment_styles));
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (mainMenu != null) {
                    if (adapter.getItem(position) instanceof LibWaudiosFragment) {
                        mainMenu.getItem(0).setVisible(true);
                    } else {
                        mainMenu.getItem(0).setVisible(false);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mainMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, mainMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //case R.id.action_update:
            //    findMusic(null);
            //    break;
            case R.id.action_qualify:
                Uri uri = Uri.parse("market://details?id=" + this.getApplicationContext().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.urlPlayStore))));
                }
                break;
            case R.id.action_share:
                String msg1 = getResources().getString(R.string.msgShareApp);
                String urlPS = getResources().getString(R.string.urlPlayStore);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, String.format("%s: %s", msg1, urlPS));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.msgShareTo)));
                break;
        }
        return true;
    }


    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(this, getResources().getString(R.string.msgExit), Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    /**
     * Gestiona los fragmentos y titulos de los tabs
     */
    public class AdaptadorSecciones extends FragmentStatePagerAdapter {

        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> titles = new ArrayList<>();

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
    }
}
