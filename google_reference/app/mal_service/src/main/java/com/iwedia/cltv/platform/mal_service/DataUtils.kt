package com.iwedia.cltv.platform.mal_service

import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.media.tv.TvTrackInfo
import android.media.tv.TvTrackInfo.TYPE_AUDIO
import android.media.tv.TvTrackInfo.TYPE_SUBTITLE
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.cltv.mal.interfaces.ILanguageMapperInterface
import com.cltv.mal.model.entities.AudioTrack
import com.cltv.mal.model.entities.DateTimeFormat
import com.cltv.mal.model.entities.SubtitleTrack
import com.cltv.mal.model.entities.SystemInfoData
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.mal_service.common.TrackBase
import com.iwedia.cltv.platform.mal_service.language.LanguageMapperBaseImpl
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.RecommendationItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

fun fromServiceChannel(serviceChannel: com.cltv.mal.model.entities.TvChannel): TvChannel {
    return TvChannel(
        serviceChannel.id ?: -1,
        serviceChannel.index ?: -1,
        serviceChannel.name ?: "",
        serviceChannel.logoImagePath ?: "",
        serviceChannel.channelUrl ?: "",
        if (serviceChannel.categoryIds != null) serviceChannel.categoryIds.toCollection(ArrayList()) else arrayListOf(),
        audioTracks = ArrayList(),
        subtitleTracks = ArrayList(),
        toVideoQuality(serviceChannel.videoQuality),
        if (serviceChannel.videoType != null) serviceChannel.videoType.toCollection(ArrayList()) else arrayListOf(),
        if (serviceChannel.audioType != null) serviceChannel.audioType.toCollection(ArrayList()) else arrayListOf(),
        serviceChannel.channelId ?: -1,
        serviceChannel.inputId ?: "",
        serviceChannel.serviceType ?: "",
        serviceChannel.displayNumber ?: "0",
        serviceChannel.lcn ?: 0,
        if (serviceChannel.favListIds != null) serviceChannel.favListIds.toCollection(ArrayList()) else arrayListOf(),
        serviceChannel.isRadioChannel ?: false,
        TunerType.getTunerTypeById(serviceChannel.tunerType ?: -1),
        serviceChannel.isSkipped ?: false,
        serviceChannel.isLocked ?: false,
        serviceChannel.ordinalNumber ?: -1,
        serviceChannel.tsId ?: 0,
        serviceChannel.onId ?: 0,
        serviceChannel.serviceId ?: 0,
        serviceChannel.internalId ?: -1,
        serviceChannel.isBrowsable ?: true,
        serviceChannel.appLinkText ?: "",
        serviceChannel.appLinkIntentUri ?: "",
        serviceChannel.appLinkIconUri ?: "",
        appLinkPosterUri = "",
        serviceChannel.packageName ?: "",
        if (serviceChannel.genres != null) serviceChannel.genres.toCollection(ArrayList()) else arrayListOf(),
        // this is where you put all SDK specific stuff related to service but not
        // according to TIF
        platformSpecific = null,
        serviceChannel.type,
        serviceChannel.providerFlag1,
        serviceChannel.providerFlag2,
        serviceChannel.providerFlag3,
        serviceChannel.providerFlag4
    )
}

fun toServiceChannel(tvChannel: TvChannel): com.cltv.mal.model.entities.TvChannel {
    var retChannel = com.cltv.mal.model.entities.TvChannel()
    retChannel.id = tvChannel.id
    retChannel.index = tvChannel.index
    retChannel.name = tvChannel.name
    retChannel.logoImagePath = tvChannel.logoImagePath
    retChannel.channelUrl = tvChannel.channelUrl
    retChannel.categoryIds = tvChannel.categoryIds.toIntArray()
    //retChannel.audioTracks = audioTracks = ArrayList()
    //retChannel.subtitleTracks = subtitleTracks = ArrayList()
    retChannel.videoQuality = fromVideoQuality(tvChannel.videoQuality)
    retChannel.videoType = tvChannel.videoType.toIntArray()
    retChannel.audioType = tvChannel.audioType.toIntArray()
    retChannel.channelId = tvChannel.channelId
    retChannel.inputId = tvChannel.inputId
    retChannel.serviceType = tvChannel.serviceType
    retChannel.displayNumber = tvChannel.displayNumber
    retChannel.lcn = tvChannel.lcn
    retChannel.favListIds = tvChannel.favListIds.toCollection(ArrayList())
    retChannel.isRadioChannel = tvChannel.isRadioChannel
    retChannel.tunerType = tvChannel.tunerType.ordinal
    retChannel.isSkipped = tvChannel.isSkipped
    retChannel.isLocked = tvChannel.isLocked
    retChannel.ordinalNumber = tvChannel.ordinalNumber
    retChannel.tsId = tvChannel.tsId
    retChannel.onId = tvChannel.onId
    retChannel.serviceId = tvChannel.serviceId
    retChannel.internalId = tvChannel.internalId
    retChannel.isBrowsable = tvChannel.isBrowsable
    retChannel.appLinkText = tvChannel.appLinkText
    retChannel.appLinkIntentUri = tvChannel.appLinkIntentUri
    retChannel.appLinkIconUri = tvChannel.appLinkIconUri
    retChannel.packageName = tvChannel.packageName
    retChannel.genres = tvChannel.genres.toTypedArray()
    retChannel.type = tvChannel.type
    retChannel.providerFlag1 = tvChannel.providerFlag1!!
    retChannel.providerFlag2 = tvChannel.providerFlag2!!
    retChannel.providerFlag3 = tvChannel.providerFlag3!!
    retChannel.providerFlag4 = tvChannel.providerFlag4!!
    return retChannel
}

fun toAudioTracks(): ArrayList<IAudioTrack> {
    return arrayListOf()
}

fun toVideoQuality(list: IntArray?): ArrayList<VideoResolution> {
    var result = arrayListOf<VideoResolution>()
    list?.forEach { item ->
        result.add(VideoResolution.getVideoResolutionById(item))
    }
    return result
}

fun fromVideoQuality(list: ArrayList<VideoResolution>): IntArray {
    var result = arrayListOf<Int>()
    list.forEach { item ->
        result.add(item.ordinal)
    }
    return result.toIntArray()
}

fun fromServiceCategory(serviceCategory: com.cltv.mal.model.entities.Category): Category {
    if (serviceCategory != null)
        return Category(serviceCategory.id, serviceCategory.name)
    else return Category(0, "")
}

fun fromServicePrefType(servicePrefType: com.cltv.mal.model.prefs.PrefType): PrefType {
    return PrefType.values()[servicePrefType.ordinal]
}

fun fromServicePrefMenu(servicePrefMenu: com.cltv.mal.model.prefs.PrefMenu): PrefMenu {
    return PrefMenu.values()[servicePrefMenu.ordinal]
}

fun fromServicePrefSubMenu(servicePrefSubMenu: com.cltv.mal.model.prefs.PrefSubMenu): PrefSubMenu {
    return PrefSubMenu.values()[servicePrefSubMenu.ordinal]
}

fun fromServiceTvEvent(serviceTvEvent: com.cltv.mal.model.entities.TvEvent): TvEvent {
    return TvEvent(
        id = serviceTvEvent.id,
        tvChannel = fromServiceChannel(serviceTvEvent.tvChannel),
        name = serviceTvEvent.name,
        shortDescription = serviceTvEvent.shortDescription,
        longDescription = serviceTvEvent.longDescription,
        imagePath = serviceTvEvent.imagePath,
        startTime = serviceTvEvent.startTime,
        endTime = serviceTvEvent.endTime,
        categories = if (serviceTvEvent.categories != null) serviceTvEvent.categories.toCollection(
            ArrayList()
        ) else arrayListOf(),
        parentalRate = serviceTvEvent.parentalRate,
        rating = serviceTvEvent.rating,
        tag = null,
        parentalRating = serviceTvEvent.parentalRatingString,
        isProgramSame = serviceTvEvent.isProgramSame,
        isInitialChannel = serviceTvEvent.isInitialChannel,
        providerFlag = serviceTvEvent.providerFlag,
        genre = serviceTvEvent.genre,
        subGenre = serviceTvEvent.subGenre,
        tvEventId = serviceTvEvent.id
    )
}

fun toServiceTvEvent(tvEvent: TvEvent): com.cltv.mal.model.entities.TvEvent {
    var resultTvEvent = com.cltv.mal.model.entities.TvEvent()
    resultTvEvent.id = tvEvent.id
    resultTvEvent.tvChannel = toServiceChannel(tvEvent.tvChannel)
    resultTvEvent.name = tvEvent.name
    resultTvEvent.shortDescription = tvEvent.shortDescription
    resultTvEvent.longDescription = tvEvent.longDescription
    resultTvEvent.imagePath = tvEvent.imagePath
    resultTvEvent.startTime = tvEvent.startTime
    resultTvEvent.endTime = tvEvent.endTime
    resultTvEvent.categories =
        if (tvEvent.categories != null) tvEvent.categories!!.toIntArray() else IntArray(0)
    resultTvEvent.parentalRate = tvEvent.parentalRate
    resultTvEvent.rating = tvEvent.rating
    resultTvEvent.parentalRatingString = tvEvent.parentalRating
    resultTvEvent.isProgramSame = tvEvent.isProgramSame
    resultTvEvent.isInitialChannel = tvEvent.isInitialChannel
    resultTvEvent.providerFlag = if (tvEvent.providerFlag != null) tvEvent.providerFlag!! else 0
    resultTvEvent.genre = tvEvent.genre
    resultTvEvent.subGenre = tvEvent.subGenre
    return resultTvEvent
}

fun toServiceFavoriteItem(item: FavoriteItem): com.cltv.mal.model.entities.FavoriteItem {
    return com.cltv.mal.model.entities.FavoriteItem(
        item.id,
        toServiceChannel(item.tvChannel),
        item.favListIds
    )
}

fun fromServiceFavoriteItem(item: com.cltv.mal.model.entities.FavoriteItem): FavoriteItem {
    return FavoriteItem(
        item.id,
        FavoriteItemType.TV_CHANNEL,
        null,
        fromServiceChannel(item.tvChannel),
        item.favListIds
    )
}

fun toServiceRecording(recording: Recording): com.cltv.mal.model.pvr.Recording {
    return com.cltv.mal.model.pvr.Recording(
        recording.id,
        recording.name,
        recording.duration,
        recording.recordingDate,
        recording.image,
        recording.videoUrl,
        toServiceChannel(recording.tvChannel!!),
        toServiceTvEvent(recording.tvEvent!!),
        recording.recordingStartTime,
        recording.recordingEndTime,
        recording.shortDescription
    )
}

fun fromServiceRecording(recording: com.cltv.mal.model.pvr.Recording): Recording {
    return Recording(
        recording.id,
        recording.name,
        recording.duration,
        recording.recordingDate,
        recording.image,
        recording.videoUrl,
        fromServiceChannel(recording.tvChannel!!),
        fromServiceTvEvent(recording.tvEvent!!),
        recording.recordingStartTime,
        recording.recordingEndTime,
        recording.shortDescription,
        recording.shortDescription,
        ""
    )
}

fun fromServiceRecordingInProgress(recordingInProgress: com.cltv.mal.model.pvr.RecordingInProgress): RecordingInProgress {
    return RecordingInProgress(
        recordingInProgress.id,
        recordingInProgress.recordingStart,
        recordingInProgress.recordingEnd,
        fromServiceChannel(recordingInProgress.tvChannel),
        fromServiceTvEvent(recordingInProgress.tvEvent)
    )
}

fun fromServiceScheduledRecording(scheduledRecording: com.cltv.mal.model.entities.ScheduledRecording): ScheduledRecording {
    return ScheduledRecording(
        scheduledRecording.id,
        scheduledRecording.name,
        scheduledRecording.scheduledDateStart,
        scheduledRecording.scheduledDateEnd,
        scheduledRecording.tvChannelId,
        scheduledRecording.tvEventId,
        RepeatFlag.values()[scheduledRecording.repeatFlag.ordinal],
        fromServiceChannel(scheduledRecording.tvChannel),
        fromServiceTvEvent(scheduledRecording.tvEvent)
    )
}

fun toServiceScheduledRecording(scheduledRecording: ScheduledRecording): com.cltv.mal.model.entities.ScheduledRecording {
    return com.cltv.mal.model.entities.ScheduledRecording(
        scheduledRecording.id,
        scheduledRecording.name,
        scheduledRecording.scheduledDateStart,
        scheduledRecording.scheduledDateEnd,
        scheduledRecording.tvChannelId,
        scheduledRecording.tvEventId!!,
        com.cltv.mal.model.pvr.RepeatFlag.values()[scheduledRecording.repeatFreq.ordinal],
        toServiceChannel(scheduledRecording.tvChannel!!),
        toServiceTvEvent(scheduledRecording.tvEvent!!)
    )
}

fun fromServiceScheduledReminder(scheduledReminder: com.cltv.mal.model.entities.ScheduledReminder): ScheduledReminder {
    return ScheduledReminder(
        scheduledReminder.id,
        scheduledReminder.name,
        fromServiceChannel(scheduledReminder.tvChannel),
        scheduledReminder.tvEvent?.let { fromServiceTvEvent(it) },
        scheduledReminder.startTime,
        scheduledReminder.tvChannelId,
        scheduledReminder.tvEventId
    )
}

fun toServiceScheduledReminder(scheduledReminder: ScheduledReminder): com.cltv.mal.model.entities.ScheduledReminder {
    return com.cltv.mal.model.entities.ScheduledReminder(
        scheduledReminder.id,
        scheduledReminder.name,
        toServiceChannel(scheduledReminder.tvChannel!!),
        toServiceTvEvent(scheduledReminder.tvEvent!!),
        scheduledReminder.startTime!!,
        scheduledReminder.tvChannelId!!,
        scheduledReminder.tvEventId!!
    )
}

fun fromServiceRailItem(railItem: com.cltv.mal.model.entities.RailItem): RailItem {
    val railItems = mutableListOf<TvEvent>()
    railItem.rail.forEach { item ->
        item?.let {
            railItems.add(fromServiceTvEvent(it))
        }
    }
    return RailItem(
        id = railItem.id,
        railName = railItem.railName,
        rail = railItems as MutableList<Any>,
        type = RailItem.RailItemType.values()[railItem.type]
    )
}

fun fromServiceContentRatingSystem(contentRatingSystem: com.cltv.mal.model.content_rating.ContentRatingSystem): ContentRatingSystem {
    val ratings = arrayListOf<ContentRatingSystem.Rating>()
    contentRatingSystem.ratings.forEach {
        it?.let {
            ratings.add(fromServiceContentSystemRating(it))
        }
    }
    val subRatings = arrayListOf<ContentRatingSystem.SubRating>()
    contentRatingSystem.subRatings.forEach {
        it?.let {
            subRatings.add(fromServiceContentRatingSystemSubRating(it))
        }
    }
    val orders = arrayListOf<ContentRatingSystem.Order>()
    contentRatingSystem.orders.forEach {
        it?.let {
            orders.add(fromServiceContentRatingSystemOrder(it))
        }
    }
    return ContentRatingSystem(
        contentRatingSystem.name,
        contentRatingSystem.domain,
        contentRatingSystem.title,
        contentRatingSystem.description,
        contentRatingSystem.countries,
        contentRatingSystem.displayName,
        ratings,
        subRatings,
        orders,
        contentRatingSystem.isCustom
    )
}

fun fromServiceContentSystemRating(serviceRating: com.cltv.mal.model.content_rating.ContentRatingSystem.Rating): ContentRatingSystem.Rating {
    val rating = ContentRatingSystem.Rating.Builder()
    rating.setName(serviceRating.name)
    rating.setTitle(serviceRating.title)
    rating.setDescription(serviceRating.description)
    rating.setContentAgeHint(serviceRating.ageHint)
    rating.setIcon(serviceRating.icon)
    serviceRating.subRatings.forEach {
        it?.let {
            rating.addSubRatingName(it.name)
        }
    }
    val subRatings = arrayListOf<ContentRatingSystem.SubRating>()
    serviceRating.subRatings.forEach {
        it?.let {
            subRatings.add(fromServiceContentRatingSystemSubRating(it))
        }
    }
    return rating.build(subRatings)
}

fun fromServiceContentRatingSystemSubRating(serviceSubRating: com.cltv.mal.model.content_rating.ContentRatingSystem.SubRating): ContentRatingSystem.SubRating {
    val subRating = ContentRatingSystem.SubRating.Builder()
    subRating.setName(serviceSubRating.name)
    subRating.setTitle(serviceSubRating.title)
    subRating.setDescription(serviceSubRating.description)
    subRating.setIcon(serviceSubRating.icon)
    return subRating.build()
}

fun fromServiceContentRatingSystemOrder(serviceOrder: com.cltv.mal.model.content_rating.ContentRatingSystem.Order): ContentRatingSystem.Order {
    val order = ContentRatingSystem.Order.Builder()
    serviceOrder.ratingOrder.forEach {
        it?.let {
            order.addRatingName(it.name)
        }
    }
    val ratings = arrayListOf<ContentRatingSystem.Rating>()
    serviceOrder.ratingOrder.forEach {
        it?.let {
            ratings.add(fromServiceContentSystemRating(it))
        }
    }
    return order.build(ratings)
}

fun toServiceContentRating(contentRating: TvContentRating): com.cltv.mal.model.content_rating.TvContentRating {
    return com.cltv.mal.model.content_rating.TvContentRating(
        if(contentRating.domain == null) "" else contentRating.domain,
        if(contentRating.ratingSystem == null) "" else contentRating.ratingSystem,
        if(contentRating.mainRating == null) "" else contentRating.mainRating,
        if(contentRating.subRatings == null) arrayListOf<String>().toTypedArray() else contentRating.subRatings.toTypedArray(),
        contentRating.hashCode()
    )
}

fun fromServiceContentRating(contentRating: com.cltv.mal.model.content_rating.TvContentRating): TvContentRating {
    return TvContentRating.createRating(
        contentRating.domain, contentRating.ratingSystem, contentRating.mainRating,
        contentRating.subRatings.get(0)
    )
}

fun toServiceContentRatingSystem(contentRatingSystem: ContentRatingSystem): com.cltv.mal.model.content_rating.ContentRatingSystem {
    val ratings = arrayListOf<com.cltv.mal.model.content_rating.ContentRatingSystem.Rating>()
    contentRatingSystem.ratings.forEach {
        it?.let {
            ratings.add(toServiceContentSystemRating(it))
        }
    }
    val subRatings = arrayListOf<com.cltv.mal.model.content_rating.ContentRatingSystem.SubRating>()
    contentRatingSystem.subRatings.forEach {
        it?.let {
            subRatings.add(toServiceContentRatingSystemSubRating(it))
        }
    }
    val orders = arrayListOf<com.cltv.mal.model.content_rating.ContentRatingSystem.Order>()
    contentRatingSystem.orders.forEach {
        it?.let {
            orders.add(toServiceContentRatingSystemOrder(it))
        }
    }
    return com.cltv.mal.model.content_rating.ContentRatingSystem(
        contentRatingSystem.name,
        contentRatingSystem.domain,
        contentRatingSystem.title,
        contentRatingSystem.description,
        contentRatingSystem.countries,
        contentRatingSystem.displayName,
        ratings,
        subRatings,
        orders,
        contentRatingSystem.isCustom
    )
}

fun toServiceContentSystemRating(rating: ContentRatingSystem.Rating): com.cltv.mal.model.content_rating.ContentRatingSystem.Rating {
    val serviceRating = com.cltv.mal.model.content_rating.ContentRatingSystem.Rating.Builder()
    serviceRating.setName(rating.name)
    serviceRating.setTitle(rating.title)
    serviceRating.setDescription(rating.description)
    serviceRating.setContentAgeHint(rating.ageHint)
    serviceRating.setIcon(rating.icon)
    rating.subRatings.forEach {
        it?.let {
            serviceRating.addSubRatingName(it.name)
        }
    }
    val subRatings = arrayListOf<com.cltv.mal.model.content_rating.ContentRatingSystem.SubRating>()
    rating.subRatings.forEach {
        it?.let {
            subRatings.add(toServiceContentRatingSystemSubRating(it))
        }
    }
    return serviceRating.build(subRatings)
}

fun toServiceContentRatingSystemSubRating(subRating: ContentRatingSystem.SubRating): com.cltv.mal.model.content_rating.ContentRatingSystem.SubRating {
    var serviceSubRating = com.cltv.mal.model.content_rating.ContentRatingSystem.SubRating.Builder()
    serviceSubRating.setName(subRating.name)
    serviceSubRating.setTitle(subRating.title)
    serviceSubRating.setDescription(subRating.description)
    serviceSubRating.setIcon(subRating.icon)
    return serviceSubRating.build()
}

fun toServiceContentRatingSystemOrder(order: ContentRatingSystem.Order): com.cltv.mal.model.content_rating.ContentRatingSystem.Order {
    val serviceOrder = com.cltv.mal.model.content_rating.ContentRatingSystem.Order.Builder()
    order.ratingOrder.forEach {
        it?.let {
            serviceOrder.addRatingName(it.name)
        }
    }
    val ratings = arrayListOf<com.cltv.mal.model.content_rating.ContentRatingSystem.Rating>()
    order.ratingOrder.forEach {
        it?.let {
            ratings.add(toServiceContentSystemRating(it))
        }
    }
    return serviceOrder.build(ratings)
}

fun toServiceInputSourceData(inputSourceData: InputSourceData): com.cltv.mal.model.entities.InputSourceData {
    return com.cltv.mal.model.entities.InputSourceData(
        inputSourceData.inputSourceName,
        inputSourceData.hardwareId,
        inputSourceData.inputMainName
    )
}

fun fromServiceInputSourceData(inputSourceData: com.cltv.mal.model.entities.InputSourceData): InputSourceData {
    return InputSourceData(
        inputSourceData.inputSourceName,
        inputSourceData.hardwareId,
        inputSourceData.inputMainName
    )
}

fun fromServicePromotionItem(promotionItem: com.cltv.mal.model.fast.PromotionItem): PromotionItem {
    return PromotionItem(
        promotionItem.type,
        promotionItem.banner,
        promotionItem.logo,
        promotionItem.callToAction,
        promotionItem.clickUrl,
        promotionItem.channelId,
        promotionItem.contentId
    )
}

fun fromServiceRecommendationItem(recommendationItem: com.cltv.mal.model.fast.RecommendationItem): RecommendationItem {
    return RecommendationItem(
        if (recommendationItem.type != null) recommendationItem.type else "",
        if (recommendationItem.title != null) recommendationItem.title else "",
        if (recommendationItem.thumbnail != null) recommendationItem.thumbnail else "",
        if (recommendationItem.description != null) recommendationItem.description else "",
        if (recommendationItem.playbackUrl != null) recommendationItem.playbackUrl else "",
        if (recommendationItem.channelId != null ) recommendationItem.channelId else "",
        recommendationItem.startTimeEpoch,
        recommendationItem.durationSec ,
        if (recommendationItem.rating != null) recommendationItem.rating else "",
        if (recommendationItem.genre != null) recommendationItem.genre else "",
        if (recommendationItem.language != null) recommendationItem.language else "",
        if (recommendationItem.previewUrl != null) recommendationItem.previewUrl else "",
        recommendationItem.previewUrlSkipSec,
        if (recommendationItem.contentId != null) recommendationItem.contentId else "0"
    )
}

fun toServicePromotionItem(promotionItem: PromotionItem): com.cltv.mal.model.fast.PromotionItem {
    return com.cltv.mal.model.fast.PromotionItem(
        promotionItem.type,
        promotionItem.banner,
        promotionItem.logo,
        promotionItem.callToAction,
        promotionItem.clickUrl,
        promotionItem.channelid!!
    )
}

fun toServiceSubtitleTrack(subtitle: ISubtitle): SubtitleTrack {
    var track = SubtitleTrack()
    track.id = subtitle!!.trackId
    track.trackName = subtitle!!.trackName
    track.languageName = subtitle!!.languageName
    track.isHoh = subtitle.isHoh
    return track
}

fun toServiceAudioTrack(audioTrack: IAudioTrack): AudioTrack {
    var track = AudioTrack()
    track.id = audioTrack!!.trackId
    track.trackName = audioTrack!!.trackName
    track.languageName = audioTrack!!.languageName
    track.isAnalogTrack = audioTrack!!.isAnalogTrack
    track.isAd = audioTrack.isAd
    track.isDolby = audioTrack.isDolby
    return track
}

fun fromServiceSubtitleTrack(
    subtitleTrack: SubtitleTrack,
    utilsInterface: UtilsInterface
): ISubtitle? {
    if (subtitleTrack.id != null) {
        var trackInfo = TvTrackInfo.Builder(TYPE_SUBTITLE, subtitleTrack.id)
            .setLanguage(subtitleTrack.languageName)
            .build()
        return TrackBase.SubtitleTrack(trackInfo, utilsInterface)
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.R)
fun fromServiceAudioTrack(audioTrack: AudioTrack, utilsInterface: UtilsInterface): IAudioTrack? {
    try {
        var trackInfo = TvTrackInfo.Builder(TYPE_AUDIO, audioTrack.id)
            .setLanguage(audioTrack.languageName)
            .setAudioDescription(audioTrack.isAd).build()
        return TrackBase.AudioTrack(
            trackInfo,
            utilsInterface,
            audioTrack.isAnalogTrack,
            audioTrack.analogName
        )
    } catch (e: Exception) {

    }
    return null
}

fun fromServiceRegion(region: com.cltv.mal.model.entities.Region): Region {
    return Region.values()[region.ordinal]
}

fun fromServiceSystemInfoData(systemInfoData: SystemInfoData) : com.iwedia.cltv.platform.model.SystemInfoData{
    return com.iwedia.cltv.platform.model.SystemInfoData(
        displayNumber = systemInfoData.getDisplayNumber(),
        displayName = systemInfoData.getDisplayName(),
        providerData = systemInfoData.getProviderData(),
        logoImagePath = systemInfoData.getLogoImagePath(),
        isRadioChannel = systemInfoData.isRadioChannel(),
        isSkipped = systemInfoData.isSkipped(),
        isLocked = systemInfoData.isLocked(),
        tunerType = systemInfoData.getTunerType(),
        ordinalNumber = systemInfoData.getOrdinalNumber(),
        frequency = systemInfoData.getFrequency(),
        tsId = systemInfoData.getTsId(),
        onId = systemInfoData.getOnId(),
        serviceId = systemInfoData.getServiceId(),
        bandwidth = systemInfoData.getBandwidth(),
        networkId = systemInfoData.getNetworkId(),
        networkName = systemInfoData.getNetworkName(),
        postViterbi = systemInfoData.getPostViterbi(),
        attr5s = systemInfoData.getAttr5s(),
        signalQuality = systemInfoData.getSignalQuality(),
        signalStrength = systemInfoData.getSignalStrength(),
        signalBer = systemInfoData.getSignalBer(),
        signalAGC = systemInfoData.getSignalAGC(),
        signalUEC = systemInfoData.getSignalUEC()
    )
}
fun fromServiceDateTimeFormat(dateTimeFormat: DateTimeFormat): com.iwedia.cltv.platform.model.DateTimeFormat {
    return com.iwedia.cltv.platform.model.DateTimeFormat(
        dateTimeFormat.datePattern,
        dateTimeFormat.timePattern,
        dateTimeFormat.dateTimePattern
    )
}

fun fromServiceLanguageMapper(languageMapper: ILanguageMapperInterface): LanguageMapperInterface {
    return LanguageMapper(languageMapper)
}

class LanguageMapper(private val languageMapper: ILanguageMapperInterface) :
    LanguageMapperBaseImpl() {

    override fun getLanguageName(languageCode: String): String? {
        return languageMapper.getLanguageName(languageCode)
    }

    override fun getPreferredLanguageName(languageCode: String): String? {
        return languageMapper.getPreferredLanguageName(languageCode)
    }

    override fun getLanguageCodeByCountryCode(countryCode: String?): String? {
        return languageMapper.getLanguageCodeByCountryCode(countryCode)
    }

    override fun getTxtDigitalLanguageMapByCountryCode(countryCode: String?): Int? {
        return languageMapper.getTxtDigitalLanguageMapByCountryCode(countryCode)
    }

    override fun getTxtDigitalLanguageMapByPosition(position: Int?): Int? {
        return languageMapper.getTxtDigitalLanguageMapByPosition(position!!)
    }

    override fun getLanguageCodes(): MutableList<LanguageCode> {
        var list = mutableListOf<LanguageCode>()
        languageMapper.languageCodes.forEach {
            list.add(
                LanguageCode(
                    it.languageCodeISO6392,
                    it.languageCodeISO6391,
                    it.englishName,
                    it.germanName,
                    it.frenchName
                )
            )
        }
        return list
    }

    override fun getDefaultLanguageCode(): LanguageCode {
        return LanguageCode(
            languageMapper.defaultLanguageCode.languageCodeISO6392,
            languageMapper.defaultLanguageCode.languageCodeISO6391,
            languageMapper.defaultLanguageCode.englishName,
            languageMapper.defaultLanguageCode.germanName,
            languageMapper.defaultLanguageCode.frenchName
        )
    }

    override fun getLanguageCode(trackLanguage: String): String {
        return languageMapper.getLanguageCode(trackLanguage)
    }
}
