package com.iwedia.cltv.am

import com.iwedia.cltv.automate.*
import java.lang.Exception


open class TestZapper() : Testcase() {

    var frameworkTest: FrameworkTestRunner? = null

    /*
     * Update current Testcase,
     * and initialize testCaseName
     * */
    init {
        testCase = this;
        testCaseName = "TestZapper"
    }

    /*
    * This method is responsible for enter into Testcase to test all features
    * */
    override fun onStart(autoTestHelper: AutomaticTestHelper?, frameworkTestRun: FrameworkTestRunner?) {
        super.onStart(autoTestHelper, frameworkTestRun)
        frameworkTest = frameworkTestRun
        onDescUpdate("$testCaseName Testcase Started")

        verifyTest();

        onFinished();
    }
    /*
      * This method is responsible to verify all feature status and update Testcase result accordingly
      * e.g ->  testA() && testB() && testC && testD...
      *
      * */
    override fun verifyTest(){
        super.verifyTest()

        if( testChannelZap() // testChannelZap() && testChannelZapDetail && ... etc
            && testChannelZapDetail()
        ){
            onDescUpdate("Completed $testCaseName Successfully")
            updateStatus(TestImplStatus.TestCaseFinished())
        }
    }

    private fun testChannelZap(): Boolean{
        featureName = "testChannelZap";
        try {

        }catch (e: Exception){
            onError("$featureName interrupted")
        }
        return getStatus()
    }

    private fun testChannelZapDetail(): Boolean{
        featureName = "testChannelZapDetail";
        try {

        }catch (e: Exception){
            onError("$featureName interrupted")
        }
        return getStatus()
    }

    /*
    * This method is responsible to update status as Error
    * */
    fun onError(errorMsg: String) {
        updateStatus(TestImplStatus.TestCaseInterrupted(errorMsg))
    }

    /*
    * This method is responsible for exit of current Testcase
    * */
    override fun onFinished() {
        super.onFinished()
    }
}