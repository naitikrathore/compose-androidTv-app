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
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

/**
 * Satellites fragment
 *
 * @author Dejan Nadj
 */
public class SatellitesFragment extends GenericFragment {

    /**
     * Satellites list
     */
    private RecyclerView list;

    /**
     * Add button
     */
    private SimpleTextView addButton;

    /**
     * Last focused list item position
     */
    private int focusedItemPos;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.satellites_layout, container, false);
        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.satellites_title);
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init list
        list = (RecyclerView) view.findViewById(R.id.list);
        final ArrayList<String> items = new ArrayList();
        items.add("Arabsat 6A");
        items.add("Ses 12");
        items.add("Alcomsat 1");
        items.add("Koreasat-5A");
        items.add("Echostar 105/SES 11");
        items.add("Asiasat 9");
        items.add("Amazonas 5 ");
        items.add("Intelsat 35E");
        items.add("Hellas-Sat 3");
        items.add("Bulgariasat-1");
        items.add("Serbia-Sat 1");
        items.add("Serbia-Sat 2");
        items.add("Serbia-Sat 3");
        items.add("Serbia-Sat 4");
        items.add("Serbia-Sat 5");
        final SimpleListAdapter listAdapter = new SimpleListAdapter(items);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.setAdapter(listAdapter);
        list.requestFocus();

        final NestedScrollView scrollView = (NestedScrollView) view.findViewById(R.id.scrollView);
        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int diff = (list.getBottom() - (scrollView.getHeight() + scrollY));
                boolean isUp = (oldScrollY - scrollY) > 0;
                if (diff <= 50 && !isUp) {
                    final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
                    int leftMargin = (int) getResources().getDimension(R.dimen.custom_dim_57);
                    int topMargin = (int) getResources().getDimension(R.dimen.custom_dim_46_5);
                    int bottomMargin = (int) getResources().getDimension(R.dimen.custom_dim_41);
                    params.setMargins(leftMargin, topMargin, 0, bottomMargin);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.setLayoutParams(params);
                            scrollView.invalidate();
                        }
                    });
                } else if (diff > 100 && isUp){
                    final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
                    int leftMargin = (int) getResources().getDimension(R.dimen.custom_dim_57);
                    int topMargin = (int) getResources().getDimension(R.dimen.custom_dim_46_5);
                    params.setMargins(leftMargin, topMargin, 0, 0);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.setLayoutParams(params);
                            scrollView.invalidate();
                        }
                    });
                }
            }
        });
        listAdapter.setListener(new SimpleListAdapter.SimpleListAdapterListener() {
            @Override
            public void onItemClicked(int position) {
                GenericFragment fragment = new SatelliteOptionsFragment();
                SatelliteHelperClass.setSatelliteName(items.get(position));
                SatelliteHelperClass.setSatellitePosition(position);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
            }
        });

        //Init signal strength progress bar
        ProgressBarView signalStrengthProgress = (ProgressBarView) view.findViewById(R.id.signal_strength_progress);
        signalStrengthProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgress.setProgress(80);

        //Init signal quality progress bar
        ProgressBarView signalQualityProgress = (ProgressBarView) view.findViewById(R.id.signal_quality_progress);
        signalQualityProgress.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgress.setProgress(90);

        //Add button click listener
        addButton = (SimpleTextView) view.findViewById(R.id.add_button);
        addButton.setText(ConfigStringsManager.Companion.getStringById("start"));
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                EditSatelliteFragment fragment = new EditSatelliteFragment();
                fragment.setAddType();
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
            }
        });

        //Scroll to satellite position in list
        final int selected = SatelliteHelperClass.getSatellitePosition();
        list.post(new Runnable() {
            @Override
            public void run() {
                if (list.getLayoutManager().findViewByPosition(selected) != null) {
                    list.getLayoutManager().findViewByPosition(selected).requestFocus();
                }
            }
        });

        return view;
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                //Save last focused list item position
                if (list.hasFocus()) {
                    focusedItemPos = list.getLayoutManager().getPosition(list.getFocusedChild());
                }
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                //Restore focused list item position
                if (addButton.hasFocus()) {
                    if (list.getLayoutManager().findViewByPosition(focusedItemPos) != null) {
                        list.getLayoutManager().findViewByPosition(focusedItemPos).requestFocus();
                    }
                }
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                //Clear satellite helper class data
                SatelliteHelperClass.setSatelliteName(null);
                SatelliteHelperClass.setSatellitePosition(0);
                GenericFragment fragment = new ManualScanFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 2);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
