// ICiPlusListener.aidl
package com.cltv.mal.model.async;

import com.cltv.mal.model.ci_plus.Enquiry;
import com.cltv.mal.model.ci_plus.CiPlusCamMenu;
import com.cltv.mal.model.ci_plus.CachedPinResult;
oneway interface ICiPlusListener {
    void onEnquiryReceived(in Enquiry enquiry);
    void onMenuReceived(in CiPlusCamMenu menu);
    void showCiPopup();
    void closePopup();
    void onPinResult(in CachedPinResult result);
    void onStartScanReceived(in String profileName);
    void onCICardEventInserted();
    void onCICardEventRemoved();
}