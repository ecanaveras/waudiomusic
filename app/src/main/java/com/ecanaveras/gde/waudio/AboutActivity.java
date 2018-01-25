package com.ecanaveras.gde.waudio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView lblVersion = (TextView) findViewById(R.id.lblVersion);

        String versionName = BuildConfig.VERSION_NAME;
        lblVersion.setText("Version: " + versionName);
    }
}
