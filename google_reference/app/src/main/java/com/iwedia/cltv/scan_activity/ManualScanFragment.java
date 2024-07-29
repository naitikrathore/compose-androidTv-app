package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.scan_activity.entities.FrontendType;

import java.util.ArrayList;

/**
 * Manual Scan fragment
 *
 * @author Dejan Nadj
 */
public class ManualScanFragment extends GenericFragment {

    /**
     * Simple check box
     */
    private ReferenceDrawableButton clearAllChannelsCb;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.manual_scan_layout, container, false);

        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.manual_tuning_title);
        clearAllChannelsCb = view.findViewById(R.id.clear_all_channels_cb);
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        clearAllChannelsCb.getTextView().setText(ConfigStringsManager.Companion.getStringById("manual_tuning"));

        clearAllChannelsCb = view.findViewById(R.id.clear_all_channels_cb);
        clearAllChannelsCb.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
//        ScanHandler.getInstance().setClearAllChannels(clearAllChannelsCb.isChecked());
        clearAllChannelsCb.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.transparent_shape));
        clearAllChannelsCb.getTextView().setText(ConfigStringsManager.Companion.getStringById("clear_all_channels"));
        clearAllChannelsCb.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_field));
        clearAllChannelsCb.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    clearAllChannelsCb.setBackground(selectorDrawable);

                    clearAllChannelsCb.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        clearAllChannelsCb.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    clearAllChannelsCb.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        clearAllChannelsCb.getDrawable().setColorFilter(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                } else {
                    clearAllChannelsCb.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.transparent_shape));
                    clearAllChannelsCb.getTextView().setTextSize(13);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        clearAllChannelsCb.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    clearAllChannelsCb.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular")));
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        clearAllChannelsCb.getDrawable().setColorFilter(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                }
            }
        });

        ScanHelper.INSTANCE.setClearAllChannels(clearAllChannelsCb.isChecked());
        clearAllChannelsCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clearAllChannelsCb.isChecked()) {
                    clearAllChannelsCb.setUnchecked();
                } else {
                    clearAllChannelsCb.setChecked();
                }
                ScanHelper.INSTANCE.setClearAllChannels(clearAllChannelsCb.isChecked());
            }
        });

        clearAllChannelsCb.requestFocus();

        //Scan for supported frontends
        String[] supportedFronteds = getResources().getStringArray(R.array.supported_frontends);

        //Prepare UI components
        final ArrayList<FrontendType> fontendTypes = new ArrayList();
        final ArrayList items = new ArrayList();

        //Resolve supported frontends
        for (String supportedFrontend : supportedFronteds) {

            String stringId = "";
            FrontendType.Type frontedType = null;

            if (supportedFrontend.equalsIgnoreCase(getResources().getString(R.string.const_fe_ipott))) {
                stringId = "ip";
                frontedType = FrontendType.Type.IP;
            } else if (supportedFrontend.equalsIgnoreCase(getResources().getString(R.string.const_fe_dvbc))) {
                stringId = "cable";
                frontedType = FrontendType.Type.DVB_C;
            } else if (supportedFrontend.equalsIgnoreCase(getResources().getString(R.string.const_fe_dvbt))) {
                stringId = "terrestrial";
                frontedType = FrontendType.Type.DVB_T;
            } else if (supportedFrontend.equalsIgnoreCase(getResources().getString(R.string.const_fe_dvbs))) {
                stringId = "satellite";
                frontedType = FrontendType.Type.DVB_S;
            }

            fontendTypes.add(new FrontendType(ConfigStringsManager.Companion.getStringById(stringId), frontedType));
            items.add(ConfigStringsManager.Companion.getStringById(stringId));

        }

        //Init list
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        SimpleListAdapter listAdapter = new SimpleListAdapter(items);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.setAdapter(listAdapter);
        listAdapter.setListener(new SimpleListAdapter.SimpleListAdapterListener() {
            @Override
            public void onItemClicked(int position) {
                if (fontendTypes.get(position).getType() == FrontendType.Type.DVB_T) {
                    ((IwediaSetupActivity) getActivity()).showFragment(new TerrestrialManualScanFragment());
                } else if (fontendTypes.get(position).getType() == FrontendType.Type.DVB_C) {
                    ((IwediaSetupActivity) getActivity()).showFragment(new CableManualScanFragment());
                } else if (fontendTypes.get(position).getType() == FrontendType.Type.DVB_S) {
                    ((IwediaSetupActivity) getActivity()).showFragment(new SatelliteOptionsFragment());
                } else if (fontendTypes.get(position).getType() == FrontendType.Type.IP) {
                    ((IwediaSetupActivity) getActivity()).showFragment(new IpManualScanFragment());
                }
            }
        });
        //Scroll to selected option
        //Selected option should be passed on back from previous opened option
        if (getArguments() != null && getArguments().containsKey(MainFragment.SELECTED_OPTION)) {
            final int selected = getArguments().getInt(MainFragment.SELECTED_OPTION);
            list.post(new Runnable() {
                @Override
                public void run() {
                    if (list.getLayoutManager().findViewByPosition(selected) != null) {
                        list.getLayoutManager().findViewByPosition(selected).requestFocus();
                    }
                }
            });
        }
        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new MainFragment();
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

