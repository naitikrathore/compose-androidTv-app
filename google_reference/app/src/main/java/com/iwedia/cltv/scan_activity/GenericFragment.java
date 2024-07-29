package com.iwedia.cltv.scan_activity;


import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

/**
 * Generic tv input setup activity fragment
 *
 * @author Dejan Nadj
 */
public class GenericFragment extends Fragment {

    /**
     * Previous fragment
     */
    protected GenericFragment previousFragment;

    /**
     * Focused view
     */
    private View focused;

    /**
     * Dispatch ket event
     *
     * @param keyCode   event key code
     * @param keyEvent  event type(up, down, repeat)
     * @return  is key event handled
     */
    public boolean dispatchKeyEvent(int keyCode, KeyEvent keyEvent) {
        return false;
    }

    /**
     * Set previous fragment
     *
     * @param previousFragment
     */
    public void setPreviousFragment(GenericFragment previousFragment) {
        this.previousFragment = previousFragment;
    }

    /**
     * Update progress
     * @param progress
     */
    public void updateProgress(int progress) {
        //TODO override and implement
    }

    /**
     * Scan finished
     */
    public void scanFinished() {
        //TODO override and implement
    }

    /**
     * Scan signal strength changed
     *
     * @param signalStrength
     */
    public void scanSignalStrengthChanged(int signalStrength) {
        //TODO override and implement
    }

    /**
     * Scan signal quality changed
     *
     * @param signalQuality
     */
    public void scanSignalQualityChanged(int signalQuality) {
        //TODO override and implement
    }

    /**
     * Scan tune frequency changed
     *
     * @param frequency
     */
    public void scanTuneFrequencyChanged(int frequency) {
        //TODO override and implement
    }

    /**
     * Scan tv service number
     *
     * @param number
     */
    public void scanTvServiceNumber(int number) {
        //TODO override and implement
    }

    /**
     * Scan radio service number
     *
     * @param number
     */
    public void scanRadioServiceNumber(int number) {
        //TODO override and implement
    }

    @Override
    public void onPause() {
        super.onPause();
        focused = getView().findFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (focused != null) {
            focused.requestFocus();
        }
    }
}
