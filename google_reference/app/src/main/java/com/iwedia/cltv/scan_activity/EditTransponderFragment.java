package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.scan_activity.entities.SatelliteTransponder;
import com.iwedia.cltv.utils.Utils;


import java.util.ArrayList;

/**
 * Add/Edit Transponders fragment
 *
 * @author Dejan Nadj
 */
public class EditTransponderFragment extends GenericFragment {

    /**
     * Is add type of fragment
     */
    private boolean isAddType;

    /**
     * Fragment title
     */
    private TextView titleTv;

    /**
     * Scan button
     */
    private SimpleTextView scanButton;

    /**
     * Keyboard layout
     */
    private RelativeLayout keyboardLayout;

    /**
     * Keyboard view
     */
    private KeyboardView keyboardView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.edit_transponder_layout, container, false);
        View view_layout = (View) view.findViewById(R.id.view_layout);
        view_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        //Init title
        titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("edit_transponder"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        SatelliteTransponder satelliteTransponder = SatelliteHelperClass.getSatelliteTransponder();
        if (isAddType) {
            titleTv.setText(ConfigStringsManager.Companion.getStringById("add_transponder"));
        } else {
            titleTv.setText(ConfigStringsManager.Companion.getStringById("edit_transponder") + " (" + satelliteTransponder.toString() + ")");
        }

        //Init numeric keyboard
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
        };

        //Init frequency edit text
        final EditTextView freqEv = (EditTextView) view.findViewById(R.id.frequency_edit_text);
        freqEv.setTitle(ConfigStringsManager.Companion.getStringById("frequency_diseqc"));
        if (isAddType) {
            freqEv.setText("");
            freqEv.getEditText().setHint(ConfigStringsManager.Companion.getStringById("insert_frequency"));
        } else {
            freqEv.setText(String.valueOf(satelliteTransponder.getFrequency()));
        }
        freqEv.getEditText().setOnClickListener(clickListener);

        //Init symbol rate edit text
        final EditTextView symbolRateEv = (EditTextView) view.findViewById(R.id.symbol_rate_edit_text);
        symbolRateEv.setTitle(ConfigStringsManager.Companion.getStringById("symbol_rate_diseqc"));
        if (isAddType) {
            symbolRateEv.setText("");
            symbolRateEv.getEditText().setHint(ConfigStringsManager.Companion.getStringById("insert_symbol_rate"));
        } else {
            symbolRateEv.setText(String.valueOf(satelliteTransponder.getSymbolRate()));
        }
        symbolRateEv.getEditText().setOnClickListener(clickListener);

        //Init polarization traversal list
        TraversalListView polarizationTl = (TraversalListView) view.findViewById(R.id.polarization_tl);
        polarizationTl.setTitle(ConfigStringsManager.Companion.getStringById("polarization"));
        ArrayList<String> items = new ArrayList<>();
        if (isAddType) {
            items.add(SatelliteTransponder.Polarization.HORIZONTAL.getText());
            items.add(SatelliteTransponder.Polarization.VERTICAL.getText());
        } else {
            items.add(satelliteTransponder.getPolarization().getText());
            if (satelliteTransponder.getPolarization() == SatelliteTransponder.Polarization.VERTICAL) {
                items.add(ConfigStringsManager.Companion.getStringById("polar_horizontal"));
            } else {
                items.add(ConfigStringsManager.Companion.getStringById("polar_vertical"));
            }
        }
        polarizationTl.setItems(items);

        //Init tyning type traversal list
        TraversalListView tyningTypeTl = (TraversalListView) view.findViewById(R.id.tyning_type_tl);
        tyningTypeTl.setTitle(ConfigStringsManager.Companion.getStringById("tyning_type"));
        ArrayList<String> itemsTyning = new ArrayList<>();
        if (isAddType) {
            itemsTyning.add(SatelliteTransponder.TyningType.DVB_S.getText());
            itemsTyning.add(SatelliteTransponder.TyningType.DVB_S2.getText());
        } else {
            itemsTyning.add(satelliteTransponder.getTyningType().getText());
            if (satelliteTransponder.getTyningType() == SatelliteTransponder.TyningType.DVB_S) {
                itemsTyning.add(SatelliteTransponder.TyningType.DVB_S2.getText());
            } else {
                itemsTyning.add(SatelliteTransponder.TyningType.DVB_S.getText());
            }
        }
        tyningTypeTl.setItems(itemsTyning);

        //Init signal strength progress bar
        ProgressBarView signalStrengthProgress = (ProgressBarView) view.findViewById(R.id.signal_strength_progress);
        signalStrengthProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgress.setProgress(80);

        //Init signal quality progress bar
        ProgressBarView signalQualityProgress = (ProgressBarView) view.findViewById(R.id.signal_quality_progress);
        signalQualityProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgress.setProgress(90);

        //Scan button click listener
        scanButton = (SimpleTextView) view.findViewById(R.id.scan_button);
        scanButton.setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                GenericFragment fragment = new SatelliteScanOptionsFragment();
                fragment.setPreviousFragment(EditTransponderFragment.this);
                ((IwediaSetupActivity) getActivity()).hideFragment(EditTransponderFragment.this, fragment);
            }

        });

        //Save button click listener
        SimpleTextView saveButton = (SimpleTextView) view.findViewById(R.id.save_button);
        saveButton.setText(ConfigStringsManager.Companion.getStringById("save"));
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                GenericFragment fragment = new TranspondersFragment();
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
            }

        });
        return view;
    }

    /**
     * Set add type fragment
     */
    public void setAddType() {
        isAddType = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        scanButton.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new TranspondersFragment();
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
