package com.iwedia.cltv.scan_activity.core;

import android.graphics.Color;
import android.view.View;


import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.config.ConfigColorManager;;

/**
 * Simple check list view holder
 *
 * @author Dragan Krnjaic
 */
public class SimpleCheckListViewHolder extends RecyclerView.ViewHolder {

    /**
     * Root item view
     */
    private View rootView;

    /**
     * Text view
     */
    private ReferenceDrawableButton checkBoxDrawableButton;

    /**
     * Constructor
     *
     * @param itemView itemView
     */
    public SimpleCheckListViewHolder(View itemView) {
        super(itemView);
        rootView = itemView;
        checkBoxDrawableButton = itemView.findViewById(R.id.check_box);
        checkBoxDrawableButton.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
    }

    /**
     * Get root view
     *
     * @return rootView
     */
    public View getRootView() {
        return rootView;
    }

    /**
     * Get text view
     *
     * @return text view
     */
    public ReferenceDrawableButton getCheckBoxDrawableButton() {
        return checkBoxDrawableButton;
    }
}
