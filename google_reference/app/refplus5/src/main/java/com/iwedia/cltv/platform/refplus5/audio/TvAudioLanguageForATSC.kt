package com.iwedia.cltv.platform.refplus5.audio

import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns.AUDIO_PRIMARY_LANGUAGE
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns.AUDIO_SECONDARY_LANGUAGE
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.AUDIO_TYPE_AUDIO_DESCRIPTION
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.AUDIO_TYPE_FOR_HARD_OF_HEARING
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.AUDIO_TYPE_NORMAL
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.AUDIO_TYPE_SPOKEN_SUBTITLE

class TvAudioLanguageForATSC {

  companion object {
    val audio_primary_lang = AUDIO_PRIMARY_LANGUAGE
    val audio_second_lang = AUDIO_SECONDARY_LANGUAGE
    val authority = Constants.AUTHORITY

    val audioType =
        arrayListOf(
            AUDIO_TYPE_NORMAL,
            AUDIO_TYPE_AUDIO_DESCRIPTION,
            AUDIO_TYPE_SPOKEN_SUBTITLE,
            AUDIO_TYPE_FOR_HARD_OF_HEARING,
            4)//Audio Description and spoken subtitle
  }
}
