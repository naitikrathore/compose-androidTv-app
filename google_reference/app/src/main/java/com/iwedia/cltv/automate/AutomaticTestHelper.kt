package com.iwedia.cltv.automate

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AutomaticTestHelper(frameworkTestRunner: FrameworkTestRunner) {
    var fileTestCases = mutableListOf<Testcase>()
    var frameworkTest: FrameworkTestRunner? = null
    var testResultMap = HashMap<Testcase, Boolean>()

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    init {
        frameworkTest = frameworkTestRunner
    }

    private fun unregisterTestCase(tc: Testcase) {
        fileTestCases.remove(tc);
    }

    private fun registerTestCase(tc: Testcase) {
        if(!fileTestCases.contains(tc))
            fileTestCases.add(tc);
    }

    fun start() {
        for (i in 0 until fileTestCases.size) {
            val triggeredTestcase: Testcase = fileTestCases[i]
            testResultMap.put(triggeredTestcase, false)
            lock.withLock {
                triggeredTestcase.onStart(this, frameworkTest)
                condition.await() //wait()
            }
        }
    }

    /*fun cancel() {
        checkAndCancelLock();
        fileTestCases.clear()
    }*/

    fun getTestCasesCount() {
        fileTestCases.size
    }

    fun onAddTestCase(testClass: Testcase) {
        registerTestCase(testClass)
    }

    fun onUpdateTestCase(testcase: Testcase, updatedStatus: Boolean) {
        var testcaseUpdateCount: Int = 0

        if(!testResultMap.isEmpty() && testResultMap.containsKey(testcase)) {

            lock.withLock {   //notify()
                testResultMap.put(testcase, updatedStatus)
                testcaseUpdateCount++;
                condition.signal()
            }
        }

        if(testcaseUpdateCount == fileTestCases.size){
            onTestSuiteComplete()
        }
    }

    fun onTestSuiteComplete(){
       checkAndCancelLock()
       frameworkTest?.let { it.generateReport() }
    }

    private fun checkAndCancelLock() {
        if(lock.isLocked){
            lock.lockInterruptibly()
        }
    }


}
