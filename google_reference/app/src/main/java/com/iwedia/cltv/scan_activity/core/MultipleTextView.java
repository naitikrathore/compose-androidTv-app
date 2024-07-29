package com.iwedia.cltv.scan_activity.core;


import android.content.Context;

import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;

/**
 * Multiple text view
 *
 * @author Dragan Krnjaic
 */
public class MultipleTextView extends RelativeLayout {

    /**
     * Left text view
     */
    private TextView leftTextView;

    /**
     * Right text view
     */
    private TextView rightTextView;

    /**
     *
     * @param context
     */
    public MultipleTextView(Context context) {
        super(context);
    }

    public MultipleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Setup view
     */
    private void init () {
        View view = inflate(getContext(), R.layout.multiple_textview_layout, this);

        leftTextView = (TextView) view.findViewById(R.id.left_text);
        rightTextView = (TextView) view.findViewById(R.id.right_text);

        //set left text font and color
        leftTextView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        leftTextView.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular")));

        //set right text font and color
        rightTextView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        rightTextView.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
    }

    /**
     * Set text size
     *
     * @param leftTextSize left text size
     * @param rightTextSize right text size
     */
    public void setTextSize(float leftTextSize, float rightTextSize) {
        leftTextView.setTextSize(leftTextSize);
        rightTextView.setTextSize(rightTextSize);
    }

    /**
     * Set text color
     *
     * @param context context
     * @param leftTextColor text color id from resources
     * @param rightTextColor text color id from resources
     */
    public void setTextColor(Context context, int leftTextColor, int rightTextColor) {
        leftTextView.setTextColor(ContextCompat.getColor(context, leftTextColor));
        rightTextView.setTextColor(ContextCompat.getColor(context, rightTextColor));
    }

    /**
     * Set text to left text view
     *
     * @param leftTextLabel text
     */
    public void setLeftText (String leftTextLabel) {
        leftTextView.setText(leftTextLabel);
    }

    /**
     * Set text to right text view
     *
     * @param rightTextLabel text
     */
    public void setRightText (String rightTextLabel) {
        rightTextView.setText(rightTextLabel);
    }
}
