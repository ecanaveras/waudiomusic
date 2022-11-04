package com.ecanaveras.gde.waudio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class WaudioPointsActivity extends AppCompatActivity {

    RewardedAd rewardedAd;
    Button videoButton;
    ImageView iconPoints;
    TextView adTextView;

    private int points;
    private MainApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waudio_points);

        app = (MainApp) getApplicationContext();

        videoButton = findViewById(R.id.ad_video_button);
        adTextView = findViewById(R.id.ad_text);
        iconPoints = findViewById(R.id.imgIconPoints);

        rewardedAd = new RewardedAd(this, MainApp.ADMOB_VIDEO_REWARDS);

        rewardedAd.loadAd(new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                super.onRewardedAdLoaded();
                videoButton.setVisibility(View.VISIBLE);
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayAd();
            }
        });

        updatePoints(0, false);
    }

    public void displayAd() {
        rewardedAd.show(this, new RewardedAdCallback() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                videoButton.setVisibility(View.INVISIBLE);
                updatePoints(rewardItem.getAmount(), true);
            }
        });
    }

    private void updatePoints(int valor, boolean increment) {
        points = app.updatePoints(valor, increment);
        updateViewPoints();
    }

    private void updateViewPoints() {
        adTextView.setText(String.format(getString(R.string.lblBtnPoints), points));
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        adTextView.setAnimation(bounce);
        iconPoints.setAnimation(bounce);
        bounce.start();
    }
}
