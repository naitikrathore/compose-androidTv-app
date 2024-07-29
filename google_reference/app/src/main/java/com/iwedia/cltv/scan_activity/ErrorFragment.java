package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;


/**
 * Error screen fragment
 *
 * @author Dejan Nadj
 */
public class ErrorFragment extends GenericFragment {

    /**
     * Error message
     */
    private String errorMessage;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.error_fragment_layout, container, false);

        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleTv.setText(ConfigStringsManager.Companion.getStringById("error_message"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        if (errorMessage != null) {
            titleTv.setText(errorMessage);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previousFragment != null) {
                    ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                }
            }
        });
        view.setClickable(true);
        view.requestFocus();

        return view;
    }

    /**
     * Set error message
     *
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (previousFragment != null) {
                    ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
