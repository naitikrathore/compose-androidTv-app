package com.iwedia.cltv.scan_activity.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;

/**
 * Simple check box
 *
 * @author Dragan Krnjaic
 */
@SuppressLint("AppCompatCustomView")
public class SimpleCheckBox extends CheckBox {

    /**
     * Text view typeface
     */
    private Typeface typeface;

    private Typeface typefaceFocus;

    public SimpleCheckBox(Context context, String textLabel) {
        this(context, null, 0);
    }

    public SimpleCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * View initialization
     */
    private void init() {

        typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular"));
        setTypeface(typeface);
        typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_bold"));
        setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        setTypeface(typeface);
        setButtonDrawable(getResources().getDrawable(R.drawable.checkbox_selector, null));
        setClickable(true);

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
     * Set check box text typeface
     *
     * @param defaultTypeface   default typeface
     */
    public void setTypeface(String defaultTypeface) {
        typeface = Typeface.createFromAsset(getContext().getAssets(),defaultTypeface);
    }

    @Override
    public void setOnClickListener(final View.OnClickListener l) {
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                l.onClick(v);
            }
        });
    }
}
