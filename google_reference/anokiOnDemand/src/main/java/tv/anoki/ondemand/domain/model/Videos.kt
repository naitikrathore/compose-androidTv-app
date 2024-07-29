package tv.anoki.ondemand.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.annotation.concurrent.Immutable

@Immutable
@Parcelize
data class Videos(
    val title: String = "",
    val description: String = "",
    val subtitle: String = "",
    val mediaID: String = "",
    val channelId: String = "",
    val type: String = "",
    var imageURL: String = "",
    val videoURL: Uri? = null,
    val currentIndex: Int = -1,
    val programTime: String = "",
    val loading: Boolean = true,
    val resume: Int = 0,
    val origRating: String = "",

    val _id: String = UUID.randomUUID().toString()
) : Parcelable