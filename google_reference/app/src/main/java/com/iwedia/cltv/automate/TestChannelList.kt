package com.iwedia.cltv.automate

import android.view.KeyEvent

class TestChannelList: Testcase() {

    var frameworkTest: FrameworkTestRunner? = null

  /*
   * Update current Testcase,
   * and initialize testCaseName
   * */
    init {
        testCase = this;
        testCaseName = "TestChannelList"
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

        if( testChannelNavigation() // testChannelNavigation() && testChannelDetail && ... etc
            && testChannelDetail()
            ){
            onDescUpdate("Completed $testCaseName Successfully")
            updateStatus(TestImplStatus.TestCaseFinished())
        }
    }

    /*
    * This method test a specific feature i.e navigation of channelList
    * featureName : String, is the name of the method
    * @Rule feature specific test must be in try..catch block to update its execution process
    * @Return must return getStatus()
    * */
    private fun testChannelNavigation() : Boolean{
         featureName = "testChannelNavigation";
        try {
            frameworkTest?.onKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER)
            frameworkTest?.onKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN)

            onDescUpdate("$featureName complete")
        } catch (e: Exception) {
            onError("$featureName interrupted")
        }

        return getStatus();
    }

    /*
    * This method test a specific feature i.e detail feature of channelList
    * featureName : String, is the name of the method
    * @Rule feature specific test must be in try..catch block to update its execution process
    * @Return must return getStatus()
    * */
    private fun testChannelDetail(): Boolean{
        featureName = "testChannelDetail";
        try {
            //frameworkTest?.onRepeatKeyEvent(2,KeyEvent.KEYCODE_DPAD_RIGHT)

            onDescUpdate("$featureName complete")
        } catch (e: Exception) {
            onError("$featureName interrupted")
        }
        return getStatus();
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
