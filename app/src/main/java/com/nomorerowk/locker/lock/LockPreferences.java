package com.nomorerowk.locker.lock;

import java.io.Serializable;

import com.nomorerowk.locker.util.PrefUtils;

import android.content.Context;

import com.nomorerowk.locker.R;

public class LockPreferences implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2334826883469805015L;

	public static final int TYPE_PASSWORD = 1; // 1
	public static final int TYPE_PATTERN = 1 << 1; // 2

	// Common
	/** Whether this user has pro features enabled or no */
    private boolean pro;
	public int type;
	public final String orientation;
	public final Boolean vibration;
	public final String message;
	public int patternSize;

	// Pro only
	public final String background;
	public final int showAnimationResId;
	public final int hideAnimationResId;
	public final int showAnimationMillis;
	public final int hideAnimationMillis;

	// Password only
	public final String password;
	public final boolean passwordSwitchButtons;

	// Pattern only
	public final String pattern;
	public boolean patternStealth;
	public final boolean patternErrorStealth;

	//time
	public final String time;

	// Pro & pattern only
	public final int patternCircleResId;

	public boolean showAds = true;

	/**
	 * You should use this constructor which loads all properties into the
	 * object automatically
	 * 
	 * @param c
	 * @return
	 */
	public LockPreferences(Context c) {
		PrefUtils prefs = new PrefUtils(c);
		// Common
		type = prefs.getCurrentLockTypeInt();
		orientation = prefs.getString(R.string.pref_key_orientation);
		vibration = prefs.getBoolean(R.string.pref_key_vibrate,
				R.bool.pref_def_vibrate);
		message = prefs.getString(R.string.pref_key_lock_message);

		if (pro) {
			background = prefs.getString(R.string.pref_key_background,
					R.string.pref_def_background);
			// Show animation
			final String showAnim = prefs.getString(
					R.string.pref_key_anim_show_type,
					R.string.pref_def_anim_show_type);
			showAnimationResId = getAnimationResId(c, showAnim, true);
			showAnimationMillis = prefs.parseInt(
					R.string.pref_key_anim_show_millis,
					R.string.pref_def_anim_show_millis);

			// Hide animation
			final String hideAnim = prefs.getString(
					R.string.pref_key_anim_hide_type,
					R.string.pref_def_anim_hide_type);
			hideAnimationResId = getAnimationResId(c, hideAnim, false);
			hideAnimationMillis = prefs.parseInt(
					R.string.pref_key_anim_hide_millis,
					R.string.pref_def_anim_hide_millis);
		} else {
			background = c.getString(R.string.pref_def_background);
			// Show animation
			final String showAnim = c
					.getString(R.string.pref_def_anim_show_type);
			showAnimationResId = getAnimationResId(c, showAnim, true);
			showAnimationMillis = Integer.parseInt(c
					.getString(R.string.pref_def_anim_show_millis));

			// Hide animation
			final String hideAnim = c
					.getString(R.string.pref_def_anim_hide_type);
			hideAnimationResId = getAnimationResId(c, hideAnim, false);
			hideAnimationMillis = Integer.parseInt(c
					.getString(R.string.pref_def_anim_hide_millis));
		}

		// Load both password and pattern because user could override the type
		// setting
		password = prefs.getString(R.string.pref_key_password);
		passwordSwitchButtons = prefs.getBoolean(
				R.string.pref_key_switch_buttons,
				R.bool.pref_def_switch_buttons);

		pattern = prefs.getString(R.string.pref_key_pattern);
		patternStealth = prefs.getBoolean(R.string.pref_key_pattern_stealth,
				R.bool.pref_def_pattern_stealth);
		patternErrorStealth = prefs.getBoolean(
				R.string.pref_key_pattern_hide_error,
				R.bool.pref_def_pattern_error_stealth);
		patternSize = prefs.parseInt(R.string.pref_key_pattern_size,
				R.string.pref_def_pattern_size);

		patternCircleResId = getPatternCircleResId(c, pro,
				prefs.getString(R.string.pref_key_pattern_color));

		time = prefs.getString(R.string.pref_key_time);
	}

	/**
	 * @param show
	 *            true if show animation, false if hide animation
	 * @return the resid to be applied
	 */
	private static int getAnimationResId(Context c, String type, boolean show) {
		if (type != null) {
			if (type.equals(c.getString(R.string.pref_val_anim_slide_left)))
				return show ? R.anim.slide_in_left : R.anim.slide_out_left;
			else if (type.equals(c
					.getString(R.string.pref_val_anim_slide_right)))
				return show ? R.anim.slide_in_right : R.anim.slide_out_right;
			else if (type.equals(c.getString(R.string.pref_val_anim_fade)))
				return show ? R.anim.fade_in : R.anim.fade_out;
		}
		return 0;
	}

	private static int getPatternCircleResId(Context c, boolean hasPro,
			String setting) {
		if (setting != null && hasPro) {
			if (setting.equals(c
					.getString(R.string.pref_val_pattern_color_blue)))
				return R.drawable.pattern_circle_blue;
			if (setting.equals(c
					.getString(R.string.pref_val_pattern_color_green)))
				return R.drawable.pattern_circle_green;
		}
		return R.drawable.pattern_circle_white;
	}
}
