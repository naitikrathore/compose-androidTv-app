package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
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

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;


import java.util.ArrayList;

/**
 * IP Manual Scan fragment
 *
 * @author Dejan Nadj
 */
public class IpManualScanFragment extends GenericFragment {

    /**
     * Main fragment text views
     */
    private ReferenceDrawableButton enterUrlTv;
    private ReferenceDrawableButton browseLocallyTv;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.ip_manual_scan_layout, container, false);

        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("ip_manual_scan"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextSize(21);
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        enterUrlTv = view.findViewById(R.id.enter_url);
        enterUrlTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        browseLocallyTv = view.findViewById(R.id.browse_locally);
        browseLocallyTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        enterUrlTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
        browseLocallyTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));

        enterUrlTv.getTextView().setText(ConfigStringsManager.Companion.getStringById("enter_url"));
        browseLocallyTv.getTextView().setText(ConfigStringsManager.Companion.getStringById("browse_locally"));

        enterUrlTv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    enterUrlTv.setBackground(selectorDrawable);

                    enterUrlTv.getTextView().setTextSize(15);
                    enterUrlTv.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    enterUrlTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    enterUrlTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
                    enterUrlTv.getTextView().setTextSize(13);
                    enterUrlTv.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    enterUrlTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        browseLocallyTv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    browseLocallyTv.setBackground(selectorDrawable);

                    browseLocallyTv.getTextView().setTextSize(15);
                    browseLocallyTv.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    browseLocallyTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    browseLocallyTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
                    browseLocallyTv.getTextView().setTextSize(13);
                    browseLocallyTv.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    browseLocallyTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });


        enterUrlTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IwediaSetupActivity) getActivity()).showFragment(new EnterUrlFragment());
            }
        });
        browseLocallyTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<ScanningFragment.ScanningType> scanningTypes = new ArrayList<>();
                scanningTypes.add(ScanningFragment.ScanningType.IP);
                ScanningFragment scanningFragment = new ScanningFragment();
                scanningFragment.setScanningTypeList(scanningTypes);
                scanningFragment.setManualScan();
                ((IwediaSetupActivity) getActivity()).showFragment(scanningFragment);
//                ScanHandler.getInstance().startIpScan(null);
            }
        });
        //Select tuning option
        //Selected option should be passed on back from opened tuning screen (Enter URL/ Browse Locally)
        if (getArguments() != null && getArguments().containsKey(MainFragment.SELECTED_OPTION)) {
            final int selected = getArguments().getInt(MainFragment.SELECTED_OPTION);
            if (selected == 1) {
                browseLocallyTv.requestFocus();
            } else {
                enterUrlTv.requestFocus();
            }
        } else {
            enterUrlTv.requestFocus();
        }
        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (enterUrlTv.hasFocus()) {
                    browseLocallyTv.requestFocus();
                }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (browseLocallyTv.hasFocus()) {
                    enterUrlTv.requestFocus();
                }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new ManualScanFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 3);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
