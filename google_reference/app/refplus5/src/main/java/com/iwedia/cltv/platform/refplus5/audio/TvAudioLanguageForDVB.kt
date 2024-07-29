package com.iwedia.cltv.platform.refplus5.audio

import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUDIO_TYPE_AUDIO_DESCRIPTION
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUDIO_TYPE_FOR_HARD_OF_HEARING
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUDIO_TYPE_NORMAL
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUDIO_TYPE_SPOKEN_SUBTITLES
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_AUDIO_PREFERRED_LANGUAGE_PRIMARY
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_AUDIO_PREFERRED_LANGUAGE_SECONDARY
class TvAudioLanguageForDVB {

  companion object {
      val audio_primary_lang = COLUMN_AUDIO_PREFERRED_LANGUAGE_PRIMARY
      val audio_second_lang = COLUMN_AUDIO_PREFERRED_LANGUAGE_SECONDARY
      val authority = Constants.AUTHORITY

      val audioType =
        arrayListOf(
            AUDIO_TYPE_NORMAL,
            AUDIO_TYPE_AUDIO_DESCRIPTION,
            AUDIO_TYPE_SPOKEN_SUBTITLES,
            AUDIO_TYPE_FOR_HARD_OF_HEARING,
            4)//Audio Description and spoken subtitle
  }
}
