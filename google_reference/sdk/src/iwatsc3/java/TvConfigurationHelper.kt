import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.entities.LanguageCodeMapper

class TvConfigurationHelper {

    companion object {

        const val CODEC_AUDIO_AC3 = "ac3"
        const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        const val CODEC_AUDIO_EAC3 = "eac3"
        const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        const val CODEC_AUDIO_DTS = "dts"

        fun setDeafaultLanguages() {
        }

        fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean{
            return false
        }

        fun getSubtitlesEnabled(): Boolean {
            return false
        }

        fun isDolby(tvTrackInfo: TvTrackInfo): Boolean{
            return false
        }
    }

}