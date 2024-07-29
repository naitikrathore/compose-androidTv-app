package com.iwedia.cltv.scan_activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.iwedia.cltv.R;
import com.iwedia.cltv.config.ConfigStringsManager;

import java.util.zip.Inflater;


public class IwediaSetupActivity extends FragmentActivity {

    //TO DO: see why functionality does not work when SIGNAL_EVENT_STOP is moved to ScanHelper
    public static final String SIGNAL_EVENT_STOP = "com.google.android.tv.dtvscan.SIGNAL_EVENT_STOP";

    /**
     * Active fragment instance
     */
    private GenericFragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.iwedia_setup_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Show tv input main fragment
        if (this.activeFragment == null) {
            showFragment(new MainFragment());
            //Init scan handler
//            ScanHandler.init(this, TvService.class);
//            ScanHandler.getInstance().registerScanningCallback(new ScanManager.ScanningCallback() {
//                @Override
//                public void progressChanged(final int progress) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.updateProgress(progress);
//                        }
//                    });
//                }
//
//                @Override
//                public void signalQualityChanged(final int signalQuality) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.scanSignalQualityChanged(signalQuality);
//                        }
//                    });
//                }
//
//                @Override
//                public void signalStrengthChanged(final int signalStrength) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.scanSignalStrengthChanged(signalStrength);
//                        }
//                    });
//                }
//
//                @Override
//                public void scanFinished() {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.scanFinished();
//                        }
//                    });
//                }
//
//                @Override
//                public void scanTuneFrequencyChanged(final int freq) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.scanTuneFrequencyChanged(freq);
//                        }
//                    });
//                }
//
//                @Override
//                public void scanTvServiceNumber(final int number) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.scanTvServiceNumber(number);
//                        }
//                    });
//                }
//
//                @Override
//                public void scanRadioServiceNumber(final int number) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activeFragment.scanRadioServiceNumber(number);
//                        }
//                    });
//                }
//            });
        }
    }

    public void finishScan() {
        ScanHelper.INSTANCE.stopScan();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        ScanHandler.getInstance().deinit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (activeFragment != null) {
            return activeFragment.dispatchKeyEvent(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (activeFragment != null) {
            return activeFragment.dispatchKeyEvent(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Show fragment
     *
     * @param fragment
     */
    public void showFragment(GenericFragment fragment) {
        //Remove previous fragment
        if (this.activeFragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            transaction.remove(this.activeFragment).commitAllowingStateLoss();
            if (this.activeFragment instanceof TerrestrialManualScanFragment) {
                ScanHelper.INSTANCE.unregisterReceiver();
            }
        }
        this.activeFragment = fragment;
        ScanHelper.INSTANCE.setActiveFragment(fragment);
        if (this.activeFragment instanceof TerrestrialManualScanFragment) {
            ScanHelper.INSTANCE.registerReceiver();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        if (fragment.isAdded()) {
            transaction.show(fragment).commitAllowingStateLoss();
            fragment.onResume();
        } else {
            transaction.add(R.id.uiView, fragment).commitAllowingStateLoss();
        }
    }

    /**
     * Hide fragment
     *
     * @param fragment      fragment to hide
     * @param nextFragment  next fragment to show
     */
    public void hideFragment(GenericFragment fragment, GenericFragment nextFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.hide(fragment).commitAllowingStateLoss();
        fragment.onPause();
        this.activeFragment = nextFragment;
        FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
        transaction1.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction1.add(R.id.uiView, nextFragment).commitAllowingStateLoss();
    }
}
