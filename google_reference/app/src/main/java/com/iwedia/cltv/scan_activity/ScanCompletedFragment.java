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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.scan_activity.core.ScanDetailsView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.ScanDetailsView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.entities.ScanDetails;
import com.iwedia.cltv.utils.Utils;


import java.util.ArrayList;


/**
 * Scan completed fragment
 *
 * @author Dragan Krnjaic
 */
public class ScanCompletedFragment extends GenericFragment {

    /**
     * Done button
     */
    private ReferenceDrawableButton doneBtn;

    /**
     * Retry button
     */
    private ReferenceDrawableButton retryBtn;

    /**
     * Scan details list
     */
    private ArrayList<ScanDetails> scanDetails = new ArrayList<>();

    /**
     * Is manual scan completed
     */
    private boolean isManualScan;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.scan_completed_fragment, container, false);

        LinearLayout detailsListLayout = (LinearLayout) view.findViewById(R.id.details_list_layout);
        for (int i = 0; i < scanDetails.size(); i++) {
            ScanDetailsView scanDetailsView = new ScanDetailsView(getContext(), scanDetails.get(i));
            detailsListLayout.addView(scanDetailsView);
        }

        SimpleTextView fragmentTitle = view.findViewById(R.id.fragment_title);
        fragmentTitle.setText(ConfigStringsManager.Companion.getStringById("scan_completed"));
        fragmentTitle.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        doneBtn = view.findViewById(R.id.done_button);

        doneBtn.getTextView().setText(ConfigStringsManager.Companion.getStringById("done"));
        doneBtn.setDrawable(null);
        doneBtn.post(new Runnable() {
            @Override
            public void run() {
                doneBtn.requestFocus();
            }
        });

        doneBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    doneBtn.setBackground(selectorDrawable);

                    doneBtn.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        doneBtn.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    doneBtn.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    doneBtn.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        doneBtn.getBackground().setColorFilter(color_context, PorterDuff.Mode.SRC_OVER);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    doneBtn.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        doneBtn.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    doneBtn.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });


        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.Companion.clickAnimation(doneBtn);
                //Show main screen
                GenericFragment fragment = new MainFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).finishScan();
            }
        });

        retryBtn = view.findViewById(R.id.retry_button);
        retryBtn.setText(ConfigStringsManager.Companion.getStringById("retry"));

        retryBtn.getTextView().setText(ConfigStringsManager.Companion.getStringById("retry"));
        retryBtn.setDrawable(null);

        retryBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    retryBtn.setBackground(selectorDrawable);

                    retryBtn.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        retryBtn.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    retryBtn.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    retryBtn.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        retryBtn.getBackground().setColorFilter(color_context, PorterDuff.Mode.SRC_OVER);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    retryBtn.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        retryBtn.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    retryBtn.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });


        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.Companion.clickAnimation(retryBtn);
                if (!isManualScan) {
                    //Show auto scan screen
                    GenericFragment fragment = new AutoScanFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                    fragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                } else {
                    GenericFragment fragment = new ManualScanFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                    fragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                }
            }
        });

        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (!isManualScan) {
                    GenericFragment fragment = new AutoScanFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                    fragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                } else {
                    GenericFragment fragment = new ManualScanFragment();
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

    /**
     * Set scan details list
     *
     * @param list  scan details list
     */
    public void setScanDetails(ArrayList<ScanDetails> list) {
        scanDetails = new ArrayList<>();
        scanDetails.addAll(list);
    }

    /**
     * Set is manual scan
     */
    public void setManualScan() {
        isManualScan = true;
    }
}
