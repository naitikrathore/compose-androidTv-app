package com.iwedia.cltv.scan_activity.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.entities.ScanDetails;

/**
 * Scann details view
 *
 * @author Dragan Krnjaic
 */
public class ScanDetailsView extends RelativeLayout {

    /**
     * Scan details
     */
    private ScanDetails scanDetailsEntity;

    /**
     * Constructor
     *
     * @param context context
     */
    public ScanDetailsView(Context context, ScanDetails scanDetailsEntity) {
        super(context);
        this.scanDetailsEntity = scanDetailsEntity;
        init(scanDetailsEntity);
    }

    public ScanDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScanDetailsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Init view
     */
    private void init (ScanDetails scanDetailsEntity) {
        View view = inflate(getContext(), R.layout.scan_details_layout, this);

        TextView title = (TextView) view.findViewById(R.id.scanning_type);
        title.setText(scanDetailsEntity.getScanType());
        title.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));
        title.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        TextView tvProgrammesFound = (TextView) view.findViewById(R.id.tv_programmes_details_info);
        tvProgrammesFound.setText(scanDetailsEntity.getTvProgrammesNumber() + "  " + ConfigStringsManager.Companion.getStringById("radio_programmes_found"));
        tvProgrammesFound.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
        tvProgrammesFound.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));


        TextView radioProgrammesFound = (TextView) view.findViewById(R.id.radio_programmes_details_info);
        radioProgrammesFound.setText(scanDetailsEntity.getRadioProgrammesNumber() + "  " + ConfigStringsManager.Companion.getStringById("radio_programmes_found"));
        radioProgrammesFound.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
        radioProgrammesFound.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        radioProgrammesFound.setVisibility(GONE);


    }
}
