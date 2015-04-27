package com.nomorerowk.locker.ui;

import com.nomorerowk.locker.lock.AppLockService;
import com.nomorerowk.locker.lock.LockService;
import com.nomorerowk.locker.util.PrefUtils;
import com.nomorerowk.util.DialogSequencer;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;

import com.nomorerowk.locker.R;

public class MainActivity extends ActionBarActivity implements
        NavigationFragment.NavigationListener {

//	private static final String VERSION_URL_PRD = "https://nomorerowk.org/apps/locker/update.php";
//	private static final String VERSION_URL_DBG = "https://nomorerowk.org/apps/locker/dbg-update.php";
//	public static final String VERSION_URL = Constants.DEBUG ? VERSION_URL_DBG
//			: VERSION_URL_PRD;
	private static final String EXTRA_UNLOCKED = "com.nomorerowk.locker.unlocked";

	private DialogSequencer mSequencer;
	private Fragment mCurrentFragment;
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationFragment mNavFragment;

	/**
	 * Used to store the last screen title. For use in
	 * .
	 */
	private CharSequence mTitle;

	private ActionBar mActionBar;
	private BroadcastReceiver mReceiver;
	private IntentFilter mFilter;


	private class ServiceStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("MainACtivity",
					"Received broadcast (action=" + intent.getAction());
			updateLayout();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		handleIntent();

		mReceiver = new ServiceStateReceiver();
		mFilter = new IntentFilter();
		mFilter.addCategory(AppLockService.CATEGORY_STATE_EVENTS);
		mFilter.addAction(AppLockService.BROADCAST_SERVICE_STARTED);
		mFilter.addAction(AppLockService.BROADCAST_SERVICE_STOPPED);

		mNavFragment = (NavigationFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		// Set up the drawer.
		mNavFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		mTitle = getTitle();

		mActionBar = getSupportActionBar();
		mCurrentFragment = new AppsFragment();
		getSupportFragmentManager().beginTransaction()
				.add(R.id.container, mCurrentFragment).commit();
		mCurrentFragmentType = NavigationElement.TYPE_APPS;

		mSequencer = new DialogSequencer();
		showDialogs();
		showLockerIfNotUnlocked(false);

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("Main", "onResume");
		showLockerIfNotUnlocked(true);
		registerReceiver(mReceiver, mFilter);
		updateLayout();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// mSequencer.stop();
		LockService.hide(this);
		unregisterReceiver(mReceiver);
		mSequencer.stop();

		// We have to finish here or the system will assign a lower priority to
		// the app (since 4.4?)
		if (mCurrentFragmentType != NavigationElement.TYPE_SETTINGS) {
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		Log.v("Main", "onDestroy");
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d("", "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent();
	}

	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		mTitle = title;
		getSupportActionBar().setTitle(title);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.global, menu);
		return true;
	}

	/**
	 * Provide a way back to {@link MainActivity} without having to provide a
	 * password again. It finishes the calling {@link Activity}
	 * 
	 * @param context
	 */
	public static void showWithoutPassword(Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.putExtra(EXTRA_UNLOCKED, true);
		if (!(context instanceof Activity)) {
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		context.startActivity(i);
	}

	public void setActionBarTitle(int resId) {
		mActionBar.setTitle(resId);
	}


	/**
	 * 
	 * @return True if the service is allowed to start
	 */
	private boolean showDialogs() {
		boolean deny = false;

		// Recovery code
		mSequencer.addDialog(Dialogs.getRecoveryCodeDialog(this));

		// Empty password
		deny = Dialogs.addEmptyPasswordDialog(this, mSequencer);

		mSequencer.start();
		return !deny;
	}

	private void showLockerIfNotUnlocked(boolean relock) {
		boolean unlocked = getIntent().getBooleanExtra(EXTRA_UNLOCKED, false);
		if (new PrefUtils(this).isCurrentPasswordEmpty()) {
			unlocked = true;
		}
		if (!unlocked) {
			LockService.showCompare(this, getPackageName());
		}
		getIntent().putExtra(EXTRA_UNLOCKED, !relock);
	}

	private void updateLayout() {
		Log.d("Main",
				"UPDATE LAYOUT Setting service state: "
						+ AppLockService.isRunning(this));
		mNavFragment.getAdapter().setServiceState(
				AppLockService.isRunning(this));
	}

	/**
	 * Handle this Intent for searching...
	 */
	private void handleIntent() {
		if (getIntent() != null && getIntent().getAction() != null) {
			if (getIntent().getAction().equals(Intent.ACTION_SEARCH)) {
				Log.d("MainActivity", "Action search!");
				if (mCurrentFragmentType == NavigationElement.TYPE_APPS) {
					final String query = getIntent().getStringExtra(
							SearchManager.QUERY);
					if (query != null) {
						((AppsFragment) mCurrentFragment).onSearch(query);
					}
				}
			}
		}
	}

	private boolean mNavPending;
	private int mCurrentFragmentType;
	private int mNavPendingType = -1;

	@Override
	public boolean onNavigationElementSelected(int type) {
		if (type == NavigationElement.TYPE_TEST) {
			// Test something here
			return false;
		} else if (type == NavigationElement.TYPE_STATUS) {
			toggleService();
			return false;
		}
		mNavPending = true;
		mNavPendingType = type;
		return true;
	}

	private void toggleService() {
		boolean newState = false;
		if (AppLockService.isRunning(this)) {
			Log.d("", "toggleService() Service is running, now stopping");
			AppLockService.stop(this);
		} else if (Dialogs.addEmptyPasswordDialog(this, mSequencer)) {
			mSequencer.start();
		} else {
			newState = AppLockService.toggle(this);
		}
		if (mNavFragment != null)
			mNavFragment.getAdapter().setServiceState(newState);
	}

	@Override
	public void onDrawerOpened() {
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	public void onDrawerClosed() {
		getSupportActionBar().setTitle(mTitle);
		if (mNavPending) {
			navigateToFragment(mNavPendingType);
			mNavPending = false;
		}
	}

	/**
	 * Open a specific Fragment
	 * 
	 * @param type
	 */
    void navigateToFragment(int type) {
		if (type == mCurrentFragmentType) {
			// Don't duplicate
			return;
		}
		if (type == NavigationElement.TYPE_CHANGE) {
			Dialogs.getChangePasswordDialog(this).show();
			// Don't change current fragment type
			return;
		}
		if (type == NavigationElement.TYPE_TIME) {
			//guitared
			Dialogs.getTimerDialog(this).show();
			// Don't change current fragment type
			return;
		}

		switch (type) {
		case NavigationElement.TYPE_APPS:
			mCurrentFragment = new AppsFragment();
			break;
		case NavigationElement.TYPE_SETTINGS:
			mCurrentFragment = new SettingsFragment();
			break;
		case NavigationElement.TYPE_STATISTICS:
			mCurrentFragment = new StatisticsFragment();
			break;
		}
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.container, mCurrentFragment)
				.commit();
		mCurrentFragmentType = type;
	}

	@Override
	public void onShareButton() {
		// Don't add never button, the user wanted to share
		Dialogs.getShareEditDialog(this, false).show();
	}

	@Override
	public void onRateButton() {
		toGooglePlay();
	}

	private void toGooglePlay() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=" + getPackageName()));
		if (getPackageManager().queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY).size() >= 1) {
			startActivity(intent);
		}
	}
}
