package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.scan_activity.entities.SatelliteTransponder;
import com.iwedia.cltv.utils.Utils;


import java.util.ArrayList;

/**
 * Satellite scan options fragment
 *
 * @author Dejan Nadj
 */
public class SatelliteScanOptionsFragment extends GenericFragment {

    /**
     * Previous screen selected list option position
     */
    private int satellitePosition;

    /**
     * Satellite name
     */
    private String satelliteName;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.satellite_scan_options_layout, container, false);
        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("scan_options"));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        //Init search type traversal list
        TraversalListView searchType = (TraversalListView) view.findViewById(R.id.search_type);
        searchType.setTitle(ConfigStringsManager.Companion.getStringById("search_type"));
        ArrayList<String> items = new ArrayList<>();
        items.add(ConfigStringsManager.Companion.getStringById("scrambled"));
        items.add(ConfigStringsManager.Companion.getStringById("descrambled"));
        searchType.setItems(items);

        //Init network search traversal list
        TraversalListView networkSearch = (TraversalListView) view.findViewById(R.id.network_search);
        networkSearch.setTitle(ConfigStringsManager.Companion.getStringById("network_search"));
        ArrayList<String> items1 = new ArrayList<>();
        items1.add(ConfigStringsManager.Companion.getStringById("on"));
        items1.add(ConfigStringsManager.Companion.getStringById("off"));
        networkSearch.setItems(items1);

        //Start button click listener
        ReferenceDrawableButton startButton = (ReferenceDrawableButton) view.findViewById(R.id.start_button);

        startButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("start"));
        startButton.setDrawable(null);

        startButton.post(new Runnable() {
            @Override
            public void run() {
                startButton.requestFocus();
            }
        });

        startButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    startButton.setBackground(selectorDrawable);

                    startButton.getTextView().setTextSize(15);
                    startButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    startButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    startButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    startButton.getBackground().setColorFilter(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")), PorterDuff.Mode.SRC_OVER);
                    startButton.getTextView().setTextSize(15);
                    startButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    startButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                ScanningFragment fragment = new ScanningFragment();
                ArrayList<ScanningFragment.ScanningType> scanningTypes = new ArrayList<>();
                scanningTypes.add(ScanningFragment.ScanningType.SATELLITE);
                fragment.setScanningTypeList(scanningTypes);
                fragment.setPreviousFragment(SatelliteScanOptionsFragment.this);
                fragment.setManualScan();
                ((IwediaSetupActivity)getActivity()).hideFragment(SatelliteScanOptionsFragment.this, fragment);
                SatelliteHelperClass.setSatelliteTransponder(new SatelliteTransponder(18887, 30000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S));
                SatelliteTransponder satelliteTransponder = SatelliteHelperClass.getSatelliteTransponder();
//                ScanHandler.getInstance().startSatelliteManualScan(null, String.valueOf(satelliteTransponder.getFrequency()), String.valueOf(satelliteTransponder.getSymbolRate()),
//                        null, satelliteTransponder.getPolarization() == SatelliteTransponder.Polarization.HORIZONTAL);
            }
        });

        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (previousFragment != null) {
                    ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                } else {
                    GenericFragment fragment = new SatelliteOptionsFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                    fragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
