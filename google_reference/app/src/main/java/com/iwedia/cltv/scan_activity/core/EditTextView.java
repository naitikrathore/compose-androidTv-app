package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.platform.model.Constants;

/**
 * Edit text view
 *
 * @author Dejan Nadj
 */
public class EditTextView extends RelativeLayout {
    /**
     * Edit text title
     */
    private TextView titleView;

    /**
     * Edit text
     */
    private EditText editText;

    /**
     * Bottom line
     */
    private View bottomLine;

    /**
     * Constructor
     * @param context
     */
    public EditTextView(Context context) {
        this(context, null, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public EditTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public EditTextView(Context context, AttributeSet attrs, int defStyleAttr) {
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
    public EditTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Layout initialization
     */
    private void init() {
        View view = inflate(getContext(), R.layout.edit_text_layout, this);
        titleView = (TextView) view.findViewById(R.id.title_tv);
        titleView.setText(ConfigStringsManager.Companion.getStringById("lnb_type"));
        editText = (EditText) view.findViewById(R.id.edit_text);

        //Init title view
        titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleView.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular")));

        //Init edit text
        editText.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
        editText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
        editText.getBackground().setColorFilter(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")), PorterDuff.Mode.SRC_OVER);
        editText.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        bottomLine = view.findViewById(R.id.bottom_line);
        bottomLine.setVisibility(GONE);
        bottomLine.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
//        bottomLine.setBackgroundColor(getResources().getColor(R.color.text_color, null));

        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
//                    bottomLine.setBackgroundColor(getResources().getColor(R.color.yellow_orange, null));
                    editText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        editText.setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                } else {
//                    bottomLine.setBackgroundColor(getResources().getColor(R.color.text_color, null));
                    editText.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        editText.getBackground().setColorFilter(color_context, PorterDuff.Mode.SRC_OVER);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        editText.setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                }
            }
        });
    }

    /**
     * Set title text
     *
     * @param title
     */
    public void setTitle(String title) {
        titleView.setText(title);
    }

    /**
     * Set edit text content
     *
     * @param text
     */
    public void setText(String text) {
        editText.setText(text);
    }

    /**
     * Get edit text content
     *
     * @return  string value of edit text content
     */
    public String getEditTextString() {
        return editText.getText().toString();
    }

    /**
     * Get edit text
     * @return
     */
    public EditText getEditText() {
        return editText;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                return false;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                return false;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (editText.getSelectionStart() == editText.getText().length()) {
                    return false;
                }
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (editText.getSelectionStart() == 0) {
                    return false;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Set focusable
     *
     * @param isFocusable
     */
    public void setFocusable(boolean isFocusable) {
        editText.setFocusable(isFocusable);
        if (isFocusable) {
            titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            editText.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            bottomLine.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        } else {
            titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            editText.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            bottomLine.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        }
    }
}
