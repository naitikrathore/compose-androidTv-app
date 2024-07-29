// ITvInputInterface.aidl
package com.cltv.mal.interfaces;

// Declare any non-default types here with import statements
import android.media.tv.TvInputInfo;
import com.cltv.mal.model.entities.TvEvent;
import com.cltv.mal.model.content_rating.ContentRatingSystem;
interface ITvInputInterface {
   TvInputInfo[] getTvInputList();
   TvInputInfo[] getTvInputFilteredList(in String filter);
   boolean startSetupActivity(in TvInputInfo input);
   void triggerScanCallback(in boolean isSuccessful);
   int getChannelCountForInput(in TvInputInfo input);
   boolean isParentalEnabled();
   ContentRatingSystem[] getContentRatingSystems();
   ContentRatingSystem[] getContentRatingSystemsList();
   String getContentRatingSystemDisplayName(in ContentRatingSystem contentRatingSystem);
   String getParentalRatingDisplayName(in String parentalRating, in TvEvent tvEvent);
   String getParentalRating(in String parentalRating);
}