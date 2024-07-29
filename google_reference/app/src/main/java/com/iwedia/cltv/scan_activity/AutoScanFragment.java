package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.scan_activity.core.SimpleCheckListAdapter;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.SimpleCheckListAdapter;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.entities.FrontendType;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

/**
 * Auto scan fragment
 *
 * @author Dragan Krnjaic
 */
public class AutoScanFragment extends GenericFragment {

    /**
     * List adapter
     */
    private SimpleCheckListAdapter listAdapter;

    /**
     * Scan button
     */
    private ReferenceDrawableButton scanButton;
    private ArrayList<FrontendType> fontendTypes;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.auto_scan_layout, container, false);

        //Init title
        final TextView titleTv = (TextView) view.findViewById(R.id.auto_tuning_title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("auto_tuning"));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        //Init list
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.list);

        //Scan for supported frontends
        String[] supportedFronteds = getResources().getStringArray(R.array.supported_frontends);

//        ScanHandler.getInstance().setClearAllChannels(true);

        //Prepare UI components
        fontendTypes = new ArrayList();
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

        //Prepare UI components
        listAdapter = new SimpleCheckListAdapter(items);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.setAdapter(listAdapter);

        listAdapter.setListener(new SimpleListAdapter.SimpleListAdapterListener() {
            @Override
            public void onItemClicked(int position) {
                checkState();
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

        scanButton = view.findViewById(R.id.scan_button);
        scanButton.setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("scan"));
        scanButton.setFocusable(false);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanningFragment fragment = new ScanningFragment();
                fragment.setScanningTypeList(getScanningList());
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
            }
        });

        scanButton.clearFocus();
        list.requestFocus();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        list.post(new Runnable() {
            @Override
            public void run() {
                if (list.getLayoutManager().findViewByPosition(0) != null) {
                    list.getLayoutManager().findViewByPosition(0).requestFocus();
                }
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                GenericFragment fragment = new MainFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }

    /**
     * Check is some list item checked and enable/disable scan button
     */
    private void checkState() {
        boolean isChecked = false;
        for (int i = 0; i < listAdapter.getItemCount(); i++) {
            if (listAdapter.isChecked(i)) {
                isChecked = true;
                break;
            }
        }
        if (isChecked) {
            scanButton.setFocusable(true);
        } else {
            scanButton.setFocusable(false);
        }
    }

    /**
     * Create scanning type lit for checked list items
     *
     * @return created scannig type list
     */
    private ArrayList<ScanningFragment.ScanningType> getScanningList() {
        ArrayList<ScanningFragment.ScanningType> scanningTypes = new ArrayList<>();
        for (int i = 0; i < listAdapter.getItemCount(); i++) {
            if (listAdapter.isChecked(i)) {
                scanningTypes.add(frontType2ScanType(fontendTypes.get(i)));
            }
        }
        return scanningTypes;
    }

    private ScanningFragment.ScanningType frontType2ScanType(FrontendType type) {
        ScanningFragment.ScanningType scanType = ScanningFragment.ScanningType.IP;

        switch(type.getType()) {
            case DVB_C:
                scanType = ScanningFragment.ScanningType.CABLE;
                break;
            case DVB_T:
                scanType = ScanningFragment.ScanningType.TERRESTRIAL;
                break;
            case DVB_S:
                scanType = ScanningFragment.ScanningType.SATELLITE;
                break;
            case IP:
            default:
                scanType = ScanningFragment.ScanningType.IP;
                break;
        }

        return scanType;
    }
}
