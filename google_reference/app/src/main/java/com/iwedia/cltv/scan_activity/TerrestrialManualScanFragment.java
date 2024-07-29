package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import androidx.annotation.RequiresApi;
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
 * Terrestrial manual scan fragment
 *
 * @author Dejan Nadj
 */
public class TerrestrialManualScanFragment extends GenericFragment {

    /**
     * Frequency edit text view
     */
    private EditTextView frequencyEv;

    /**
     * Keyboard layout
     */
    private RelativeLayout keyboardLayout;

    /**
     * Keyboard view
     */
    private KeyboardView keyboardView;

    /**
     * Tune button
     */
    private ReferenceDrawableButton tuneButton;

    /**
     * Signal strength progress bar
     */
    private ProgressBarView signalStrengthProgress;

    /**
     * Signal quality progress bar
     */
    private ProgressBarView signalQualityProgress;

    private int firstChannelUHF8MHZ = 21;
    private int lastChannelUHF8MHZ = 69;
    private int firstChannelVHF7MHZ = 5;
    private int lastChannelVHF7MHZ = 12;
    private int lowFreqUHF8MHZ = 474000000;
    private int lowFreqVHF7MHZ = 177500000;
    private int bandwidth7MHZ = 7000000;
    private int bandwidth8MHZ = 8000000;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.terrestrial_manual_scan_layout, container, false);

        View view_layout = (View) view.findViewById(R.id.view_layout);
        view_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("terrestrial_manual_scan"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        
        //Init vhf/uhf traversal list
        TraversalListView vhfUhf = (TraversalListView) view.findViewById(R.id.vhf_uhf);
        vhfUhf.setTitle(ConfigStringsManager.Companion.getStringById("vhf_uhf_manual_tuning"));
        ArrayList<String> vhfUhfItems = new ArrayList<>();
        vhfUhfItems.add("5");
        vhfUhfItems.add("6");
        vhfUhfItems.add("7");
        vhfUhfItems.add("8");
        vhfUhfItems.add("9");
        vhfUhfItems.add("10");
        vhfUhfItems.add("11");
        vhfUhfItems.add("12");

        for(int i=firstChannelUHF8MHZ;i<lastChannelUHF8MHZ+1;i++)
        {
            vhfUhfItems.add(Integer.toString(i));
        }

        vhfUhf.setItems(vhfUhfItems);

        keyboardLayout = (RelativeLayout) view.findViewById(R.id.keyboard_layout);
        keyboardLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_background")));
        keyboardView = (KeyboardView) view.findViewById(R.id.keyboard);
        keyboardView.setNumericType();

        vhfUhf.setListener(new TraversalListView.TraversalListListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                String freq=getFrequencyToSetTextView(Integer.parseInt(vhfUhfItems.get(itemPosition)));
                frequencyEv.setText(freq);
            }
        });

        //Init frequency edit text
        frequencyEv = (EditTextView) view.findViewById(R.id.frequency_edit_text);
        frequencyEv.setTitle(ConfigStringsManager.Companion.getStringById("frequency_manual_tuning"));
        frequencyEv.setText("177500");

        frequencyEv.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                keyboardLayout.setVisibility(View.VISIBLE);
                keyboardView.setVisibility(View.VISIBLE);
                keyboardView.requestFocus();
                keyboardView.setEditText((EditText) v);

                keyboardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (keyboardView.getVisibility() == View.GONE) {
                            keyboardLayout.setVisibility(View.GONE);
                            v.requestFocus();
                            keyboardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });

        //Init bandwidth traversal list
        TraversalListView bandwidthTl = (TraversalListView) view.findViewById(R.id.bandwidth_tl);
        bandwidthTl.setTitle(ConfigStringsManager.Companion.getStringById("bandwidth_manual_tuning"));
        ArrayList<String> bandwidthItems = new ArrayList<>();
        bandwidthItems.add("5 MHz");
        bandwidthItems.add("7 MHz");
        bandwidthItems.add("8 MHz");
        bandwidthTl.setItems(bandwidthItems);

        //Init signal strength progress bar
        signalStrengthProgress = (ProgressBarView) view.findViewById(R.id.signal_strength_progress);
        signalStrengthProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgress.setProgress(0);
        signalStrengthProgress.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init signal quality progress bar
        signalQualityProgress = (ProgressBarView) view.findViewById(R.id.signal_quality_progress);
        signalQualityProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgress.setProgress(0);
        signalQualityProgress.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_progress")));

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

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    tuneButton.setBackground(selectorDrawable);

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
                ScanHelper.INSTANCE.startTune(Integer.parseInt(frequencyEv.getEditTextString()) * 1000);
            }
        });

        //Scan button click listener
        ReferenceDrawableButton scanButton = (ReferenceDrawableButton) view.findViewById(R.id.scan_button);
        scanButton.setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.setDrawable(null);

        scanButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    scanButton.setBackground(selectorDrawable);

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
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                ArrayList<ScanningFragment.ScanningType> scanningTypes = new ArrayList<>();
                scanningTypes.add(ScanningFragment.ScanningType.TERRESTRIAL);
                ScanningFragment scanningFragment = new ScanningFragment();
                scanningFragment.setScanningTypeList(scanningTypes);
                scanningFragment.setScanningProgressDescription("Frequency: " + frequencyEv.getEditTextString() + " kHz");
                scanningFragment.disableBack();
                scanningFragment.setManualScan();
                ((IwediaSetupActivity) getActivity()).showFragment(scanningFragment);
                ScanHelper.INSTANCE.setActiveFragment(scanningFragment);
                ScanHelper.INSTANCE.startManualScan(Integer.valueOf(frequencyEv.getEditTextString()), getBandWidth(bandwidthTl.getCurrentItem()));
            }
        });

        return view;
    }

    private Integer getBandWidth(int position){
        switch (position) {
            case 0: {
                return 5000;
            }
            case 1: {
                return 7000;
            }
            case 2: {
                return 8000;
            }
        }
        return 8000;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (tuneButton.hasFocus()) {
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new ManualScanFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        scanSignalStrengthChanged(0);
        scanSignalQualityChanged(0);
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

    public String getFrequencyToSetTextView(int channel)
    {
        if(channel<=lastChannelVHF7MHZ)
        {
            int freq=(lowFreqVHF7MHZ+(channel-firstChannelVHF7MHZ)*bandwidth7MHZ)/1000;
            return Integer.toString(freq);
        }
        else
        {
            int freq=(lowFreqUHF8MHZ+(channel-firstChannelUHF8MHZ)*bandwidth8MHZ)/1000;
            return Integer.toString(freq);
        }
    }
}
