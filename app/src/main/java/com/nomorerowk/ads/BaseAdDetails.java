package com.nomorerowk.ads;

public abstract class BaseAdDetails {

	/**
	 * @return The Ad Unit Id of the banner if this app uses banners
	 */
	public abstract String getBannerAdUnitId();

	/**
	 * @return The Ad Unit Id of the interstitial if this app uses interstitial
	 */
	public abstract String getInterstitialAdUnitId();

	/**
	 * @return The device id's of devices that must show test ads
	 */
	public abstract String[] getTestDevices();

    /**
     *
     * @return True if ads should be displayed
     */
    public abstract boolean adsEnabled();

}
