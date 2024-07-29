package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.EditTextView;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.scan_activity.core.TraversalListView;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

import static com.iwedia.cltv.scan_activity.TextInsertFragment.TEXT_INSERT_RESULT;


/**
 * Edit satellite fragment
 *
 * @author Dejan Nadj
 */
public class EditSatelliteFragment extends GenericFragment {

    /**
     * Is add type of fragment
     */
    private boolean isAddType;

    /**
     * Satellite name
     */
    private String satelliteName;

    /**
     * Satellite name edit text
     */
    private EditTextView nameEv;

    /**
     * Keyboard layout
     */
    private RelativeLayout keyboardLayout;

    /**
     * Keyboard view
     */
    private KeyboardView keyboardView;

    /**
     * Fragment title
     */
    private TextView titleTv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.edit_satellite_layout, container, false);
        View view_layout = (View) view.findViewById(R.id.view_layout);
        view_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        //Init title
        titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("edit_satellite"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init satellite name edit text
        nameEv = (EditTextView) view.findViewById(R.id.satellite_name_edit_text);
        nameEv.setTitle(ConfigStringsManager.Companion.getStringById("satellite_name"));
        nameEv.setText(SatelliteHelperClass.getSatelliteName());

        TextView satellitesTitle = view.findViewById(R.id.satellites_title);
        satellitesTitle.setText(ConfigStringsManager.Companion.getStringById("satellites"));

        //Set edit name click listener
        nameEv.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInsertFragment fragment = new TextInsertFragment();
                fragment.setTitle(ConfigStringsManager.Companion.getStringById("satellite_name"));
                fragment.setEditTextContent(satelliteName);
                fragment.setButtonText(ConfigStringsManager.Companion.getStringById("save"));
                fragment.setPreviousFragment(EditSatelliteFragment.this);
                ((IwediaSetupActivity) getActivity()).hideFragment(EditSatelliteFragment.this, fragment);
            }
        });

        //Get satellite name
        if (isAddType) {
            nameEv.setText("");
            nameEv.getEditText().setHint(ConfigStringsManager.Companion.getStringById("satellite_name"));
            titleTv.setText(ConfigStringsManager.Companion.getStringById("add_satellite"));
        } else {
            satelliteName = SatelliteHelperClass.getSatelliteName();
            nameEv.setText(satelliteName);
            titleTv.setText(ConfigStringsManager.Companion.getStringById("edit_satellite") + " (" + satelliteName + ")");
        }

        //Init satellite angle edit text
        EditTextView angleEv = (EditTextView) view.findViewById(R.id.satellite_angle_edit_text);
        angleEv.setTitle(ConfigStringsManager.Companion.getStringById("satellite_angle"));
        if (isAddType) {
            angleEv.setText("");
            angleEv.getEditText().setHint(ConfigStringsManager.Companion.getStringById("satellite_angle"));
        } else {
            angleEv.setText("019.2");
        }

        keyboardLayout = (RelativeLayout) view.findViewById(R.id.keyboard_layout);
        keyboardLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_background")));
        keyboardView = (KeyboardView) view.findViewById(R.id.keyboard);
        keyboardView.setNumericType();

        angleEv.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                keyboardLayout.setVisibility(View.VISIBLE);
                keyboardView.setVisibility(View.VISIBLE);
                keyboardView.requestFocus();
                keyboardView.setEditText((EditText) v);

                keyboardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (keyboardView.getVisibility() == View.GONE) {
                            keyboardLayout.setVisibility(View.GONE);
                            v.requestFocus();
                            keyboardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });

        //Init east/west traversal list
        TraversalListView eastWest = (TraversalListView) view.findViewById(R.id.east_wast_tl);
        eastWest.setTitle(ConfigStringsManager.Companion.getStringById("east_west"));
        ArrayList<String> items = new ArrayList<>();
        items.add(ConfigStringsManager.Companion.getStringById("east"));
        items.add(ConfigStringsManager.Companion.getStringById("west"));
        eastWest.setItems(items);

        //Save button click listener
        ReferenceDrawableButton saveButton = (ReferenceDrawableButton) view.findViewById(R.id.save_button);
        saveButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("save"));

        saveButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("save"));
        saveButton.setDrawable(null);

        saveButton.post(new Runnable() {
            @Override
            public void run() {
                saveButton.requestFocus();
            }
        });

        saveButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    saveButton.setBackground(selectorDrawable);

                    saveButton.getTextView().setTextSize(15);
                    saveButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    saveButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    saveButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    saveButton.getBackground().setColorFilter(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")), PorterDuff.Mode.SRC_OVER);
                    saveButton.getTextView().setTextSize(15);
                    saveButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    saveButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
            }

        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Get satellite name
        if (getArguments() != null && getArguments().containsKey(TextInsertFragment.TEXT_INSERT_RESULT)) {
            satelliteName = getArguments().getString(TextInsertFragment.TEXT_INSERT_RESULT);
            if (satelliteName != null) {
                SatelliteHelperClass.setSatelliteName(satelliteName);
                nameEv.setText(satelliteName);
                nameEv.getEditText().setSelection(satelliteName.length());
            }
            if (!isAddType) {
                titleTv.setText(ConfigStringsManager.Companion.getStringById("edit_satellite") + " (" + satelliteName + ")");
            }
            nameEv.requestFocus();
        }
    }

    /**
     * Set add type fragment
     */
    public void setAddType() {
        isAddType = true;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (isAddType) {
                    GenericFragment fragment = new SatellitesFragment();
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                    return true;
                }
                GenericFragment fragment = new SatelliteOptionsFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 1);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
