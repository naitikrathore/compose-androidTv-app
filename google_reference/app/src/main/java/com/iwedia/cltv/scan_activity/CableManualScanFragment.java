package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

/**
 * Cable manual scan fragment
 *
 * @author Dejan Nadj
 */
public class CableManualScanFragment extends GenericFragment {

    /**
     * Network id edit text
     */
    private EditTextView nitEv;

    /**
     * Nit check box
     */
    private ReferenceDrawableButton nitCb;

    /**
     * Modulation traversal list
     */
    private TraversalListView modulationTl;

    /**
     * Scan button
     */
    private ReferenceDrawableButton scanButton;

    /**
     * Tune button
     */
    private ReferenceDrawableButton tuneButton;

    /**
     * Symbol rate edit text
     */
    private EditTextView symbolRateEv;

    /**
     * Signal strength progress bar
     */
    private ProgressBarView signalStrengthProgress;

    /**
     * Signal quality progress bar
     */
    private ProgressBarView signalQualityProgress;

    /**
     * Keyboard layout
     */
    private RelativeLayout keyboardLayout;

    /**
     * Keyboard view
     */
    private KeyboardView keyboardView;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.cable_manual_scan_layout, container, false);
        View view_layout = (View) view.findViewById(R.id.view_layout);
        view_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        final RelativeLayout mainContainer = (RelativeLayout) view.findViewById(R.id.main_container);

        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("cable_manual_scan"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init frequency edit text
        final EditTextView frequencyEv = (EditTextView) view.findViewById(R.id.frequency_edit_text);
        frequencyEv.setTitle(ConfigStringsManager.Companion.getStringById("frequency_cable_manual_tuning"));
        frequencyEv.setText("270000");

        keyboardLayout = (RelativeLayout) view.findViewById(R.id.keyboard_layout);
        keyboardLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_background")));
        keyboardView = (KeyboardView) view.findViewById(R.id.keyboard);
        keyboardView.setNumericType();

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                keyboardLayout.setVisibility(View.VISIBLE);
                keyboardView.setVisibility(View.VISIBLE);
                keyboardView.requestFocus();
                keyboardView.setEditText((EditText) v);
                if (v == nitEv.getEditText()) {
                    float posY = container.getY() - 200;
                    mainContainer.animate().y(posY);
                }

                keyboardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (keyboardView.getVisibility() == View.GONE) {
                            keyboardLayout.setVisibility(View.GONE);
                            v.requestFocus();
                            if (v == nitEv.getEditText()) {
                                mainContainer.animate().y(0);
                            }
                            keyboardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        };
        frequencyEv.getEditText().setOnClickListener(clickListener);

        //Init bandwidth traversal list
        modulationTl = (TraversalListView) view.findViewById(R.id.modulation_tl);
        modulationTl.setTitle(ConfigStringsManager.Companion.getStringById("modulation_cable"));
        ArrayList<String> modulationItems = new ArrayList<>();
        modulationItems.add("16 QAM");
        modulationItems.add("32 QAM");
        modulationItems.add("64 QAM");
        modulationItems.add("128 QAM");
        modulationItems.add("256 QAM");
        modulationTl.setItems(modulationItems);

        //Init symbol rate edit text
        symbolRateEv = (EditTextView) view.findViewById(R.id.symbol_rate_edit_text);
        symbolRateEv.setTitle(ConfigStringsManager.Companion.getStringById("symbol_rate_cable"));
        symbolRateEv.setText("6590");
        symbolRateEv.getEditText().setOnClickListener(clickListener);

        //Init signal strength progress bar
        signalStrengthProgress = (ProgressBarView) view.findViewById(R.id.signal_strength_progress);
        signalStrengthProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgress.setProgress(0);

        //Init signal quality progress bar
        signalQualityProgress = (ProgressBarView) view.findViewById(R.id.signal_quality_progress);
        signalQualityProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgress.setProgress(0);

        //Init NIT id edit text
        nitEv = (EditTextView) view.findViewById(R.id.nit_edit_text);
        nitEv.setTitle(ConfigStringsManager.Companion.getStringById("nit_id"));
        nitEv.setFocusable(true);
        nitEv.getEditText().setText("1");
        //nitEv.getEditText().setHint("0");
        nitEv.getEditText().setOnClickListener(clickListener);

        /**
         * Init nit check box
         */
        nitCb = view.findViewById(R.id.nit_cb);
        nitCb.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        nitCb.setText(ConfigStringsManager.Companion.getStringById("nit"));
        nitCb.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
        nitCb.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_field));
        nitCb.getTextView().setText(ConfigStringsManager.Companion.getStringById("nit"));
        nitCb.setVisibility(View.GONE);

        nitCb.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    v.setBackground(selectorDrawable);

                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        nitCb.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    nitCb.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        nitCb.getDrawable().setColorFilter(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                } else {
                    v.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
                    nitCb.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        nitCb.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        nitCb.getDrawable().setColorFilter(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                }
            }
        });

        nitCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nitCb.isChecked()) {
                    nitCb.setUnchecked();
                } else {
                    nitCb.setChecked();
                }
                nitEv.setFocusable(nitCb.isChecked());
            }
        });

        //Tune button click listener
        tuneButton = view.findViewById(R.id.tune_button);
        tuneButton.setText(ConfigStringsManager.Companion.getStringById("tune"));
        tuneButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("tune"));
        tuneButton.setDrawable(null);
        tuneButton.post(new Runnable() {
            @Override
            public void run() {
                tuneButton.requestFocus();
            }
        });

        tuneButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    tuneButton.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.focus_shape));
                    tuneButton.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        tuneButton.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    tuneButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    tuneButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        tuneButton.getBackground().setColorFilter(color_context, PorterDuff.Mode.SRC_OVER);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    tuneButton.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        tuneButton.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    tuneButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        tuneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);

//                ScanHandler.getInstance().tuneCable(frequencyEv.getEditTextString(), symbolRateEv.getEditTextString(), modulationTl.getCurrentItem());
            }
        });

        //Scan button click listener
        scanButton = view.findViewById(R.id.scan_button);
        scanButton.setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.setDrawable(null);

        scanButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    scanButton.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.focus_shape));
                    scanButton.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        scanButton.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    scanButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    scanButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        scanButton.getBackground().setColorFilter(color_context, PorterDuff.Mode.SRC_OVER);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    scanButton.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        scanButton.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    scanButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                ArrayList<ScanningFragment.ScanningType> scanningTypes = new ArrayList<>();
                scanningTypes.add(ScanningFragment.ScanningType.CABLE);
                ScanningFragment scanningFragment = new ScanningFragment();
                scanningFragment.setScanningTypeList(scanningTypes);
                scanningFragment.setScanningProgressDescription("Frequency: " + frequencyEv.getEditTextString() + " kHz");
                scanningFragment.disableBack();
                scanningFragment.setManualScan();
                ((IwediaSetupActivity) getActivity()).showFragment(scanningFragment);
                ScanHelper.INSTANCE.setActiveFragment(scanningFragment);
                ScanHelper.INSTANCE.startDvbCManualScan(Integer.valueOf(nitEv.getEditTextString()),
                        Integer.valueOf(frequencyEv.getEditTextString()),
                        getModulation(modulationTl.getCurrentItem()),
                        Integer.valueOf(symbolRateEv.getEditTextString()));
            }
        });

        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (nitEv.hasFocus()) {
                    modulationTl.requestFocus();
                    return true;
                }
                if (scanButton.hasFocus()) {
                    symbolRateEv.requestFocus();
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (nitEv.hasFocus()) {
                    return true;
                }
                if (nitCb.hasFocus() && !nitCb.isChecked()) {
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (tuneButton.hasFocus()) {
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (symbolRateEv.hasFocus()) {
                    scanButton.requestFocus();
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new ManualScanFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 1);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }

    @Override
    public void scanSignalStrengthChanged(int signalStrength) {
        super.scanSignalStrengthChanged(signalStrength);
        signalStrengthProgress.setProgress(signalStrength);
    }

    @Override
    public void scanSignalQualityChanged(int signalQuality) {
        super.scanSignalQualityChanged(signalQuality);
        signalQualityProgress.setProgress(signalQuality);
    }

    private String getModulation(int position){
        switch (position) {
            case 0: {
                return "qam_16";
            }
            case 1: {
                return "qam_32";
            }
            case 2: {
                return "qam_64";
            }
            case 3: {
                return "qam_128";
            }
            case 4: {
                return "qam_256";
            }
        }
        return "qam_256";
    }
}
