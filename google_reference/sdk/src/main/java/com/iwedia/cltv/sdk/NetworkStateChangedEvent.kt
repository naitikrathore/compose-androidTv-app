package com.iwedia.cltv.sdk

import com.iwedia.cltv.sdk.handlers.ReferenceNetworkHandler
import utils.information_bus.Event

/**
 * Network state changed event
 *
 * @author Dragan Krnjaic
 */
class NetworkStateChangedEvent(wifiStateType: ReferenceNetworkHandler.WifiStateType?) : Event(ReferenceEvents.NETWORK_STATE_CHANGED_EVENT, wifiStateType)