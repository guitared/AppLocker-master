package com.nomorerowk.locker.ui;

import com.nomorerowk.locker.lock.LockPreferences;
import com.nomorerowk.locker.lock.LockService;
import com.nomorerowk.locker.util.PrefUtils;
import com.nomorerowk.util.Analytics;
import com.nomorerowk.util.DialogSequencer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.nomorerowk.locker.R;

class Dialogs {

	/**
	 * The dialog that allows the user to select between password and pattern
	 * options
	 * 
	 * @param c
	 * @return
	 */
	public static AlertDialog getChangePasswordDialog(final Context c) {
		final AlertDialog.Builder choose = new AlertDialog.Builder(c);
		choose.setTitle(R.string.old_main_choose_lock_type);
		choose.setItems(R.array.lock_type_names, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int type = which == 0 ? LockPreferences.TYPE_PASSWORD
						: LockPreferences.TYPE_PATTERN;
				LockService.showCreate(c, type);
			}
		});
		return choose.create();
	}

	public static AlertDialog getTimerDialog(final Context c) {
		//guitared
		final AlertDialog.Builder choose = new AlertDialog.Builder(c);
		choose.setTitle("Set The Timer");
		choose.setItems(R.array.nav_timer, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// The 'which' argument contains the index position
				// of the selected item
				long tsLong = System.currentTimeMillis()/1000;
				int min=0;
				switch(which){
					case 0: //1 min
						min=1;
						break;
					case 1: //10 min
						min=10;
						break;
					case 2: //1 hr
						min=60;
						break;
					case 3: //6 hr
						min=360;
						break;
					case 4: //12 hr
						min=720;
						break;
					case 5: //1 day
						min=1440;
						break;
					case 6: //2 day
						min=2880;
						break;
					case 7: //1 week
						min=10080;
						break;
				}
				min = (int)tsLong+(min*60);
				String ts = min+"";
				PrefUtils prefs = new PrefUtils(c);
				prefs.put(R.string.pref_key_time, ts);
				prefs.apply();
			}
		});
		return choose.create();
	}

	// private void showVersionDialogs() {
	// if (mVersionManager.isDeprecated()) {
	// new VersionUtils(this).getDeprecatedDialog().show();
	// } else if (mVersionManager.shouldWarn()) {
	// new VersionUtils(this).getUpdateAvailableDialog().show();
	// }
	// }

	/**
	 * 
	 * @param c
	 * @param ds
	 * @return True if the dialog was added
	 */
	public static boolean addEmptyPasswordDialog(Context c,
			final DialogSequencer ds) {
		final boolean empty = new PrefUtils(c).isCurrentPasswordEmpty();
		if (empty) {
			ds.addDialog(getChangePasswordDialog(c));
			return true;
		}
		return false;
	}

	// private static AlertDialog getEmptyPasswordDialog(Context c,
	// final DialogSequencer ds) {
	//
	// final AlertDialog.Builder msg = new AlertDialog.Builder(c);
	// msg.setTitle(R.string.main_setup);
	// msg.setMessage(R.string.main_no_password);
	// msg.setCancelable(false);
	// msg.setPositiveButton(android.R.string.ok, null);
	// msg.setNegativeButton(android.R.string.cancel, new OnClickListener() {
	// @Override
	// public void onClick(DialogInterface dialog, int which) {
	// ds.removeNext(dialog);
	// }
	// });
	// return msg.create();
	// }

	public static AlertDialog getRecoveryCodeDialog(final Context c) {
		PrefUtils prefs = new PrefUtils(c);
		String code = prefs.getString(R.string.pref_key_recovery_code);
		if (code != null) {
			return null;
		}
		// Code = null
		code = PrefUtils.generateRecoveryCode(c);
		// save it directly to avoid it to change
		prefs.put(R.string.pref_key_recovery_code, code).apply();
		final String finalcode = code;
		AlertDialog.Builder ab = new AlertDialog.Builder(c);
		ab.setCancelable(false);
		ab.setNeutralButton(R.string.recovery_code_send_button,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(
								android.content.Intent.ACTION_SEND);
						i.setType("text/plain");
						i.putExtra(Intent.EXTRA_TEXT, c.getString(
								R.string.recovery_intent_message, finalcode));
						c.startActivity(Intent.createChooser(i,
								c.getString(R.string.recovery_intent_tit)));
					}
				});
		ab.setPositiveButton(android.R.string.ok, null);
		ab.setTitle(R.string.recovery_tit);
		ab.setMessage(String.format(c.getString(R.string.recovery_dlgmsg),
				finalcode));
		return ab.create();
	}

	/**
	 * 
	 * Get the dialog to share the app
	 */
	public static AlertDialog getShareEditDialog(final Context c,
			boolean addNeverButton) {
		String promoText = c.getString(R.string.share_promo_text);
		final AlertDialog.Builder ab = new AlertDialog.Builder(c);

		LayoutInflater inflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View v = inflater.inflate(R.layout.share_dialog, null);
		ab.setView(v);
		final EditText et = (EditText) v
				.findViewById(R.id.share_dialog_et_content);
		et.setText(promoText);

		ab.setCancelable(false);
		ab.setTitle(R.string.lib_share_dlg_tit);
		ab.setMessage(R.string.lib_share_dlg_msg);
		ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				intent.setType("text/plain");
				final String text = et.getText().toString();
				intent.putExtra(Intent.EXTRA_TEXT, text);
				Intent sender = Intent.createChooser(intent,
						c.getString(R.string.lib_share_dlg_tit));
				Analytics anal = new Analytics(c);
				c.startActivity(sender);
				// At this point, we can assume the user will share the app.
				// So never show the dialog again, he can manually open it from
				// the navigation
			}
		});
		ab.setNeutralButton(R.string.share_dlg_later, null);
		if (addNeverButton) {
			ab.setNegativeButton(R.string.share_dlg_never,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
		}
		return ab.create();
	}
}
