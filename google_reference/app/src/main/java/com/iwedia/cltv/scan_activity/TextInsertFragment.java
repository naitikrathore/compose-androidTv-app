package com.iwedia.cltv.scan_activity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.utils.Utils;

/**
 * Text insert fragment
 *
 * @author Dejan Nadj
 */
public class TextInsertFragment extends GenericFragment {

    /**
     * Text insert fragment result argument key
     */
    public static final String TEXT_INSERT_RESULT = "text_insert_result";

    /**
     * Title text
     */
    private String title;

    /**
     * Edit text content
     */
    private String editTextContent;

    /**
     * Button text
     */
    private String buttonText;

    /**
     * Title text view
     */
    private TextView titleTv;

    /**
     * Button
     */
    private TextView button;

    /*
     * Edit text
     */
    private EditText editText;

    /**
     * Previous fragment
     */
    private GenericFragment previousFragment;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.text_insert_fragment, container, false);
        View bottom_line = (View) view.findViewById(R.id.bottom_line);
        bottom_line.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("enter_url"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        if (title != null) {
            titleTv.setText(title);
        }

        editText = (EditText) view.findViewById(R.id.edit_text);
        editText.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        try {
            int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
            editText.setBackgroundTintList(ColorStateList.valueOf(color_context));
        } catch(Exception ex) {
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
        }
        editText.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
        if (editTextContent != null) {
            editText.setText(editTextContent);
        }
        editText.setFocusable(false);

        KeyboardView keyboardView = (KeyboardView) view.findViewById(R.id.keyboard);
        keyboardView.requestFocus();
        keyboardView.setEditText(editText);

        button = (TextView) view.findViewById(R.id.button);
        button.setText(ConfigStringsManager.Companion.getStringById("start"));
        button.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
        button.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        if (buttonText != null) {
            button.setText(buttonText);
        } else {
            button.setText(ConfigStringsManager.Companion.getStringById("save"));
        }
        button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    button.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                } else {
                    v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                    button.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                }
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                if (previousFragment != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(TEXT_INSERT_RESULT, editText.getText().toString());
                    previousFragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                }
            }
        });
        return view;
    }

    /**
     * Set fragment title
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Set edit text content
     * @param content
     */
    public void setEditTextContent(String content) {
        this.editTextContent = content;
    }

    /**
     * Set button text
     * @param buttonText
     */
    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    /**
     * Set previous fragment
     * @param previousFragment
     */
    public void setPreviousFragment(GenericFragment previousFragment) {
        this.previousFragment = previousFragment;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (previousFragment != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(TEXT_INSERT_RESULT, editTextContent);
                    previousFragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
