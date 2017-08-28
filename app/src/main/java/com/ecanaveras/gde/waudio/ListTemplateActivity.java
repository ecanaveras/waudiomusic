package com.ecanaveras.gde.waudio;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import com.ecanaveras.gde.waudio.adapters.TemplatesAdapter;
import com.ecanaveras.gde.waudio.editor.GeneratorWaudio;
import com.google.firebase.analytics.FirebaseAnalytics;

public class ListTemplateActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;

    private RecyclerView recyclerView;
    private TemplatesAdapter templatesAdapter;
    private GeneratorWaudio generatorWaudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_template);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        MainApp app = (MainApp) getApplicationContext();
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

        prepareTemplates();

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpTopz(10), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(templatesAdapter);

        mFirebaseAnalytics.setUserProperty("open_list_template", String.valueOf(true));

        final float[] historicX = {Float.NaN};
        final float[] historicY = {Float.NaN};
        final int DELTA = 50;
        //enum Direction {LEFT, RIGHT;}
        /*recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        historicX[0] = event.getX();
                        historicY[0] = event.getY();
                        break;

                    case MotionEvent.ACTION_UP:
                        if (event.getX() - historicX[0] < -DELTA) {
                            //FunctionDeleteRowWhenSlidingLeft();
                            return true;
                        } else if (event.getX() - historicX[0] > DELTA) {
                            //FunctionDeleteRowWhenSlidingRight();

                            //System.out.println("POSITION RECICYER" + recyclerView.getChildAdapterPosition(view));
                            return true;
                        }
                        break;

                    default:
                        return false;
                }
                return false;
            }

        });*/

    }

    /**
     * Templates para crear Waudios
     */
    private void prepareTemplates() {
        LoadTemplates templates = new LoadTemplates(".mp4", getExternalFilesDir("").getAbsolutePath());
        templatesAdapter = new TemplatesAdapter(this, templates.getTemplateList(), generatorWaudio);

        templatesAdapter.notifyDataSetChanged();
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            //TODO Revisar Codigo que renderiza el RecyclerView
            int position = parent.getChildAdapterPosition(view); //item position
            int column = position % spanCount; //item column;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }

        }
    }

    private int dpTopz(int dp) {
        Resources resources = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        templatesAdapter.notifyDataSetChanged();
    }

}
