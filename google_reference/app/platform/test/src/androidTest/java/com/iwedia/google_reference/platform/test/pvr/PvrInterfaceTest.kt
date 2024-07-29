package com.iwedia.cltv.platform.test.pvr

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.permission.PermissionRequester
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


@RunWith(AndroidJUnit4::class)
class PvrInterfaceTest {
    private val TAG = javaClass.simpleName
    private lateinit var epgInterface: EpgInterface
    private lateinit var playerInterface: PlayerInterface
    private lateinit var tvInterface: TvInterface
    private lateinit var utilsInterface: UtilsInterface
    private lateinit var pvr: PvrInterface
    private lateinit var context:Context

    companion object {
        private val permissions = arrayListOf(
            "com.android.providers.tv.permission.WRITE_EPG_DATA",
            "com.android.providers.tv.permission.READ_EPG_DATA",
            "android.permission.READ_TV_LISTINGS",
        )

        val channels = arrayListOf(
            TvChannel(id = 93, index = 0, packageName = "com.mediatek.tvinput", inputId = "com.mediatek.tvinput/.tuner.TunerInputService/HW0", name = "Rai 1", channelId = 93),
            TvChannel(id = 94, index = 1, inputId = "com.google.android.tv.dtvinput/.DtvInputService", name = "Rai 2", channelId = 94),
            TvChannel(id = 95, index = 2, inputId = "com.google.android.tv.dtvinput/.DtvInputService", name = "Rai 3 TGR Veneto", channelId = 95)
        )
    }

    @Before
    fun setup() = runTest {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        context = applicationContext.applicationContext

        withContext(Dispatchers.Main.immediate) {
            val factory = ModuleFactory(applicationContext)
            epgInterface = factory.createEpgModule()
            utilsInterface = factory.createUtilsModule()
            playerInterface = factory.createPlayerModule(utilsInterface, epgInterface)
            tvInterface = factory.createTvModule(playerInterface, factory.createNetworkModule(), factory.createTvInputModule())
            pvr = factory.createPvrModule(epgInterface, playerInterface, tvInterface, utilsInterface)
        }
    }

    @After
    fun tearDown() = runTest{
        pvr.dispose()
        tvInterface.dispose()
    }

    @Test
    fun tifDatabasePermission_success_permissionsShouldBeGranted() = runBlocking {
        permissions.forEach { permission ->
            val permissionRequester = PermissionRequester()
            if(checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
                Log.w(TAG, "$permission is not granted.")
                permissionRequester.addPermissions(permission)
            }
            permissionRequester.requestPermissions()
        }
    }

    private fun checkSelfPermission(context: Context, permission: String): Int {
        return ContextCompat.checkSelfPermission(context, permission)
    }

    @Test
    fun startRecordingByChannel_test() = runBlocking {
        var result = 0
        val tvChannel = channels[0]
        val semaphore = Semaphore(1, 1)

        //startRecordingByChannel_test()
        pvr.startRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) { semaphore.release() }
            override fun onSuccess() {
                result = 1
                semaphore.release()
            }
        }, true)

        semaphore.acquire()
        MatcherAssert.assertThat(result, `is`(1))
        delay(5000)


        //getRecordingInProgressTvChannel_test()
        var channel = pvr.getRecordingInProgressTvChannel()
        MatcherAssert.assertThat(channel, notNullValue())


        //isRecordingInProgress_test()
        var isRecInProgress = pvr.isRecordingInProgress()
        MatcherAssert.assertThat(isRecInProgress, `is`(true))


        //getRecordingInProgress_test()
        var recProgress: RecordingInProgress? = null
        pvr.getRecordingInProgress(object : IAsyncDataCallback<RecordingInProgress> {
            override fun onFailed(error: Error) { semaphore.release() }
            override fun onReceive(data: RecordingInProgress) {
                recProgress = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(recProgress, notNullValue())
        MatcherAssert.assertThat(recProgress?.tvChannel?.channelId, `is`(channels[0].channelId))


        //stopRecordingByChannel_test()
        result = 0
        pvr.stopRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) { semaphore.release() }
            override fun onSuccess() {
                result = 1
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, `is`(1))

        //getRecording_test()
        result = 0
        var recordings: List<Recording>? = null
        pvr.getRecordingList(object : IAsyncDataCallback<List<Recording>> {
            override fun onFailed(error: Error) { semaphore.release() }
            override fun onReceive(data: List<Recording>) {
                recordings = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(recordings, notNullValue())
        Log.d(Constants.LogTag.CLTV_TAG + TAG, recordings.toString())
        MatcherAssert.assertThat(recordings!!.last().tvChannel?.channelId, `is`(channels[0].channelId))

        //renameRecording_test()
        result = 0
        pvr.renameRecording(recordings!![0], "Renamed Rai 1", object: IAsyncCallback {
            override fun onFailed(error: Error) { semaphore.release() }
            override fun onSuccess() {
                result = 1
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(result, `is`(0))

        //removeRecording_test()
        result = 0
        pvr.removeRecording(recordings!![0], object: IAsyncCallback {
            override fun onFailed(error: Error) { semaphore.release() }
            override fun onSuccess() {
                result = 1
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(result, `is`(0))
    }
}
