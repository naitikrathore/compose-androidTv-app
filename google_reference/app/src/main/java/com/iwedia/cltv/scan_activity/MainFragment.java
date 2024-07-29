package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
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
import com.iwedia.cltv.platform.model.Constants;


/**
 * Tv input activity main fragment
 *
 * @author Dejan Nadj
 */
public class MainFragment extends GenericFragment {

    /**
     * Selected option bundle argument key
     */
    public static final String SELECTED_OPTION = "selected_option_";

    /**
     * Main fragment text views
     */
    private ReferenceDrawableButton autoTuningTv;
    private ReferenceDrawableButton manualTuningTv;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.main_fragment, container, false);

        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("programme_tuning"));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        autoTuningTv = (ReferenceDrawableButton) view.findViewById(R.id.auto_tuning);
        autoTuningTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        manualTuningTv = (ReferenceDrawableButton) view.findViewById(R.id.manual_tuning);
        manualTuningTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        manualTuningTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
        autoTuningTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));

        manualTuningTv.getTextView().setText(ConfigStringsManager.Companion.getStringById("manual_tuning"));
        autoTuningTv.getTextView().setText(ConfigStringsManager.Companion.getStringById("auto_tuning"));

        manualTuningTv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    manualTuningTv.setBackground(selectorDrawable);

                    manualTuningTv.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        manualTuningTv.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    manualTuningTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    manualTuningTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
                    manualTuningTv.getTextView().setTextSize(13);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        manualTuningTv.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    manualTuningTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        autoTuningTv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    autoTuningTv.setBackground(selectorDrawable);

                    autoTuningTv.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        autoTuningTv.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    autoTuningTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    autoTuningTv.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
                    autoTuningTv.getTextView().setTextSize(13);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        autoTuningTv.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    autoTuningTv.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        autoTuningTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IwediaSetupActivity)getActivity()).showFragment(new AutoScanFragment());
            }
        });
        manualTuningTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IwediaSetupActivity)getActivity()).showFragment(new ManualScanFragment());
            }
        });

        //Select tuning option
        //Selected option should be passed on back from opened tuning screen (Auto Tuning/ Manual Tuning)
        if (getArguments() != null && getArguments().containsKey(MainFragment.SELECTED_OPTION)) {
            final int selected = getArguments().getInt(MainFragment.SELECTED_OPTION);
            if (selected == 1) {
                manualTuningTv.requestFocus();
            } else {
                autoTuningTv.requestFocus();
            }
        } else {
            autoTuningTv.requestFocus();
        }

        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (autoTuningTv.hasFocus()) {
                    manualTuningTv.requestFocus();
                }

                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (manualTuningTv.hasFocus()) {
                    autoTuningTv.requestFocus();
                }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                getActivity().onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
