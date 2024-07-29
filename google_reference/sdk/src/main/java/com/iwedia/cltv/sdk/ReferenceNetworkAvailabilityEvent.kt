package com.iwedia.cltv.sdk

import utils.information_bus.Event

/**
 * Neon network availability event
 *
 * This event is used only during initialization process
 *
 * @author Aleksandar Lazic
 */
class ReferenceNetworkAvailabilityEvent(isAvailable: Boolean?) : Event(ReferenceEvents.NETWORK_AVAILABILITY, isAvailable)