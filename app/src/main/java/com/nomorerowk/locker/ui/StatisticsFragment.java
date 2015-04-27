package com.nomorerowk.locker.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nomorerowk.locker.R;
import com.nomorerowk.locker.util.PrefUtils;

import java.util.Timer;
import java.util.TimerTask;

public class StatisticsFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_statistics, container,
				false);


		getActivity().setTitle(R.string.fragment_title_statistics);
		TextView t = (TextView) root.findViewById(R.id.test_time);
		PrefUtils prefs = new PrefUtils(getActivity());
		t.setText(prefs.getString(R.string.pref_key_time));
		Long tsLong = System.currentTimeMillis()/1000;
		String ts = tsLong.toString();
		TextView t2 = (TextView) root.findViewById(R.id.textView6);
		t2.setText(ts);
		return root;
	}

}
