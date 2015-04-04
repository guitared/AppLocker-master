package com.nomorerowk.locker.util;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.nomorerowk.ads.InterstitialHelper;
import com.nomorerowk.locker.R;

import com.nomorerowk.ads.DefaultAdInterface;

public class LaunchInterstitialActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Transparent);
        setContentView(new View(this));

        InterstitialHelper helper = new InterstitialHelper(this,
                new DefaultAdInterface());
        helper.load();
    }

    boolean mCreated;
    @Override
    protected void onResume() {
        super.onResume();
        if (!mCreated) mCreated = true;
        else finish();
    }
}