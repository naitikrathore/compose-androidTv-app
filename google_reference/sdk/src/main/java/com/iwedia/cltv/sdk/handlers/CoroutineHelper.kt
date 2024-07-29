package com.iwedia.cltv.sdk.handlers

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object CoroutineHelper {

    fun runCoroutine(coroutineFun: () -> Unit, context: CoroutineContext = Dispatchers.Default): Job{
        return CoroutineScope(context).launch {
            coroutineFun.invoke()
        }
    }

    fun runCoroutineWithDelay(coroutineFun: () -> Unit, delayTime: Long = 5000, context: CoroutineContext = Dispatchers.Default): Job{
        return CoroutineScope(context).launch {
            delay(delayTime)
            coroutineFun.invoke()
        }
    }
}