package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleListAdapter;
import com.iwedia.cltv.scan_activity.entities.SatelliteTransponder;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;

/**
 * Satellite Transponders fragment
 *
 * @author Dejan Nadj
 */
public class TranspondersFragment extends GenericFragment {

    /**
     * Satellites list
     */
    private RecyclerView list;

    /**
     * Add button
     */
    private ReferenceDrawableButton addButton;

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
        titleTv.setText(ConfigStringsManager.Companion.getStringById("transponders"));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Init list
        list = (RecyclerView) view.findViewById(R.id.list);
        final ArrayList<SatelliteTransponder> satelliteTransponders = new ArrayList();
        SatelliteTransponder transponder = new SatelliteTransponder(10729, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder1 = new SatelliteTransponder(10743, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder2 = new SatelliteTransponder(10758, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder3 = new SatelliteTransponder(10773, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder4 = new SatelliteTransponder(10788, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder5 = new SatelliteTransponder(10802, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder6 = new SatelliteTransponder(10817, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder7 = new SatelliteTransponder(10832, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder8 = new SatelliteTransponder(10847, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder9 = new SatelliteTransponder(10861, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder10 = new SatelliteTransponder(10888, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder11 = new SatelliteTransponder(10910, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder12 = new SatelliteTransponder(10932, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        SatelliteTransponder transponder13 = new SatelliteTransponder(10947, 22000, SatelliteTransponder.Polarization.HORIZONTAL, SatelliteTransponder.TyningType.DVB_S2);
        SatelliteTransponder transponder14 = new SatelliteTransponder(10961, 22000, SatelliteTransponder.Polarization.VERTICAL, SatelliteTransponder.TyningType.DVB_S);
        satelliteTransponders.add(transponder);
        satelliteTransponders.add(transponder1);
        satelliteTransponders.add(transponder2);
        satelliteTransponders.add(transponder3);
        satelliteTransponders.add(transponder4);
        satelliteTransponders.add(transponder5);
        satelliteTransponders.add(transponder6);
        satelliteTransponders.add(transponder7);
        satelliteTransponders.add(transponder8);
        satelliteTransponders.add(transponder9);
        satelliteTransponders.add(transponder10);
        satelliteTransponders.add(transponder11);
        satelliteTransponders.add(transponder12);
        satelliteTransponders.add(transponder13);
        satelliteTransponders.add(transponder14);

        ArrayList<String> items = new ArrayList<>();
        for (SatelliteTransponder satelliteTransponder : satelliteTransponders) {
            items.add(satelliteTransponder.toString());
        }
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
                SatelliteHelperClass.setSatelliteTransponder(satelliteTransponders.get(position));
                SatelliteHelperClass.setSelectedTransponderPos(position);
                GenericFragment fragment = new EditTransponderFragment();
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
        addButton = (ReferenceDrawableButton) view.findViewById(R.id.add_button);

        addButton.getTextView().setText(ConfigStringsManager.Companion.getStringById("start"));
        addButton.setDrawable(null);
        
        addButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {

                    String selectorColor = ConfigColorManager.Companion.getColor("color_selector");
                    Drawable selectorDrawable = ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.focus_shape
                    );

                    DrawableCompat.setTint(selectorDrawable, Color.parseColor(selectorColor));
                    addButton.setBackground(selectorDrawable);

                    addButton.getTextView().setTextSize(15);
                    addButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                    addButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    addButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.reference_button_non_focus_shape));
                    addButton.getBackground().setColorFilter(Color.parseColor(ConfigColorManager.Companion.getColor("color_not_selected")), PorterDuff.Mode.SRC_OVER);
                    addButton.getTextView().setTextSize(15);
                    addButton.getTextView().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    addButton.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });
        
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Companion.clickAnimation(v);
                SatelliteHelperClass.setSelectedTransponderPos(-1);
                EditTransponderFragment fragment = new EditTransponderFragment();
                fragment.setAddType();
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
            }
        });

        //Scroll to satellite position in list
        final int selected = SatelliteHelperClass.getSelectedTransponderPos();
        if (selected == -1) {
            addButton.requestFocus();
        } else {
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
                SatelliteHelperClass.setSatelliteTransponder(null);
                SatelliteHelperClass.setSelectedTransponderPos(0);
                GenericFragment fragment = new SatelliteOptionsFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(MainFragment.SELECTED_OPTION, 3);
                fragment.setArguments(bundle);
                ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }
}
