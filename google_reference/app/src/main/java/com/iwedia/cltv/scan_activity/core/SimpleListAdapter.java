package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.platform.model.Constants;

import java.util.ArrayList;

/**
 * Simple list with text items
 *
 * @author Dejan Nadj
 */
public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListViewHolder> {

    /**
     * Is next focus down enabled
     */
    private boolean enableNextFocusDown;

    /**
     * List items
     */
    private ArrayList<String> items = new ArrayList<>();

    /**
     * Typefaces
     */
    private Typeface typeface;
    private Typeface typefaceFocus;

    /**
     * Application context
     */
    private Context context;

    /**
     * Adapter listener
     */
    private SimpleListAdapterListener listener;

    /**
     * Constructor
     *
     * @param data  adapter data
     */
    public SimpleListAdapter(ArrayList<String> data) {
        this.items.addAll(data);
    }

    @NonNull
    @Override
    public SimpleListViewHolder onCreateViewHolder(final @NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        final View view = LayoutInflater.from(context).inflate(R.layout.simple_list_item, viewGroup, false);
        final SimpleListViewHolder viewHolder = new SimpleListViewHolder(view);
        typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular"));
        typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium"));

        viewHolder.getOptionButton().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                           context,
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    v.setBackground(selectorDrawable);

                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        viewHolder.getOptionButton().getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    viewHolder.getOptionButton().getTextView().setTypeface(typefaceFocus);
                } else {
                    v.setBackground(ContextCompat.getDrawable(context,R.drawable.transparent_shape));
                    viewHolder.getOptionButton().getTextView().setTypeface(typeface);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        viewHolder.getOptionButton().getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                }
            }
        });
        viewHolder.getOptionButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = viewHolder.getAdapterPosition();
                if (listener != null) {
                    listener.onItemClicked(position);
                }
            }
        });
        viewHolder.getOptionButton().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (viewHolder.getAdapterPosition() == items.size() - 1 && !enableNextFocusDown) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleListViewHolder simpleListViewHolder, int i) {
        String item = items.get(i);
        simpleListViewHolder.getOptionButton().setBackground(ContextCompat.getDrawable(context,R.drawable.transparent_shape));
        simpleListViewHolder.getOptionButton().getTextView().setTextSize(15);
        simpleListViewHolder.getOptionButton().setText(item);
        simpleListViewHolder.getOptionButton().getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        simpleListViewHolder.getOptionButton().getTextView().setTypeface(typeface);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Enable/disable next focus down
     * @param enableNextFocusDown
     */
    public void setEnableNextFocusDown(boolean enableNextFocusDown) {
        this.enableNextFocusDown = enableNextFocusDown;
    }

    /**
     * Refresh adapter data
     * @param data
     */
    public void refresh(ArrayList<String> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    /**
     * Set adapter listener
     *
     * @param listener
     */
    public void setListener(SimpleListAdapterListener listener) {
        this.listener = listener;
    }

    /**
     * Simple list adapter listener
     */
    public interface SimpleListAdapterListener {
        /**
         * On list item clicked
         *
         * @param position item position inside the list
         */
        void onItemClicked(int position);
    }
}
