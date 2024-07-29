package com.iwedia.cltv.automate

import android.util.Log
import com.iwedia.cltv.platform.model.Constants

open class Testcase : ITestImpl{
    private var status: Boolean = false;

    lateinit var frameworkTestHelper: AutomaticTestHelper
    lateinit var testCase: Testcase
    val TAG = javaClass.simpleName
    var testCaseName: String = ""
    var featureName : String = "";
    private var testCaseDesc: String = ""

    open fun onStart(autoTestHelper: AutomaticTestHelper?, frameworkTest: FrameworkTestRunner?) {
        status = false;
        autoTestHelper?.let { frameworkTestHelper = it }
    }

    open fun verifyTest() {}

    open fun onFinished()  {
        frameworkTestHelper?.let {  it.onUpdateTestCase(testCase, getStatus()); }
    }


    fun onDescUpdate(descMsg: String) {
        testCaseDesc = descMsg
    }

     fun updateStatus(implStatus : TestImplStatus) {
        when (implStatus){
            is TestImplStatus.TestCaseFinished -> {
                status = true
            }
            is TestImplStatus.TestCaseInterrupted -> {
                with(implStatus){
                    onDescUpdate(error)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateStatus: $error")
                }
                status = false
            }
        }
    }

    override fun getName(): String {
        return testCaseName
    }

    override fun getDescription(): String {
        return testCaseDesc
    }
    override fun getStatus(): Boolean {
        return status;
    }

 }


sealed interface TestImplStatus {
    class TestCaseFinished: TestImplStatus
    class TestCaseInterrupted (var error: String): TestImplStatus
}


