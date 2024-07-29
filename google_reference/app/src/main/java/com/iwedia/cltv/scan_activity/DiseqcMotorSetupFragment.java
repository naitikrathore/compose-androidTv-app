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

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

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
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

/**
 * DiSEqC Motor Setup fragment
 *
 * @author Dejan Nadj
 */
public class DiseqcMotorSetupFragment extends GenericFragment {

    /**
     * Scroll view
     */
    private NestedScrollView scrollView;

    /**
     * Position edit view
     */
    private EditTextView positionEv;

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

        final View view = inflater.inflate(R.layout.diseqc_motor_setup_layout, container, false);
        View view_layout = (View) view.findViewById(R.id.view_layout);
        view_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("diseqc_motor_setup"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init moving type traversal list
        TraversalListView movingTypeTl = (TraversalListView) view.findViewById(R.id.moving_type_tl);
        movingTypeTl.setTitle(ConfigStringsManager.Companion.getStringById("moving_type"));
        ArrayList<String> movingTypeItems = new ArrayList<>();
        movingTypeItems.add(ConfigStringsManager.Companion.getStringById("installer"));
        movingTypeItems.add(ConfigStringsManager.Companion.getStringById("advanced"));
        movingTypeTl.setItems(movingTypeItems);
        movingTypeTl.requestFocus();

        //Init limit traversal list
        final TraversalListView limitTl = (TraversalListView) view.findViewById(R.id.limit_tl);
        limitTl.setTitle(ConfigStringsManager.Companion.getStringById("limit"));
        ArrayList<String> limitItems = new ArrayList<>();
        limitItems.add(ConfigStringsManager.Companion.getStringById("disable"));
        limitItems.add(ConfigStringsManager.Companion.getStringById("enable"));
        limitTl.setItems(limitItems);
        limitTl.setFocusable(false);

        //Init set limit traversal list
        final TraversalListView setLimitTl = (TraversalListView) view.findViewById(R.id.set_limit_tl);
        setLimitTl.setTitle(ConfigStringsManager.Companion.getStringById("set_limit"));
        final ArrayList<String> setLimitItems = new ArrayList<>();
        setLimitItems.add("West Limit");
        setLimitItems.add("East Limit");
        setLimitTl.setItems(setLimitItems);
        setLimitTl.setFocusable(false);

        //Recalculate button
        final SimpleTextView recalculateButton = (SimpleTextView) view.findViewById(R.id.recalculate_button);
        recalculateButton.setText(ConfigStringsManager.Companion.getStringById("recalculate"));
        recalculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.Companion.clickAnimation(view);
            }
        });

        //Reset position button
        final SimpleTextView resetPositionButton = (SimpleTextView) view.findViewById(R.id.reset_position_button);
        resetPositionButton.setText(ConfigStringsManager.Companion.getStringById("reset_position"));
        resetPositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.Companion.clickAnimation(view);
            }
        });

        recalculateButton.setFocusable(false);
        resetPositionButton.setFocusable(false);
        movingTypeTl.setListener(new TraversalListView.TraversalListListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                if (itemPosition == 0) {
                    limitTl.setFocusable(false);
                    setLimitTl.setFocusable(false);
                    recalculateButton.setFocusable(false);
                    resetPositionButton.setFocusable(false);
                } else {
                    recalculateButton.setFocusable(true);
                    resetPositionButton.setFocusable(true);
                    limitTl.setFocusable(true);
                    if (limitTl.getCurrentItem() == 0) {
                        setLimitTl.setFocusable(false);
                    } else {
                        setLimitTl.setFocusable(true);
                    }
                }
            }
        });

        limitTl.setListener(new TraversalListView.TraversalListListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                if (itemPosition == 0) {
                    setLimitTl.setFocusable(false);
                } else {
                    setLimitTl.setFocusable(true);
                }
            }
        });

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
                if (v == positionEv.getEditText()) {
                    if (scrollView.getScrollY() < 200) {
                        scrollView.scrollBy(0, 350);
                    }
                } else {
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

        //Init position edit text
        positionEv = (EditTextView) view.findViewById(R.id.position_edit_text);
        positionEv.setTitle(ConfigStringsManager.Companion.getStringById("position"));
        positionEv.setText("00");
        positionEv.getEditText().setOnClickListener(clickListener);

        //Init drive E/W traversal list
        TraversalListView driveTl = (TraversalListView) view.findViewById(R.id.drive_tl);
        driveTl.setTitle(ConfigStringsManager.Companion.getStringById("drive_ew"));
        ArrayList<String> driveItems = new ArrayList<>();
        driveItems.add(ConfigStringsManager.Companion.getStringById("east"));
        driveItems.add(ConfigStringsManager.Companion.getStringById("west"));
        driveTl.setItems(driveItems);

        //Init transponders traversal list
        TraversalListView transpondersTl = (TraversalListView) view.findViewById(R.id.transponder_tl);
        transpondersTl.setTitle(ConfigStringsManager.Companion.getStringById("transponders"));
        ArrayList<String> transpondersItems = new ArrayList<>();
        transpondersItems.add("10739,V,22000");
        transpondersItems.add("10740,V,22100");
        transpondersItems.add("10750,V,22200");
        transpondersTl.setItems(transpondersItems);

        //Init frequency edit text
        final EditTextView freqEv = (EditTextView) view.findViewById(R.id.frequency_edit_text);
        freqEv.setTitle(ConfigStringsManager.Companion.getStringById("frequency_diseqc"));
        freqEv.setText("10729");
        freqEv.getEditText().setOnClickListener(clickListener);

        //Init symbol rate edit text
        final EditTextView symbolRateEv = (EditTextView) view.findViewById(R.id.symbol_rate_edit_text);
        symbolRateEv.setTitle(ConfigStringsManager.Companion.getStringById("symbol_rate_diseqc"));
        symbolRateEv.setText("22000");
        symbolRateEv.getEditText().setOnClickListener(clickListener);

        //Init signal strength progress bar
        ProgressBarView signalStrengthProgress = (ProgressBarView) view.findViewById(R.id.signal_strength_progress);
        signalStrengthProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgress.setProgress(80);

        //Init signal quality progress bar
        ProgressBarView signalQualityProgress = (ProgressBarView) view.findViewById(R.id.signal_quality_progress);
        signalQualityProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgress.setProgress(90);

        //Save button
        final SimpleTextView saveButton = (SimpleTextView) view.findViewById(R.id.save_button);
        saveButton.setText(ConfigStringsManager.Companion.getStringById("save"));
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
