package com.iwedia.cltv.anoki_fast.vod.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A class that handles debounce logic, ensuring an action is only executed
 * once after the last call within a specified interval.
 *
 * @property interval The debounce interval in milliseconds.
 */
class DebounceLast(private val interval: Long) {
    private var debounceJob: Job? = null
    //private var lastCallTime: Long = 0

    /**
     * Debounce the provided action, executing it only once after the last call within the specified interval.
     *
     * @param scope The CoroutineScope in which the debounce logic should run.
     * @param action The action to be performed after the debounce interval.
     */
    fun debounceLast(scope: CoroutineScope, action: () -> Unit) {
        //val currentTime = System.currentTimeMillis()
        //val timeSinceLastCall = currentTime - lastCallTime
        //Log.d("Denounce","$timeSinceLastCall")
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(interval)
            action()
        }
        //lastCallTime = currentTime
    }
}