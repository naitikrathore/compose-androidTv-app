package com.iwedia.cltv.scan_activity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import androidx.core.graphics.drawable.DrawableCompat;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.KeyboardView;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

/**
 * Enter URL fragment
 *
 * @author Dejan Nadj
 */
public class EnterUrlFragment extends GenericFragment {

    /**
     * Edit text
     */
    private EditText editText;

    @SuppressLint("ResourceType")
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.enter_url_layout, container, false);
        View bottom_line = (View) view.findViewById(R.id.bottom_line);
        bottom_line.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        RelativeLayout relative_background_layout = (RelativeLayout) view.findViewById(R.id.relative_background_layout);
        relative_background_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("enter_url"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        editText = (EditText) view.findViewById(R.id.edit_text);
        editText.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
        editText.setText("http://");
        editText.setFocusable(false);
        editText.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        try {
            int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
            editText.setBackgroundTintList(ColorStateList.valueOf(color_context));
        } catch(Exception ex) {
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
        }

        KeyboardView keyboardView = (KeyboardView) view.findViewById(R.id.keyboard);
        keyboardView.requestFocus();
        keyboardView.setEditText(editText);

        final ReferenceDrawableButton startButton = (ReferenceDrawableButton) view.findViewById(R.id.start_button);
        startButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
        startButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("start"));
        startButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        startButton.setDrawable(null);

        startButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
        startButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    startButton.setBackground(selectorDrawable);

                    startButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                } else {
                    startButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    startButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                }
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                ArrayList<ScanningFragment.ScanningType> scanningTypes = new ArrayList<>();
                scanningTypes.add(ScanningFragment.ScanningType.IP);
                ScanningFragment scanningFragment = new ScanningFragment();
                scanningFragment.setScanningTypeList(scanningTypes);
                scanningFragment.setUrl(editText.getText().toString());
                scanningFragment.setManualScan();
                ((IwediaSetupActivity) getActivity()).showFragment(scanningFragment);
//                ScanHandler.getInstance().startIpScan(editText.getText().toString());
            }
        });
        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new IpManualScanFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
