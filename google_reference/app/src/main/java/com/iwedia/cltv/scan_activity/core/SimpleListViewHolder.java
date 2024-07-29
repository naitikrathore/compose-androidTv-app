package com.iwedia.cltv.scan_activity.core;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceDrawableButton;

/**
 * Simple list view holder
 *
 * @author Dejan Nadj
 */
public class SimpleListViewHolder extends RecyclerView.ViewHolder {

    /**
     * Root item view
     */
    private View rootView;

    /**
     * Text view
     */
    private ReferenceDrawableButton textView;

    /**
     * Constructor
     *
     * @param itemView itemView
     */
    public SimpleListViewHolder(View itemView) {
        super(itemView);
        rootView = itemView;
        textView = itemView.findViewById(R.id.text_view);
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
    public ReferenceDrawableButton getOptionButton() {
        return textView;
    }
}
