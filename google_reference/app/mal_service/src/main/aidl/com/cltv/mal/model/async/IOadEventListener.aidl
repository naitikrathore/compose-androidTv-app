// IOadEventListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
oneway interface IOadEventListener {
    void onFileFound(in int version);
    void onFileNotFound();
    void onScanStart();
    void onScanProgress(in int progress);
    void onDownloadStarted();
    void onDownloadProgress(in int progress);
    void onDownloadFail();
    void onDownloadSucess();
    void onUpgradeSuccess(in int version);
    void onNewestVersion();
}