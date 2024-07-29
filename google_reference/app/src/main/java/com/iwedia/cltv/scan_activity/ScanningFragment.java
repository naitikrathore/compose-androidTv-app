package com.iwedia.cltv.scan_activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.scan_activity.core.MultipleTextView;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.ReferenceDrawableButton;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.config.ConfigStringsManager;
import com.iwedia.cltv.scan_activity.core.MultipleTextView;
import com.iwedia.cltv.scan_activity.core.ProgressBarView;
import com.iwedia.cltv.scan_activity.core.SimpleTextView;
import com.iwedia.cltv.scan_activity.entities.ScanDetails;
import com.iwedia.cltv.utils.Utils;

import java.util.ArrayList;


/**
 * Scanning fragment
 *
 * @author Dragan Krnjaic
 */
public class ScanningFragment extends GenericFragment {

    /**
     * Scanning type
     */
    public enum ScanningType {
        TERRESTRIAL("Scanning Terrestrial"),
        CABLE("Scanning Cable"),
        SATELLITE("Scanning Satellite"),
        IP("Scanning IP");

        private String title;

        ScanningType(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public static ScanningType getType(int id) {
            switch (id) {
                case 0:
                    return TERRESTRIAL;
                case 1:
                    return CABLE;
                case 2:
                    return SATELLITE;
                case 3:
                    return IP;
                default:
                    return null;
            }
        }

        public String toString() {
            switch (ordinal()) {
                case 0: {
                    return "Terrestrial";
                }
                case 1: {
                    return "Cable";
                }
                case 2: {
                    return "Satellite";
                }
                case 3: {
                    return "IP";
                }
                default: {
                    return "";
                }
            }
        }
    }

    /**
     * Scanning fragment scanning type
     */
    private ScanningType scanningType;

    /**
     * Scanning type list
     */
    private ArrayList<ScanningType> scanningList = new ArrayList<>();

    /**
     * Signal strength progress view
     */
    private ProgressBarView signalStrengthProgressView;

    /**
     * Signal quality progress view
     */
    private ProgressBarView signalQualityProgressView;

    /**
     * Tv programmes multiple views
     */
    private MultipleTextView tvProgramsFound;

    /**
     * Radio programmes multiple view
     */
    private MultipleTextView radioProgramsFound;

    /**
     * Scanning fragment title
     */
    private SimpleTextView title;

    /**
     * Skip button hint text view
     */
    private TextView hintTv;

    /**
     * Title text
     */
    private String titleText;

    /**
     * Scanning progress number
     */
    private int scanningProgressPercentage = -1;

    /**
     * Scanning progress description
     */
    private String scanningProgressDescription;

    /**
     * Tv programmes percentage
     */
    private int tvProgrammesProgressPercentage = -1;

    /**
     * Radio programmes percentage
     */
    private int radioProgrammesProgressPercentage = -1;

    /**
     * Tv programmes number
     */
    private String tvProgrammesNumber;

    /**
     * Radio programmes number
     */
    private String radioProgrammesNumber;

    /**
     * Next screen name
     */
    private String upNextHint;

    /**
     * Scanning progress bar
     */
    private ProgressBar scanningProgressBar;

    /**
     * Progress bar percentage indicator
     */
    private SimpleTextView progressValue;

    /**
     * Scanning progress description text view
     */
    private TextView scanningProgressDescriptionTv;

    /**
     * Disable back flag
     */
    private boolean disableBack;

    /**
     * Skip button
     */
    private ReferenceDrawableButton skipBtn;

    /**
     * Is manual scan
     */
    private boolean isManualScan;

    /**
     * Scan details list
     */
    private ArrayList<ScanDetails> scanDetails;

    /**
     * Is scan finished flag
     */
    private boolean isScanFinished = false;

    /**
     * Current tune frequency
     */
    private int tuneFrequency;

    /**
     * Url
     */
    private String url;

    /**
     * Set url
     *
     * @param url url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.scanning_fragment, container, false);

        title = (SimpleTextView) view.findViewById(R.id.title);
        title.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        title.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_medium")));

        hintTv = (TextView) view.findViewById(R.id.up_next_label);
        hintTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        hintTv.setText(ConfigStringsManager.Companion.getStringById("up_next_ip_scan"));
        hintTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));

        //Text bellow progress bar
        scanningProgressDescriptionTv = (TextView) view.findViewById(R.id.scanning_progress_description);
        scanningProgressDescriptionTv.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        scanningProgressDescriptionTv.setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));


        if (titleText != null) {
            title.setText(titleText);
        }

        //Percentage text indicator
        progressValue = (SimpleTextView) view.findViewById(R.id.progress_value);
        progressValue.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));

        //Scanning progress bar
        scanningProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        //Init signal strength/quality progress bar views
        signalStrengthProgressView = (ProgressBarView) view.findViewById(R.id.signal_strength_progress_view);
        signalStrengthProgressView.setTitle(ConfigStringsManager.Companion.getStringById("signal_strength"));
        signalStrengthProgressView.setVisibility(View.GONE);
        signalQualityProgressView = (ProgressBarView) view.findViewById(R.id.signal_quality_progress_view);
        signalQualityProgressView.setTitle(ConfigStringsManager.Companion.getStringById("signal_quality"));
        signalQualityProgressView.setVisibility(View.GONE);

        //Init multiple text views
        tvProgramsFound = (MultipleTextView) view.findViewById(R.id.tv_programmes_found);
        tvProgramsFound.setRightText(ConfigStringsManager.Companion.getStringById("tv_programmes_found"));
        tvProgramsFound.setTextSize(18, 18);
        radioProgramsFound = (MultipleTextView) view.findViewById(R.id.radio_programmes_found);
        radioProgramsFound.setRightText(ConfigStringsManager.Companion.getStringById("radio_programmes_found"));
        radioProgramsFound.setTextSize(12, 12);
        radioProgramsFound.setVisibility(View.GONE);

        skipBtn = view.findViewById(R.id.skip_button);
        skipBtn.setText(ConfigStringsManager.Companion.getStringById("skip"));

        skipBtn.getTextView().setText(ConfigStringsManager.Companion.getStringById("skip"));
        skipBtn.post(new Runnable() {
            @Override
            public void run() {
                skipBtn.requestFocus();
            }
        });

        skipBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    skipBtn.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.focus_shape));
                    skipBtn.getTextView().setTextSize(15);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        skipBtn.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    skipBtn.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_medium")));
                } else {
                    skipBtn.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.transparent_shape));
                    skipBtn.getTextView().setTextSize(13);
                    try {
                        int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"));
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
                        skipBtn.getTextView().setTextColor(color_context);
                    } catch(Exception ex) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
                    }
                    skipBtn.getTextView().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));
                }
            }
        });

        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.Companion.clickAnimation(view);
                String btnText = (String) skipBtn.getTextView().getText();
                if (skipBtn.getTextView().getText().toString().equals(ConfigStringsManager.Companion.getStringById("skip"))) {
                    if (scanningList != null && !scanningList.isEmpty()) {
                        setScanningType(scanningList.get(0));
                    }
                } else if (btnText.equals(ConfigStringsManager.Companion.getStringById("stop"))) {
                    ScanHelper.INSTANCE.stopScan();
                    skipBtn.setText(ConfigStringsManager.Companion.getStringById("done"));
                } else if (btnText.equals(ConfigStringsManager.Companion.getStringById("done"))) {
                    if (scanDetails != null && !scanDetails.isEmpty()) {
                        ScanCompletedFragment fragment = new ScanCompletedFragment();
                        fragment.setScanDetails(scanDetails);
                        if (isManualScan) {
                            fragment.setManualScan();
                        }
                        ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                    } else {
                        GenericFragment fragment = new MainFragment();
                        Bundle bundle = new Bundle();
                        bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                        fragment.setArguments(bundle);
                        ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                    }
                }
            }
        });

        scanDetails = new ArrayList<>();
        if (scanningList != null && !scanningList.isEmpty()) {
            setScanningType(scanningList.get(0));
        }

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset all views
        updateProgress(0);
        scanTuneFrequencyChanged(0);
        scanSignalQualityChanged(0);
        scanSignalStrengthChanged(0);
        scanRadioServiceNumber(0);
        scanTvServiceNumber(0);
    }

    /**
     * Set scanning type list
     *
     * @param scanningTypes
     */
    public void setScanningTypeList(ArrayList<ScanningType> scanningTypes) {
        this.scanningList = scanningTypes;
    }

    /**
     * Set scanning progress description text
     *
     * @param scanningProgressDescription
     */
    public void setScanningProgressDescription(String scanningProgressDescription) {
        this.scanningProgressDescription = scanningProgressDescription;
    }

    /**
     * Disable back key
     */
    public void disableBack() {
        disableBack = true;
    }

    /**
     * Set manual scan flag
     */
    public void setManualScan() {
        isManualScan = true;
    }

    /**
     * Set fragment scanning type
     *
     * @param scanningType
     */
    private void setScanningType(ScanningType scanningType) {
        this.scanningType = scanningType;
        if (scanningType != null) {
            switch (scanningType) {
                case TERRESTRIAL:
                case CABLE: {
                    title.setText(scanningType.getTitle());
                    tuneFrequency = 0;
                    String descText = scanningProgressDescription != null ? scanningProgressDescription : "Frequency: 0 kHz";
                    scanningProgressDescriptionTv.setText(descText);
                    scanningProgressBar.setProgress(0);
                    progressValue.setText(String.valueOf(0) + "%");
                    signalStrengthProgressView.setProgress(0);
                    signalQualityProgressView.setProgress(0);
                    tvProgramsFound.setLeftText("0");
                    radioProgramsFound.setLeftText("0");
                    if (!isManualScan) {
                        ScanHelper.INSTANCE.stopScan();
                        if (scanningType == ScanningType.TERRESTRIAL) {
                            ScanHelper.INSTANCE.setActiveFragment(this);
                            ScanHelper.INSTANCE.startAutoScan();
                        } else {
                            //TODO start dvbc auto scan
                        }
                    }
                    break;
                }
                case SATELLITE: {
                    String satelliteName = SatelliteHelperClass.getSatelliteName() == null ? "Alcomsat 1" : SatelliteHelperClass.getSatelliteName();
                    title.setText(scanningType.getTitle() + ": " + satelliteName);
                    String descText = SatelliteHelperClass.getSatelliteTransponder() != null ? SatelliteHelperClass.getSatelliteTransponder().toString() : "10887,H,30000";
                    scanningProgressDescriptionTv.setText(descText);
                    scanningProgressBar.setProgress(50);
                    progressValue.setText(String.valueOf(50) + "%");
                    signalStrengthProgressView.setProgress(80);
                    signalQualityProgressView.setProgress(90);
                    tvProgramsFound.setLeftText("384");
                    radioProgramsFound.setLeftText("7");
                    if (!isManualScan) {
//                        ScanHandler.getInstance().stopScan();
//                        ScanHandler.getInstance().startSatelliteAutoScan();
                    }
                    break;
                }
                case IP: {
                    title.setText(scanningType.getTitle());
                    scanningProgressDescriptionTv.setText("");
                    scanningProgressBar.setProgress(0);
                    progressValue.setText(String.valueOf(0) + "%");
                    signalStrengthProgressView.setProgress(0);
                    signalQualityProgressView.setProgress(0);
                    tvProgramsFound.setLeftText("0");
                    radioProgramsFound.setLeftText("0");
                    if (!isManualScan) {
//                        ScanHandler.getInstance().stopScan();
//                        ScanHandler.getInstance().startIpScan(url);
                    }
                    break;
                }
            }
        }
        scanningList.remove(0);
        if (!scanningList.isEmpty()) {
            hintTv.setVisibility(View.VISIBLE);
            hintTv.setText(getSkipHint(scanningList.get(0)));
        } else {
            hintTv.setVisibility(View.GONE);
            skipBtn.setText(ConfigStringsManager.Companion.getStringById("stop"));
        }
    }

    /**
     * Get skip button hint text
     *
     * @param scanningType scanning type
     * @return created hint text
     */
    private String getSkipHint(ScanningType scanningType) {
        switch (scanningType) {
            case TERRESTRIAL:
                return ConfigStringsManager.Companion.getStringById("up_next_terrestrial_scan");
            case CABLE:
                return ConfigStringsManager.Companion.getStringById("up_next_cable_scan");
            case SATELLITE:
                return ConfigStringsManager.Companion.getStringById("up_next_satellite_scan");
            case IP:
                return ConfigStringsManager.Companion.getStringById("up_next_ip_scan");
            default:
                return "";
        }
    }

    @Override
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (disableBack) {
                    return true;
                } else {
//                    ScanHandler.getInstance().stopScan();
                    GenericFragment fragment = new AutoScanFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(MainFragment.SELECTED_OPTION, 0);
                    fragment.setArguments(bundle);
                    ((IwediaSetupActivity) getActivity()).showFragment(fragment);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent);
    }

    @Override
    public void updateProgress(int progress) {
        super.updateProgress(progress);
        scanningProgressBar.setProgress(progress);
        progressValue.setText(String.valueOf(progress) + "%");
    }

    @Override
    public void scanFinished() {
        super.scanFinished();

        //Prevent multiple scan finished callbacks
        if (!isScanFinished) {
//            SourceType sourceType = SourceType.UNDEFINED;
//            switch (scanningType){
//                case TERRESTRIAL:
//                    sourceType = SourceType.TER;
//                    break;
//                case CABLE:
//                    sourceType = SourceType.CAB;
//                    break;
//                case SATELLITE:
//                    sourceType = SourceType.SAT;
//                    break;
//                case IP:
//                    sourceType = SourceType.IP;
//                    break;
//            }
            int tvServiceNumber = ScanHelper.INSTANCE.getChannelNum();
            int radioServiceNumber = 0;

            tvProgramsFound.setLeftText(String.valueOf(tvServiceNumber));
            radioProgramsFound.setLeftText(String.valueOf(radioServiceNumber));

            ScanDetails scanDetailItem = new ScanDetails(scanningType.toString(), tvServiceNumber, radioServiceNumber);

            //Check if details already added
            boolean isAlreadyExists = false;
            for (ScanDetails existingDetails : scanDetails) {
                if (existingDetails.getScanType().equals(scanDetailItem.getScanType())) {
                    isAlreadyExists = true;
                }
            }

            if (!isAlreadyExists) {
                scanDetails.add(scanDetailItem);
            }

            if (scanningList == null || scanningList.isEmpty()) {
                skipBtn.setText(ConfigStringsManager.Companion.getStringById("done"));
                isScanFinished = true;
            }
        }
        else{
            skipBtn.setText(ConfigStringsManager.Companion.getStringById("done"));
        }
    }

    @Override
    public void scanSignalQualityChanged(int signalQuality) {
        super.scanSignalQualityChanged(signalQuality);
        signalQualityProgressView.setProgress(signalQuality);

    }

    @Override
    public void scanSignalStrengthChanged(int signalStrength) {
        super.scanSignalStrengthChanged(signalStrength);
        signalStrengthProgressView.setProgress(signalStrength);
    }

    @Override
    public void scanTuneFrequencyChanged(int frequency) {
        super.scanTuneFrequencyChanged(frequency);
        if (!isManualScan && frequency > tuneFrequency) {
            String descText = "Frequency: " + frequency + " kHz";
            scanningProgressDescriptionTv.setText(descText);
            tuneFrequency = frequency;
        }
    }

    @Override
    public void scanTvServiceNumber(int tvServiceNumber) {
        super.scanTvServiceNumber(tvServiceNumber);
        tvProgramsFound.setLeftText(String.valueOf(tvServiceNumber));
    }

    @Override
    public void scanRadioServiceNumber(int radioServiceNumber) {
        super.scanRadioServiceNumber(radioServiceNumber);
        radioProgramsFound.setLeftText(String.valueOf(radioServiceNumber));
    }
}
