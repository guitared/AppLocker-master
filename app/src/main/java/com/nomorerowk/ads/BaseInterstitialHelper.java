package com.nomorerowk.ads;

import android.content.Context;


public abstract class BaseInterstitialHelper {

    public BaseInterstitialHelper() {}

    public BaseInterstitialHelper(Context context, BaseAdDetails iface) {}

    public abstract void load();

    public abstract void show();

}
