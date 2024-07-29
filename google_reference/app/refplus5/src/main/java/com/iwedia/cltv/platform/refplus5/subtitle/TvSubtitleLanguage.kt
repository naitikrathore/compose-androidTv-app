package com.iwedia.cltv.platform.refplus5.subtitle

import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_SUBTITLE
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_SUBTITLE_PREFERRED_LANGUAGE_PRIMARY
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_SUBTITLE_PREFERRED_LANGUAGE_SECONDARY

class TvSubtitleLanguage {
  companion object {
    val digital_subtitle = COLUMN_SUBTITLE
    val subtitle_primary_langauge = COLUMN_SUBTITLE_PREFERRED_LANGUAGE_PRIMARY
    val subtitle_second_langauge = COLUMN_SUBTITLE_PREFERRED_LANGUAGE_SECONDARY
    val subtitle_type = "SUBTITLE_TYPE"
    val authority = Constants.AUTHORITY
  }
}
