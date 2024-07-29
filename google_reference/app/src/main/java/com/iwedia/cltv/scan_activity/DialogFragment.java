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
 * Dialog screen fragment
 *
 * @author Dejan Nadj
 */
public class DialogFragment extends GenericFragment {

    /**
     * Dialog entity
     */
    public DialogEntity dialogEntity;

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_layout, container, false);

        //Init title
        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(ConfigStringsManager.Companion.getStringById("are_you_sure"));
        titleTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        titleTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        if (dialogEntity != null) {
            titleTv.setText(dialogEntity.getTitle());
        }

        //Init list
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        ArrayList items = new ArrayList();
        if (dialogEntity != null && dialogEntity.getOptions() != null) {
            for (String option : dialogEntity.getOptions()) {
                items.add(option);
            }
        } else {
            items.add(ConfigStringsManager.Companion.getStringById("yes"));
            items.add(ConfigStringsManager.Companion.getStringById("no"));
        }
        SimpleListAdapter listAdapter = new SimpleListAdapter(items);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.setAdapter(listAdapter);
        list.requestFocus();

        listAdapter.setListener(new SimpleListAdapter.SimpleListAdapterListener() {
            @Override
            public void onItemClicked(int position) {
                if (dialogEntity != null && dialogEntity.getDialogListener() != null) {
                    if (previousFragment != null) {
                        ((IwediaSetupActivity) getActivity()).showFragment(previousFragment);
                    }
                    dialogEntity.getDialogListener().onOptionClicked(position);
                }
            }
        });

        return view;
    }

    /**
     * Set dialog entity
     *
     * @param dialogEntity
     */
    public void setDialogEntity(DialogEntity dialogEntity) {
        this.dialogEntity = dialogEntity;
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
