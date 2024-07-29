package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
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
import androidx.core.widget.NestedScrollView;

import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
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
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.utils.Utils;


import java.util.ArrayList;

/**
 * Antenna config fragment
 *
 * @author Dejan Nadj
 */
public class AntennaConfigFragment extends GenericFragment {

    /**
     * Scroll view
     */
    private NestedScrollView scrollView;

    /**
     * Motor setup button
     */
    private SimpleTextView motorSetup;

    /**
     * Center frequency edit view
     */
    private EditTextView centerFrequencyEv;

    /**
     * Keyboard layout
     */
    private RelativeLayout keyboardLayout;

    /**
     * Keyboard view
     */
    private KeyboardView keyboardView;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.antenna_config_layout, container, false);
        View view_layout = (View) view.findViewById(R.id.view);
        view_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setText(ConfigStringsManager.Companion.getStringById("antenna_config") + " (" + SatelliteHelperClass.getSatelliteName() + ")");
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init LNB type traversal list
        TraversalListView lnbTypeTl = (TraversalListView) view.findViewById(R.id.lnb_type_tl);
        lnbTypeTl.setTitle(ConfigStringsManager.Companion.getStringById("lnb_type"));
        ArrayList<String> lnbTypeItems = new ArrayList<>();
        lnbTypeItems.add("Universal");
        lnbTypeItems.add("C-band");
        lnbTypeItems.add("Ku-band");
        lnbTypeTl.setItems(lnbTypeItems);
        lnbTypeTl.requestFocus();

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
                if (v == centerFrequencyEv.getEditText()) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
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

        //Init low frequency range edit text
        final EditTextView lowFrequencyEv = (EditTextView) view.findViewById(R.id.low_frequency_edit_text);
        lowFrequencyEv.setTitle(ConfigStringsManager.Companion.getStringById("low_frequency_range"));
        lowFrequencyEv.setText("09750");
        lowFrequencyEv.getEditText().setOnClickListener(clickListener);

        //Init high frequency edit text
        final EditTextView highFrequencyEv = (EditTextView) view.findViewById(R.id.high_frequency_edit_text);
        highFrequencyEv.setTitle(ConfigStringsManager.Companion.getStringById("high_frequency_range"));
        highFrequencyEv.setText("10600");
        highFrequencyEv.getEditText().setOnClickListener(clickListener);

        lowFrequencyEv.setFocusable(false);
        highFrequencyEv.setFocusable(false);
        lnbTypeTl.setListener(new TraversalListView.TraversalListListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                if (itemPosition == 0) {
                    lowFrequencyEv.setFocusable(false);
                    highFrequencyEv.setFocusable(false);
                } else {
                    lowFrequencyEv.setFocusable(true);
                    highFrequencyEv.setFocusable(true);
                }
            }
        });

        //Init frequency traversal list
        TraversalListView freqTl = (TraversalListView) view.findViewById(R.id.frequency_tl);
        freqTl.setTitle("22 kHz");
        ArrayList<String> freqItems = new ArrayList<>();
        freqItems.add("AUTO");
        freqItems.add("10 kHz");
        freqItems.add("15 kHz");
        freqItems.add("20 kHz");
        freqTl.setItems(freqItems);

        //Init diSEqC traversal list
        TraversalListView diseqcTl = (TraversalListView) view.findViewById(R.id.diseqc_tl);
        diseqcTl.setTitle(ConfigStringsManager.Companion.getStringById("diseqc"));
        ArrayList<String> diseqcItems = new ArrayList<>();
        diseqcItems.add(ConfigStringsManager.Companion.getStringById("deactivate"));
        diseqcItems.add(ConfigStringsManager.Companion.getStringById("activate"));
        diseqcTl.setItems(diseqcItems);

        //Init tone burst traversal list
        TraversalListView toneBurstTl = (TraversalListView) view.findViewById(R.id.tone_burst_tl);
        toneBurstTl.setTitle(ConfigStringsManager.Companion.getStringById("tone_burst"));
        ArrayList<String> toneBurstItems = new ArrayList<>();
        toneBurstItems.add(ConfigStringsManager.Companion.getStringById("activate"));
        toneBurstItems.add(ConfigStringsManager.Companion.getStringById("deactivate"));
        toneBurstTl.setItems(toneBurstItems);

        //Init positioner traversal list
        final TraversalListView positionerTl = (TraversalListView) view.findViewById(R.id.positioner_tl);
        positionerTl.setTitle(ConfigStringsManager.Companion.getStringById("positioner"));
        ArrayList<String> positionserItems = new ArrayList<>();
        positionserItems.add(ConfigStringsManager.Companion.getStringById("none"));
        positionserItems.add(ConfigStringsManager.Companion.getStringById("usals"));
        positionserItems.add(ConfigStringsManager.Companion.getStringById("diseqc"));
        positionerTl.setItems(positionserItems);

        //Motor setup button
        motorSetup = (SimpleTextView) view.findViewById(R.id.motor_setup_button);
        motorSetup.setText(ConfigStringsManager.Companion.getStringById("motor_setup"));
        motorSetup.setFocusable(false);
        positionerTl.setListener(new TraversalListView.TraversalListListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                if (itemPosition == 0) {
                    motorSetup.setFocusable(false);
                } else {
                    motorSetup.setFocusable(true);
                }
            }
        });
        motorSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                if (positionerTl.getCurrentItem() == 1) {
                    UsalsMotorSetupFragment usalsMotorSetupFragment = new UsalsMotorSetupFragment();
                    usalsMotorSetupFragment.setPreviousFragment(AntennaConfigFragment.this);
                    ((IwediaSetupActivity) getActivity()).hideFragment(AntennaConfigFragment.this, usalsMotorSetupFragment);
                } else {
                    DiseqcMotorSetupFragment diseqcMotorSetupFragment = new DiseqcMotorSetupFragment();
                    diseqcMotorSetupFragment.setPreviousFragment(AntennaConfigFragment.this);
                    ((IwediaSetupActivity) getActivity()).hideFragment(AntennaConfigFragment.this, diseqcMotorSetupFragment);
                }
            }
        });

        //Init LNB volts traversal list
        TraversalListView lnbVoltsTl = (TraversalListView) view.findViewById(R.id.lnb_volts_tl);
        lnbVoltsTl.setTitle(ConfigStringsManager.Companion.getStringById("lnb_volts"));
        ArrayList<String> lnbVoltsItems = new ArrayList<>();
        lnbVoltsItems.add(ConfigStringsManager.Companion.getStringById("off"));
        lnbVoltsItems.add(ConfigStringsManager.Companion.getStringById("on"));
        lnbVoltsTl.setItems(lnbVoltsItems);

        //Init unicable traversal list
        TraversalListView unicableTl = (TraversalListView) view.findViewById(R.id.unicable_tl);
        unicableTl.setTitle(ConfigStringsManager.Companion.getStringById("unicable"));
        ArrayList<String> unicableItems = new ArrayList<>();
        unicableItems.add(ConfigStringsManager.Companion.getStringById("deactivate"));
        unicableItems.add(ConfigStringsManager.Companion.getStringById("activate"));
        unicableTl.setItems(unicableItems);

        //Init center frequency edit text
        centerFrequencyEv = (EditTextView) view.findViewById(R.id.center_frequency_edit_text);
        centerFrequencyEv.setTitle(ConfigStringsManager.Companion.getStringById("center_frequency"));
        centerFrequencyEv.setText("0000");
        centerFrequencyEv.getEditText().setOnClickListener(clickListener);

        //Init port traversal list
        final TraversalListView portTl = (TraversalListView) view.findViewById(R.id.port_tl);
        portTl.setTitle(ConfigStringsManager.Companion.getStringById("port"));
        ArrayList<String> portItems = new ArrayList<>();
        portItems.add("A");
        portItems.add("B");
        portTl.setItems(portItems);

        centerFrequencyEv.setFocusable(false);
        portTl.setFocusable(false);
        unicableTl.setListener(new TraversalListView.TraversalListListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                if (itemPosition == 0) {
                    centerFrequencyEv.setFocusable(false);
                    portTl.setFocusable(false);
                } else {
                    centerFrequencyEv.setFocusable(true);
                    portTl.setFocusable(true);
                }
            }
        });

        //Save button
        final ReferenceDrawableButton saveButton = (ReferenceDrawableButton) view.findViewById(R.id.save_button);

        saveButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("save"));
        saveButton.setDrawable(null);

        saveButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    saveButton.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.focus_shape));
                    saveButton.getTextView().setTextSize(15);
                    saveButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    saveButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    saveButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    saveButton.getBackground().setColorFilter(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")), PorterDuff.Mode.SRC_OVER);
                    saveButton.getTextView().setTextSize(15);
                    saveButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    saveButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Utils.Companion.clickAnimation(view);
            }
        });

        scrollView = (NestedScrollView) view.findViewById(R.id.scrollView);
        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            boolean isBottomReached = false;
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int diff = (saveButton.getBottom() - (scrollView.getHeight() + scrollY));
                if (diff <= 0) {
                    final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
                    int leftMargin = (int) getResources().getDimension(R.dimen.custom_dim_57);
                    int topMargin = (int) getResources().getDimension(R.dimen.custom_dim_48);
                    int bottomMargin = (int) getResources().getDimension(R.dimen.custom_dim_41);
                    params.setMargins(leftMargin, topMargin, 0, bottomMargin);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.setLayoutParams(params);
                            scrollView.invalidate();
                        }
                    });
                    isBottomReached = true;
                } else if (isBottomReached){
                    final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
                    int leftMargin = (int) getResources().getDimension(R.dimen.custom_dim_57);
                    int topMargin = (int) getResources().getDimension(R.dimen.custom_dim_48);
                    params.setMargins(leftMargin, topMargin, 0, 0);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.setLayoutParams(params);
                            scrollView.invalidate();
                        }
                    });
                    isBottomReached = false;
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        motorSetup.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    GenericFragment fragment = new SatelliteOptionsFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(MainFragment.SELECTED_OPTION, 2);
                    fragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
