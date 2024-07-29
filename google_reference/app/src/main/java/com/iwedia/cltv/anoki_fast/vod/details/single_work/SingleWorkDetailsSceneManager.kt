package com.iwedia.cltv.anoki_fast.vod.details.single_work

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneManagerBase
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface

class SingleWorkDetailsSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    playerInterface: PlayerInterface,
    networkInterface: NetworkInterface
) : DetailsSceneManagerBase(
    context,
    worldHandler,
    playerInterface,
    Type.SINGLE_WORK,
    networkInterface
)