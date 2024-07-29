package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;

/**
 * Simple text view
 *
 * @author Dejan Nadj
 */
public class SimpleTextView extends AppCompatTextView {

    /**
     * Text view typeface
     */
    private Typeface typeface;
    private Typeface typefaceFocus;

    public SimpleTextView(Context context) {
        this(context, null, 0);
    }

    public SimpleTextView(Context context,  AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleTextView(Context context,  AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * View initialization
     */
    private void init() {
        setClickable(true);
        setFocusable(true);
//        typeface = Typeface.createFromAsset(getContext().getAssets(),"fonts/montserrat_regular.ttf");
//        typefaceFocus = Typeface.createFromAsset(getContext().getAssets(),"fonts/montserrat_semibold.ttf");
        typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular"));
        typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium"));

        setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        setTypeface(typeface);
        setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    setTypeface(typefaceFocus);
                } else {
                    v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                    setTypeface(typeface);
                    setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                }
            }
        });
    }

    /**
     * Set text view typeface
     *
     * @param defaultTypeface   default typeface
     * @param focusTypeface     focused state typeface
     */
    public void setTypeface(String defaultTypeface, String focusTypeface) {
        typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular"));
        typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium"));
    }

    @Override
    public void setFocusable(boolean focusable) {
        super.setFocusable(focusable);
        if (focusable) {
            setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        } else {
            setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        }
    }
}
