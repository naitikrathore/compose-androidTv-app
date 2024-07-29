package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
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
import java.util.HashMap;

/**
 * Simple check list adapter
 *
 * @author Dragan Krnjaic
 */
public class SimpleCheckListAdapter extends RecyclerView.Adapter<SimpleCheckListViewHolder> {

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
    private SimpleListAdapter.SimpleListAdapterListener listener;

    /**
     * Holders hash map
     */
    private HashMap<Integer, SimpleCheckListViewHolder> holderHashMap;

    /**
     * Constructor
     *
     * @param data adapter data
     */
    public SimpleCheckListAdapter(ArrayList<String> data) {
        this.items.addAll(data);
        this.holderHashMap = new HashMap<>();
    }

    @NonNull
    @Override
    public SimpleCheckListViewHolder onCreateViewHolder(final @NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        final View view = LayoutInflater.from(context).inflate(R.layout.simple_check_list_item, viewGroup, false);
        final SimpleCheckListViewHolder viewHolder = new SimpleCheckListViewHolder(view);
        typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular"));
        typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium"));

        viewHolder.getCheckBoxDrawableButton().setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
                        viewHolder.getCheckBoxDrawableButton().getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    viewHolder.getCheckBoxDrawableButton().getTextView().setTypeface(typefaceFocus);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        viewHolder.getCheckBoxDrawableButton().getDrawable().setColorFilter(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                } else {
                    v.setBackground(ContextCompat.getDrawable(context, R.drawable.transparent_shape));
                    viewHolder.getCheckBoxDrawableButton().getTextView().setTypeface(typeface);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        viewHolder.getCheckBoxDrawableButton().getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        viewHolder.getCheckBoxDrawableButton().getDrawable().setColorFilter(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                }
            }
        });

        viewHolder.getCheckBoxDrawableButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                if (listener != null) {
                    if (viewHolder.getCheckBoxDrawableButton().isChecked()) {
                        viewHolder.getCheckBoxDrawableButton().setUnchecked();
                    } else {
                        viewHolder.getCheckBoxDrawableButton().setChecked();
                    }
                    listener.onItemClicked(position);
                }
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleCheckListViewHolder simpleCheckListViewHolder, int i) {
        if (holderHashMap.containsKey(i)) {
            holderHashMap.remove(i);
        }
        holderHashMap.put(i, simpleCheckListViewHolder);
        String item = items.get(i);
        simpleCheckListViewHolder.getCheckBoxDrawableButton().setBackground(ContextCompat.getDrawable(context, R.drawable.transparent_shape));
        simpleCheckListViewHolder.getCheckBoxDrawableButton().setDrawable(ContextCompat.getDrawable(context, R.drawable.ic_field));
        simpleCheckListViewHolder.getCheckBoxDrawableButton().setText(item);
        simpleCheckListViewHolder.getCheckBoxDrawableButton().getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        simpleCheckListViewHolder.getCheckBoxDrawableButton().getTextView().setTypeface(typeface);
    }

    /**
     * Is item checked
     *
     * @param position item position in list
     * @return
     */
    public boolean isChecked(int position) {
        return holderHashMap.containsKey(position) ? holderHashMap.get(position).getCheckBoxDrawableButton().isChecked() : false;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Set adapter listener
     *
     * @param listener
     */
    public void setListener(SimpleListAdapter.SimpleListAdapterListener listener) {
        this.listener = listener;
    }

    /**
     * Refresh adapter data
     *
     * @param data
     */
    public void refresh(ArrayList<String> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }
}
