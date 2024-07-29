package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.utils.Utils;

/**
 * USALS Motor Setup fragment
 *
 * @author Dejan Nadj
 */
public class UsalsMotorSetupFragment extends GenericFragment {

    /**
     * Save button
     */
    private SimpleTextView saveButton;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.usals_motor_setup_layout, container, false);

        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("usals_motor_setup"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init satellite angle traversal list
        TraversalListView satelliteAngle = (TraversalListView) view.findViewById(R.id.satellite_angle_tl);
        satelliteAngle.setTitle(ConfigStringsManager.Companion.getStringById("satellite_angle"));
        satelliteAngle.setCustomValues(19.0f, 30.0f, 0.1f, "W", "%05.1f");

        //Init longitude traversal list
        TraversalListView longitude = (TraversalListView) view.findViewById(R.id.longitude_tl);
        longitude.setTitle(ConfigStringsManager.Companion.getStringById("longitude"));
        longitude.setCustomValues(5.0f, 10.0f, 0.5f, "W","%05.1f");

        //Init local latitude traversal list
        TraversalListView localLatitude = (TraversalListView) view.findViewById(R.id.local_latitude_tl);
        localLatitude.setTitle(ConfigStringsManager.Companion.getStringById("local_latitude"));
        localLatitude.setCustomValues(50.0f, 100.0f, 0.5f, "N","%02.1f");

        //Init signal strength progress bar
        ProgressBarView signalStrengthProgress = (ProgressBarView) view.findViewById(R.id.signal_strength_progress);
        signalStrengthProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgress.setProgress(80);

        //Init signal quality progress bar
        ProgressBarView signalQualityProgress = (ProgressBarView) view.findViewById(R.id.signal_quality_progress);
        signalQualityProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgress.setProgress(90);

        //Save button click listener
        saveButton = (SimpleTextView) view.findViewById(R.id.save_button);
        saveButton.setText(ConfigStringsManager.Companion.getStringById("save"));
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Utils.Companion.clickAnimation(v);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        saveButton.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (previousFragment != null) {
                        ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                    } else {
                        ((IwediaSetupActivity) getActivity()).showFragment(new AntennaConfigFragment());
                    }
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
