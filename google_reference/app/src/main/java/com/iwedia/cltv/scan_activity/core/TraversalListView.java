package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
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


import java.util.ArrayList;

/**
 * Traversal list view
 *
 * @author Dejan Nadj
 */
public class TraversalListView extends RelativeLayout {

    /**
     * Title text view
     */
    private TextView titleView;

    /**
     * Item text view
     */
    private TextView textView;

    /**
     * Arrows
     */
    private ImageView leftArrow;
    private ImageView rightArrow;

    /**
     * Text view typeface
     */
    private Typeface typeface;
    private Typeface typefaceFocus;

    /**
     * Default item value
     */
    private static final String DEFAULT = " - ";

    /**
     * Current item position
     */
    private int currentItem = 0;

    /**
     * Items
     */
    private ArrayList<String> items;

    /**
     * Traversal list listener
     */
    private TraversalListListener listListener;

    /**
     * Traversal list custom values
     */
    private float startValue;
    private float endValue;
    private float step;
    private String valueText;
    private String format;
    private float currentValue = -1;

    /**
     * Constructor
     * @param context
     */
    public TraversalListView(Context context) {
        this(context, null, 0, 0);
    }

    /**
     * Constructor
     * @param context
     * @param attrs
     */
    public TraversalListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    /**
     * Constructor
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public TraversalListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructor
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    public TraversalListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * View initialization
     */
    private void init() {
        View view = inflate(getContext(), R.layout.traversal_view, this);
        titleView = (TextView) view.findViewById(R.id.title_tv);
        titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")));
        titleView.setText(ConfigStringsManager.Companion.getStringById("lnb_type"));
        textView = (TextView) view.findViewById(R.id.text_view);
        leftArrow = (ImageView) view.findViewById(R.id.arrow_left);
        leftArrow.setVisibility(INVISIBLE);
        rightArrow = (ImageView) view.findViewById(R.id.arrow_right);
        rightArrow.setVisibility(INVISIBLE);

        //Init title
        titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleView.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular")));

        //Init item text view
        textView.setClickable(true);
        textView.setFocusable(true);
        textView.setText(DEFAULT);

        typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular"));
        typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium"));
        textView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        textView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
        textView.getBackground().setColorFilter(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")), PorterDuff.Mode.SRC_OVER);
        textView.setTypeface(typeface);
        textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        textView.setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    textView.setTypeface(typefaceFocus);
                    leftArrow.setVisibility(VISIBLE);
                    rightArrow.setVisibility(VISIBLE);
                } else {
                    v.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        v.getBackground().setColorFilter(color_context, PorterDuff.Mode.SRC_OVER);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    textView.setTypeface(typeface);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        textView.setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    leftArrow.setVisibility(INVISIBLE);
                    rightArrow.setVisibility(INVISIBLE);
                }
            }
        });

    }

    /**
     * Set view title
     *
     * @param title
     */
    public void setTitle(String title) {
        titleView.setText(title);
    }

    /**
     * Set list custom values
     *
     * @param startValue    start value
     * @param endValue      end value
     * @param step          step
     * @param valueText     value additional text
     * @param format        text format
     */
    public void setCustomValues(float startValue, float endValue, float step, String valueText, String format) {
        this.startValue = startValue;
        this.endValue = endValue;
        this.step = step;
        this.valueText = valueText;
        this.format = format;
        this.currentValue = startValue;
        textView.setText(String.format(format, currentValue) + " " + valueText);
    }

    /**
     * Set items
     *
     * @param data
     */
    public void setItems(ArrayList<String> data) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.clear();
        if (data != null && !data.isEmpty()) {
            items.addAll(data);
            textView.setText(items.get(0));
            currentItem = 0;
        } else {
            textView.setText(DEFAULT);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (items != null && !items.isEmpty()) {
                    currentItem = currentItem == 0 ? currentItem : currentItem - 1;
                    textView.setText(items.get(currentItem));
                    if (listListener != null) {
                        listListener.onItemSelected(currentItem);
                    }
                } else if (currentValue != -1) {
                    currentValue = currentValue - step >= startValue ? currentValue - step : currentValue;
                    textView.setText(String.format(format, currentValue) + " " + valueText);
                }
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (items != null && !items.isEmpty()) {
                    currentItem = currentItem + 1 >= items.size() ? currentItem : currentItem + 1;
                    textView.setText(items.get(currentItem));
                    if (listListener != null) {
                        listListener.onItemSelected(currentItem);
                    }
                } else if (currentValue != -1) {
                    currentValue = currentValue + step <= endValue ? currentValue + step : currentValue;
                    textView.setText(String.format(format, currentValue) + " " + valueText);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Get current item position
     *
     * @return  current item position
     */
    public int getCurrentItem() {
        return currentItem;
    }

    /**
     * Set traversal list listener
     *
     * @param listener
     */
    public void setListener(TraversalListListener listener) {
        this.listListener = listener;
    }

    /**
     * Set view focusable option
     *
     * @param focusable
     */
    public void setFocusable(boolean focusable) {
        textView.setFocusable(focusable);
        if (focusable) {
            titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            try {
                int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_selector"));
                Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                textView.setBackgroundColor(color_context);
            } catch(Exception ex) {
                Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
            }
            textView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        } else {
            try {
                int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_selector"));
                Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                textView.setBackgroundColor(color_context);
            } catch(Exception ex) {
                Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
            }
            titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            textView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        }
    }

    /**
     * Traversal list listener
     */
    public interface TraversalListListener {
        /**
         * On list item selected
         *
         * @param itemPosition
         */
        void onItemSelected(int itemPosition);
    }
}
