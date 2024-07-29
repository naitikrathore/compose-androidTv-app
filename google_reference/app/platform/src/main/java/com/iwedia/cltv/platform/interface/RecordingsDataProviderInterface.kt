package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.recording.Recording

interface RecordingsDataProviderInterface {
    fun getRecordings(): List<Recording>
    fun loadRecordings()
    fun deleteRecording(recording: Recording, callback: IAsyncCallback)
    fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback)
    fun updateRecording(
        duration: Long?,
        name: String?,
        shortDescription: String?,
        longDescription: String?,
        parentalRating: String?,
        genre: String?,
        subGenre: String?,
        resolution: String?
    )

    fun deleteAllRecordings(callback: IAsyncCallback)
}