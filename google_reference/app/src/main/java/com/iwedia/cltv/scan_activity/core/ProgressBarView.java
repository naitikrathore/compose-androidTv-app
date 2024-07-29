package com.iwedia.cltv.scan_activity.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;


/**
 * Progress bar view
 *
 * @author Dejan Nadj
 */
public class ProgressBarView extends RelativeLayout {
    /**
     * Title text view
     */
    private TextView titleView;
    /**
     * Progress value text
     */
    private TextView textView;
    /**
     * Progress bar
     */
    private ProgressBar progressBar;

    /**
     * Constructor
     *
     * @param context
     */
    public ProgressBarView(Context context) {
        this(context, null, 0, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public ProgressBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public ProgressBarView(Context context, AttributeSet attrs, int defStyleAttr) {
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
    public ProgressBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * View initialization
     */
    @SuppressLint("WrongConstant")
    private void init() {
        View view = inflate(getContext(), R.layout.progress_bar_layout, this);
        titleView = (TextView) view.findViewById(R.id.title_tv);
        titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")));
        titleView.setText(ConfigStringsManager.Companion.getStringById("text_signal_strength"));
        textView = (TextView) view.findViewById(R.id.progress_text);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        titleView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleView.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular")));
        titleView.setTextSize(13);

        textView.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        textView.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
        textView.setTextSize(13);


        progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor(ConfigColorManager.Companion.getColor("color_progress"))));
        progressBar.setScrollBarStyle(android.R.attr.progressBarStyleHorizontal);
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor(ConfigColorManager.Companion.getColor("color_text_description"))));
        setProgress(0);
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
     * Set progress value
     *
     * @param progress
     */
    public void setProgress(int progress) {
        progressBar.setProgress(progress);

        String progressText = String.valueOf(progress) + "%";
        textView.setText(progressText);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setTitleVisibility(int visibility) {
        titleView.setVisibility(visibility);
    }
}
