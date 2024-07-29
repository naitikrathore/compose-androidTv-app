package com.iwedia.cltv.platform.gretzky

import android.content.Context
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.base.FavoritesInterfaceBaseImpl
import com.iwedia.cltv.platform.base.content_provider.FavoriteDataProvider
import com.iwedia.cltv.platform.`interface`.UtilsInterface


internal class FavoritesInterfaceImpl(
    applicationContext: Context,
    channelDataProvider: ChannelDataProviderInterface,
    utilsInterface: UtilsInterface
) :
    FavoritesInterfaceBaseImpl(applicationContext, channelDataProvider, utilsInterface) {
}