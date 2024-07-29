package com.iwedia.cltv.platform.test

import org.junit.Assert.*
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.model.network.NetworkData
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NetworkHandlerTest {

    val TAG = javaClass.simpleName
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Volatile
    private lateinit var tests: MutableList<TestCase>

    private lateinit var networkHandler: NetworkInterface
    private var cm: ConnectivityManager
    private var shadowCM: ShadowConnectivityManager

    private val ethernet: Network
    private val ethernetNoInternet: Network
    private val wifi: Network
    private val cellular: Network

    init {
        val context = ApplicationProvider.getApplicationContext<Application>()
        cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowCM = shadowOf(cm)

        ethernet = ShadowNetwork.newInstance(10)
        var networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            it.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        shadowCM.setNetworkCapabilities(ethernet, networkCapabilities)

        ethernetNoInternet = ShadowNetwork.newInstance(11)
        networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        shadowCM.setNetworkCapabilities(ethernetNoInternet, networkCapabilities)

        wifi = ShadowNetwork.newInstance(12)
        networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            it.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        shadowCM.setNetworkCapabilities(wifi, networkCapabilities)

        cellular = ShadowNetwork.newInstance(13)
        networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            it.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        shadowCM.setNetworkCapabilities(cellular, networkCapabilities)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowCM = shadowOf(cm)
        shadowCM.clearAllNetworks()

        val factory = ModuleFactory(context)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setUp: ModuleFactory $factory")
        networkHandler = factory.createNetworkModule()

        tests = mutableListOf()
        networkHandler.networkStatus.observeForever(networkStateObserver)
    }

    @After
    fun tearDown() {
        networkHandler.networkStatus.removeObserver(networkStateObserver)
    }

    private val networkStateObserver = object: Observer<NetworkData> {
        var index = 0
        override fun onChanged(network: NetworkData?) {
            if(this@NetworkHandlerTest::tests.isInitialized) {
                if(tests[index].updatingNetwork == ethernetNoInternet) index++
                if(index < tests.size) {
                    try {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChanged: CHECK test case [$index] >>>> Expects: $network >>> Updating: ${tests[index].expectedNetworkState}")
                        assertThat(network, `is`(tests[index].expectedNetworkState))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChanged: Test case >> PASSED")
                    } catch (e: AssertionError) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChanged: Failed test case $index")
                        tests[index].testException = e
                    }
                    index++
                }
            }
        }
    }

    @Test
    fun `Set network without net capability should return no internet`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(this.testScheduler))
        // Adding test cases for this test
        tests.addAll(
            listOf(
                createNewTestCase(null, ethernetNoInternet, true)
            )
        )

        performNetworkStateSwitchesAsync().await()

        tests.forEach{
            if(it.testException != null) throw it.testException!!
        }
    }

    @Test
    fun `Verify network is available and than lost`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(this.testScheduler))
        tests.addAll(
            listOf(
                createNewTestCase(ethernet, ethernet, true),
                createNewTestCase(null, ethernet, false)
            )
        )

        performNetworkStateSwitchesAsync().await()

        tests.forEach{
            if(it.testException != null) throw it.testException!!
        }
    }

    @Test
    fun `Verify added two networks and lost one of networks`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(this.testScheduler))

        tests.addAll(
            listOf(
                createNewTestCase(cellular, cellular, true),
//                createNewTestCase(wifi, wifi, true),
                createNewTestCase(cellular, ethernetNoInternet, true),
//                createNewTestCase(cellular, wifi, false),
                createNewTestCase(null, cellular, false)
            )
        )

        performNetworkStateSwitchesAsync().await()
        tests.forEach{
            if(it.testException != null) throw it.testException!!
        }
    }

    private fun performNetworkStateSwitchesAsync() = CoroutineScope(Dispatchers.IO).async {
        for(i in 0 until tests.size) {
            delay(500)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "performNetworkStateSwitchesAsync: Execute $i. test case")
            if(tests[i].isNetworkAvailable) {
                shadowCM.networkCallbacks.forEach{
                    it.onAvailable(tests[i].updatingNetwork!!)
                }
            }
            else {
                shadowCM.networkCallbacks.forEach{
                    it.onLost(tests[i].updatingNetwork!!)
                }
            }
            delay(100)
        }
    }

    private fun createNewTestCase(
        expectedNetwork: Network?, updatingNetwork: Network?, isNetworkAvailable: Boolean
    ): TestCase {
        if(expectedNetwork == null) {
            return TestCase(NetworkData.NoConnection, updatingNetwork, isNetworkAvailable)
        }
        val networkCapabilities = cm.getNetworkCapabilities(expectedNetwork)
        val linkProperties = cm.getLinkProperties(expectedNetwork)
        return TestCase(NetworkData.Network(networkCapabilities, linkProperties), updatingNetwork, isNetworkAvailable)
    }

    data class TestCase(
        val expectedNetworkState: NetworkData,
        val updatingNetwork: Network?,
        val isNetworkAvailable: Boolean,
        var testException: AssertionError? = null
    )
}