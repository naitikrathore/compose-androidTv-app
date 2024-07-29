package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;

/**
 * Tuning result info layout
 *
 * @author Dejan Nadj
 */
public class ScanResultInfoView extends RelativeLayout {

    /**
     * Programmes number text view
     */
    private TextView programmesNumber;
    /**
     * Radio number text view
     */
    private TextView radioNumber;

    /**
     * Constructor
     *
     * @param context
     */
    public ScanResultInfoView(Context context) {
        this(context, null, 0, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public ScanResultInfoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public ScanResultInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    public ScanResultInfoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * View initialization
     */
    private void init() {
        View view = inflate(getContext(), R.layout.scan_result_info_view, this);
        programmesNumber = (TextView) view.findViewById(R.id.programme_number_tv);
        programmesNumber.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        programmesNumber.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular")));
        TextView programmesFound = (TextView) view.findViewById(R.id.programme_found_tv);
        programmesFound.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        programmesFound.setText(ConfigStringsManager.Companion.getStringById("tv_programmes_found"));
        programmesFound.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));

        radioNumber = (TextView) view.findViewById(R.id.radio_number_tv);
        radioNumber.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        radioNumber.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));

        TextView radioFound = (TextView) view.findViewById(R.id.radio_found_tv);
        radioFound.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        radioFound.setText(ConfigStringsManager.Companion.getStringById("radio_programmes_found"));
        radioFound.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
    }

    /**
     * Set programmes number
     * @param number
     */
    public void setProgrammesNumber(int number) {
        programmesNumber.setText(String.valueOf(number));
    }

    /**
     * Set radio number
     * @param number
     */
    public void setRadioNumber(int number) {
        radioNumber.setText(String.valueOf(number));
    }
}
