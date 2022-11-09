package com.ecanaveras.gde.waudio;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import es.dmoral.toasty.Toasty;

public class WaudioPointsActivity extends AppCompatActivity {

    RewardedAd rewardedAd;
    Button videoButton;
    ImageView iconPoints;
    TextView adTextView;
    Animation bounce;
    Animation bounceButton;

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


        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayAd();
            }
        });

        bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        bounceButton = AnimationUtils.loadAnimation(this, R.anim.bounce);

        reloadRewardedAd();
        updateViewPoints();
    }

    private void reloadRewardedAd() {
        rewardedAd.load(this, MainApp.ADMOB_VIDEO_REWARDS, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                super.onAdLoaded(ad);
                rewardedAd = ad;
                videoButton.setEnabled(true);
                videoButton.setAnimation(bounceButton);
                bounceButton.start();
            }
        });
    }

    private void displayAd() {
        if (rewardedAd != null) {
            rewardedAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    videoButton.setEnabled(false);
                    if (rewardItem != null) {
                        Toasty.custom(getApplicationContext(), String.format(getString(R.string.msgWindPoints), rewardItem.getAmount()), getDrawable(R.drawable.ic_points), getColor(R.color.colorAccent), Toast.LENGTH_SHORT, true, true).show();
                        updatePointsAdmob(rewardItem.getAmount(), true, true);
                        reloadRewardedAd();
                    }
                }
            });
        }

    }


    private void updatePointsAdmob(int valor, boolean increment, boolean isAdmob) {
        app.updatePoints(valor, increment, isAdmob);
        updateViewPoints();
    }


    private void updateViewPoints() {
        points = app.getPoints();
        adTextView.setText(String.format(getString(R.string.lblBtnPoints), points));
        adTextView.setAnimation(bounce);
        iconPoints.setAnimation(bounce);
        bounce.start();
    }
}
