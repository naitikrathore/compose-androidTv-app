package com.iwedia.cltv.automate

import android.app.Instrumentation
import android.util.Log
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.guide.android.tools.debug.debugViews.PerformanceHelper
import utils.information_bus.Event
import utils.information_bus.InformationBus

class FrameworkTestRunner : InstrumentationTestCaseRunner() {
    var suite: AutomaticTestHelper? = null

    init {
        suite = AutomaticTestHelper(this)
    }

    fun addTestCases(testClass: Testcase) {
        suite?.let { it.onAddTestCase(testClass) }
    }

    fun getAllTestCaseCount() {
        suite?.let { it.getTestCasesCount() }
    }

    fun startTest() {
        suite?.let {
            PerformanceHelper().startTracking()
            it.start()
        }
    }

    fun generateReport(){
        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
        Thread.sleep(2000)
        val isSuccessful = PerformanceHelper().stopTracking(this.context)
        if(isSuccessful){
            Log.d(Constants.LogTag.CLTV_TAG + "Automate test", "App Testing over successfully")
            InformationBus.submitEvent(Event(Events.TESTING_FINISHED,0))
        }
        else{
            InformationBus.submitEvent(Event(Events.TESTING_FINISHED,1))
            Log.d(Constants.LogTag.CLTV_TAG + "Automate test", "test complete: file not created")
        }
    }
}

open class InstrumentationTestCaseRunner : Instrumentation() {
    var instrumentation: Instrumentation? = null

    init {
        instrumentation = this
    }

     fun onKeyEvent(key: Int) {
        instrumentation?.let {
            sendKeyDownUpSync(key)
            Thread.sleep(2000)
        }
    }

     fun onRepeatKeyEvent(repeatCount: Int, keyEvent: Int) {
        for (j in 0 until repeatCount) {
            onKeyEvent(keyEvent)
        }
    }
}


interface ITestImpl {
    fun getName(): String
    fun getDescription(): String
    fun getStatus(): Boolean
}