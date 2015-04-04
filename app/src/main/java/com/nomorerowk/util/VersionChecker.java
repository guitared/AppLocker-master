package com.nomorerowk.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.nomorerowk.util.VersionChecker.VersionInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.nomorerowk.locker.R;

/**
 * This class provides an easy way to check outdated versions.
 * 
 * @author Twinone
 * 
 */
public class VersionChecker extends AsyncTask<String, Void, VersionInfo> {

	private static final String PREF_FILE_NAME = "com.nomorerowk.version";
	private static final String PREF_KEY_DEPRECATED_VERSION = "com.nomorerowk.util.version.deprecated_version";
	private static final String PREF_KEY_AVAILABLE_VERSION = "com.nomorerowk.util.version.current_version";
	private static final String PREF_KEY_LAST_CHECK = "com.nomorerowk.util.version.last_check";

	/**
	 * If this flag is set, the user will not be allowed to use the app if it's
	 * deprecated. <br>
	 * <b>Note:</b> This automatically enables {@link #SHOW_UPDATE}
	 */
	private static final int DENY_DEPRECATED = 1;
	/**
	 * If this flag is set and an update is available, a dialog will be shown to
	 * the user.
	 */
	private static final int SHOW_UPDATE = 1 << 1;
	/**
	 * If you provide this flag, a checksum must be used in the file in order
	 * for this {@link VersionChecker} to complete.
	 */
	private static final int REQUIRE_CHECKSUM = 1 << 2;

	private static final String TAG = "VersionChecker";
	private static final String FIELD_CURRENT = "CURRENT";
	private static final String FIELD_DEPRECATED = "DEPRECATED";
	private static final String FIELD_CHECKSUM = "CHECKSUM";
	private static final String FIELD_POINTER = "POINTER";

	public static final long INTERVAL_HALF_MINUTE_DEBUG = 30;
	public static final long INTERVAL_DAILY = 86400; // 60 * 60 * 24
	/** Once every two days */
	private static final long INTERVAL_BI_DAILY = 172800; // 60 * 60 * 24 * 2
	public static final long INTERVAL_WEEKLY = 604800; // 60 * 60 * 24 * 7
	/** Once every two weeks */
	public static final long INTERVAL_BI_WEEKLY = 1209600; // 60 * 60 * 24 * 14

	private static final int MAX_REDIRECTS = 1;

	private final String mUrl;
	private Listener mListener;
	private final Context mContext;
	private final int mInstalledVersion;
	private int mNumRedirects;
	private int mFlags = SHOW_UPDATE | DENY_DEPRECATED;
	private int mPrefsDeprecatedVersion;
//	private int mPrefsCurrentVersion;
	private long mInterval = INTERVAL_BI_DAILY;
	private final long mLastUpdate;

	private VersionChecker(Context context, String url) {
		mUrl = url;
		mContext = context;
		mInstalledVersion = getInstalledVersion(context);
		mPrefsDeprecatedVersion = getPrefs(context).getInt(
				PREF_KEY_DEPRECATED_VERSION, 0);
		// mPrefsCurrentVersion = getPrefs(context).getInt(
		// PREF_KEY_AVAILABLE_VERSION, 0);
		mLastUpdate = getPrefs(context).getLong(PREF_KEY_LAST_CHECK, 0);
	}

	private static int getInstalledVersion(Context context) {
		int ver = 0;
		try {
			ver = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
		}
		return ver;
	}

	private long timeSinceEpoch() {
		return System.currentTimeMillis() / 1000;
	}

	private static SharedPreferences getPrefs(Context c) {
		return c.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
	}

	public VersionChecker(Context context, String url, Listener listener) {
		this(context, url);
		mListener = listener;

	}

	public interface Listener {
		public void onVersionInfoReceived(VersionInfo versionInfo);
	}

	public class VersionInfo {
		/**
		 * The last available version
		 */
		public int current;
		/**
		 * All versions equal or below this version should not be allowed to
		 * run.
		 */
		public int deprecated;

	}

	@Override
	protected VersionInfo doInBackground(String... params) {
		if (mLastUpdate == 0 || mLastUpdate + mInterval < timeSinceEpoch()) {
			Log.d(TAG, "Executing...");
			return getVersionInfoFromHttp(mUrl);
		}
		Log.d(TAG, "Waiting " + (mLastUpdate + mInterval - timeSinceEpoch())
				+ " Seconds");
		return new VersionInfo();
	}

	@Override
	protected void onPostExecute(VersionInfo result) {
		super.onPostExecute(result);
		if (result == null)
			return;
		if (mListener != null) {
			mListener.onVersionInfoReceived(result);
		}
		// Only write to preferences if connection succeeded
		savePreferences(result);

		Log.d(TAG, "result.current: " + result.current + " result.deprecated: "
				+ result.deprecated);
		Log.d(TAG, "installed version: " + mInstalledVersion);
		if ((mFlags & DENY_DEPRECATED) != 0) {
			if (isDeprecated(result.deprecated, mInstalledVersion)
					|| isDeprecated(mPrefsDeprecatedVersion, mInstalledVersion)) {
				showDeprecationDialog();
			}
		}
		if ((mFlags & SHOW_UPDATE) != 0) {
			if (isUpdateAvailable(result.current, mInstalledVersion)
			// ||
			// isUpdateAvailable(mPrefsCurrentVersion,
			// mInstalledVersion)
			) {
				showUpdateAvailableDialog();
			}
		}

		// if ((mFlags & DENY_DEPRECATED) != 0
		// && mPrefsDeprecatedVersion >= mInstalledVersion) {
		// Log.d(TAG, "Deprecated from preferences");
		// showDeprecationDialog();
		// } else if ((mFlags & DENY_DEPRECATED) != 0 && result.isDeprecated())
		// {
		// showDeprecationDialog();
		// } else if ((mFlags & NOTIFY_UPDATE) != 0
		// && result.current < mPrefsCurrentVersion) {
		// Log.d(TAG, "" + result.current + ", " + mPrefsCurrentVersion);
		// showUpdateAvailableDialog();
		// } else if ((mFlags & NOTIFY_UPDATE) != 0 &&
		// result.isUpdateAvailable()) {
		// Log.d(TAG, "show update dialog 2");
		// showUpdateAvailableDialog();
		// }
	}

	private static boolean isUpdateAvailable(int current, int installed) {
		if (current == 0)
			return false;
		return installed < current;
	}

	private static boolean isDeprecated(int deprecated, int installed) {
		if (deprecated == 0)
			return false;
		return installed <= deprecated;
	}

	private void savePreferences(VersionInfo vi) {
		SharedPreferences.Editor editor = getPrefs(mContext).edit();
		if (vi.deprecated != 0) {
			editor.putInt(PREF_KEY_AVAILABLE_VERSION, vi.current);
			mPrefsDeprecatedVersion = vi.deprecated;
		}
		if (vi.current != 0) {
			editor.putInt(PREF_KEY_DEPRECATED_VERSION, vi.deprecated);
//			mPrefsCurrentVersion = vi.current;
		}
		editor.putLong(PREF_KEY_LAST_CHECK, timeSinceEpoch());
		editor.apply();

	}

	private void showDeprecationDialog() {
		AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
		ab.setTitle(R.string.vc_tit_unsupported);
		ab.setMessage(R.string.vc_msg_unsupported);
		ab.setNegativeButton(android.R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				((Activity) mContext).finish();
			}
		});
		ab.setPositiveButton(R.string.vc_update, mUpdateListener);
		ab.setCancelable(false);
		ab.show();
	}

	private final DialogInterface.OnClickListener mUpdateListener = new OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String str = "https://play.google.com/store/apps/details?id="
					+ mContext.getPackageName();
			mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse(str)));
		}
	};

	private void showUpdateAvailableDialog() {
		AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
		ab.setTitle(R.string.vc_tit_new_version);
		ab.setMessage(R.string.vc_msg_new_version);
		ab.setNegativeButton(R.string.vc_later, null);
		ab.setPositiveButton(R.string.vc_update, mUpdateListener);
		ab.show();
	}

	private VersionInfo getVersionInfoFromHttp(String url) {
		VersionInfo res = new VersionInfo();
		boolean passed = false;
		Log.d(TAG, "Attempting to get file from url: " + url);
		HttpClient hc = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse resp;
		try {
			resp = hc.execute(get);
			Log.d(TAG, "Response:" + resp.getStatusLine().toString());
			HttpEntity entity = resp.getEntity();
			InputStream is = entity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String readLine = null;
			while ((readLine = br.readLine()) != null) {
				try {
					String[] line = readLine.split(" ");
					Log.d(TAG, readLine);
                    switch (line[0]) {
                        case FIELD_CURRENT:
                            res.current = Integer.parseInt(line[1]);
                            break;
                        case FIELD_DEPRECATED:
                            res.deprecated = Integer.parseInt(line[1]);
                            break;
                        case FIELD_POINTER:
                            Log.w(TAG, "Pointer detected: " + line[1]);
                            is.close();
                            mNumRedirects++;
                            if (mNumRedirects > MAX_REDIRECTS) {
                                Log.w(TAG,
                                        "Maximum redirections exceeded, not redirecting");
                            } else {
                                return getVersionInfoFromHttp(line[1]);
                            }
                            break;
                        case FIELD_CHECKSUM:
                            if (check(res, line[1])) {
                                passed = true;
                            }
                            break;
                    }
				} catch (Exception e) {
					Log.d(TAG, "Exception processing Version info", e);
				}
			}
			is.close();
		} catch (Exception e) {
			Log.w(TAG, "Error getting file from http:", e);
		}
		if ((REQUIRE_CHECKSUM & mFlags) != 0 && !passed) {
			Log.w(TAG,
					"This VersionChecker requres a checksum which didn't match, aborting");
			return new VersionInfo();
		}
		return res;
	}

	private static boolean check(VersionInfo vi, String checkSum) {
		try {

			// The VersionInfo
			byte[] viInputBytes = ("com.nomorerowk.locker " + vi.current + " " + vi.deprecated)
					.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] viDigest = md.digest(viInputBytes);
			return bytesToHexString(viDigest).equals(checkSum);
		} catch (Exception e) {
			return false;
		}
	}

	private static String bytesToHexString(byte[] hash) {
		StringBuilder sb = new StringBuilder(32);
        for (byte aHash : hash) {
            if ((0xff & aHash) < 0x10) {
                sb.append("0" + Integer.toHexString((0xFF & aHash)));
            } else {
                sb.append(Integer.toHexString(0xFF & aHash));
            }
        }
		return sb.toString();
	}

	/**
	 * Every how much should we check (approximately)<br>
	 * Default is {@link #INTERVAL_BI_WEEKLY}<br>
	 * One of {@link #INTERVAL_BI_DAILY}, {@link #INTERVAL_DAILY},
	 * {@link #INTERVAL_BI_WEEKLY}, {@link #INTERVAL_WEEKLY}
	 * 
	 * @param interval
	 */
	public void setInterval(long interval) {
		this.mInterval = interval;
	}

	public int installedVersion() {
		return mInstalledVersion;
	}

	public void setFlags(int newFlags) {
		mFlags = newFlags;
	}

	public void addFlags(int newFlags) {
		mFlags |= newFlags;
	}

	public void removeFlags(int removeFlags) {
		mFlags &= ~removeFlags;
	}

	public static boolean isDeprecated(Context c) {
		int deprecated = c.getSharedPreferences(PREF_FILE_NAME,
				Context.MODE_PRIVATE).getInt(PREF_KEY_DEPRECATED_VERSION, 0);
		int installed = getInstalledVersion(c);
		return isDeprecated(deprecated, installed);
	}

	public static boolean isUpdateAvailable(Context c) {
		int available = c.getSharedPreferences(PREF_FILE_NAME,
				Context.MODE_PRIVATE).getInt(PREF_KEY_AVAILABLE_VERSION, 0);
		int installed = getInstalledVersion(c);
		return isUpdateAvailable(available, installed);

	}
}
