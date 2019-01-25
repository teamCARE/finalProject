package com.example.android.caredemo;

import android.app.Activity;
import android.os.Bundle;

import com.everysight.base.EvsBaseActivity;
import com.everysight.notifications.EvsToast;

public class MainActivity extends EvsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onTap() {
        super.onTap();
        EvsToast.show(this,"Nice Tap!\nSwipe down to Exit");
    }
}
