package com.iwedia.cltv.platform.model

import kotlinx.coroutines.*
import java.util.Collections
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine helper class
 *
 * @author Dejan Nadj
 */
object CoroutineHelper {

    private var jobList = Collections.synchronizedList(mutableListOf<Job>())

    fun runCoroutine(coroutineFun: () -> Unit, context: CoroutineContext = Dispatchers.Default): Job{
        val job = CoroutineScope(context).launch {
            coroutineFun.invoke()
        }
        synchronized(jobList) {
            jobList.add(job)
        }
        return job
    }

    /**
    * Method for running coroutine function for suspend functions
    */
    fun runCoroutineForSuspend(coroutineFun: suspend () -> Unit, context: CoroutineContext = Dispatchers.Default): Job{
        val job = CoroutineScope(context).launch {
            coroutineFun.invoke()
        }
        synchronized(jobList) {
            jobList.add(job)
        }
        return job
    }

    private suspend fun suspendAsync(coroutineFun: () -> Unit, context: CoroutineContext) {
        withContext(context){
            coroutineFun.invoke()
        }
    }

    fun runCoroutineWithDelay(coroutineFun: () -> Unit, delayTime: Long = 5000, context: CoroutineContext = Dispatchers.Default): Job{
        val job = CoroutineScope(context).launch {
            delay(delayTime)
            coroutineFun.invoke()
        }
        synchronized(jobList) {
            jobList.add(job)
        }
        return job
    }

    fun cleanUp() {
        synchronized(jobList) {
            val indexToRemove = arrayListOf<Int>()
            jobList.forEachIndexed { index, job ->
                if (!job.isActive) {
                    job.cancel()
                    indexToRemove.add(index)
                }
            }
            indexToRemove.forEach {
                if (it < jobList.size)
                    jobList.removeAt(it)
            }
        }
    }
}