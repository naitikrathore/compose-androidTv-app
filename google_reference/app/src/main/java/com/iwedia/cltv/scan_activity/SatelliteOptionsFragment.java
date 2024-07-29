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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.scan_activity.entities.DialogEntity;

import java.util.ArrayList;

/**
 * Satellite Options fragment
 *
 * @author Dejan Nadj
 */
public class SatelliteOptionsFragment extends GenericFragment {

    /**
     * Satellite name
     */
    private String satelliteName;

    /**
     * List
     */
    private RecyclerView list;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.satellite_options, container, false);
        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.satellite_options_title);
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));        //Init list
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        list = (RecyclerView) view.findViewById(R.id.list);
        ArrayList items = new ArrayList();
        items.add(ConfigStringsManager.Companion.getStringById("scan_option"));
        items.add(ConfigStringsManager.Companion.getStringById("edit"));
        items.add(ConfigStringsManager.Companion.getStringById("antenna_config"));
        items.add(ConfigStringsManager.Companion.getStringById("transponders"));
        items.add(ConfigStringsManager.Companion.getStringById("delete"));
        SimpleListAdapter listAdapter = new SimpleListAdapter(items);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.setAdapter(listAdapter);
        list.requestFocus();
        listAdapter.setListener(new SimpleListAdapter.SimpleListAdapterListener() {
            @Override
            public void onItemClicked(int position) {
                switch (position) {
                    case 0: {
                        //Show satellite scan fragment
                        GenericFragment fragment = new SatelliteScanOptionsFragment();
                        ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                        break;
                    }
                    case 1: {
                        //Show satellite edit fragment
                        GenericFragment fragment = new EditSatelliteFragment();
                        ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                        break;
                    }
                    case 2: {
                        //Show satellite antenna config fragment
                        GenericFragment fragment = new AntennaConfigFragment();
                        ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                        break;
                    }
                    case 3: {
                        //Show satellite transponders fragment
                        GenericFragment fragment = new TranspondersFragment();
                        ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                        break;
                    }
                    case 4: {
                        //Show satellite delete fragment
                        final DialogFragment fragment = new DialogFragment();
                        final ArrayList<String> options = new ArrayList<>();
                        options.add(ConfigStringsManager.Companion.getStringById("yes"));
                        options.add(ConfigStringsManager.Companion.getStringById("no"));
                        fragment.setPreviousFragment(SatelliteOptionsFragment.this);
                        DialogEntity dialogEntity = new DialogEntity(ConfigStringsManager.Companion.getStringById("are_you_sure"),
                                options, new DialogEntity.DialogListener() {
                            @Override
                            public void onOptionClicked(int position) {
                                if (position == 0) {
                                    //TODO delete satellite
                                    ((IwediaSetupActivity) getActivity()).showFragment(new SatellitesFragment());
                                }
                            }
                        });
                        fragment.setDialogEntity(dialogEntity);
                        ((IwediaSetupActivity) getActivity()).hideFragment(SatelliteOptionsFragment.this, fragment);
                        break;
                    }
                }
            }
        });

        //Set satellite name
        satelliteName = SatelliteHelperClass.getSatelliteName();
        titleTv.setText(satelliteName);

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
                GenericFragment fragment = new SatellitesFragment();
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
