package com.iwedia.cltv.platform.test

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.UtilsInterface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito


@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class UtilsInterfaceInstrumentedTest {
    private val TAG = javaClass.simpleName
    private lateinit var utilsInterface: UtilsInterface

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val factory = ModuleFactory(context)
        withContext(Dispatchers.Main.immediate) {
            utilsInterface = factory.createUtilsModule()
        }
    }

    @Test
    fun testSetPrimaryAudioLanguage() {
        utilsInterface.setPrimaryAudioLanguage("deu")
        val result = utilsInterface.getPrimaryAudioLanguage()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetPrimaryAudioLanguage result is $result")
        assertThat(result, `is`("deu"))
    }

    @Test
    fun testGetPrimaryAudioLanguage() {
        utilsInterface.setPrimaryAudioLanguage("tur")
        val result = utilsInterface.getPrimaryAudioLanguage()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetPrimaryAudioLanguage result is $result")
        assertThat(result, `is`("tur"))
    }

    @Test
    fun testSetPrimarySubtitleLanguage() {
        utilsInterface.setPrimarySubtitleLanguage("fra")
        val result = utilsInterface.getPrimarySubtitleLanguage()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetPrimarySubtitleLanguage result is $result")
        assertThat(result, `is`("fra"))
    }

    @Test
    fun testGetPrimarySubtitleLanguage() {
        utilsInterface.setPrimarySubtitleLanguage("ita")
        val result = utilsInterface.getPrimarySubtitleLanguage()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetPrimarySubtitleLanguage result is $result")
        assertThat(result, `is`("ita"))
    }

    @Test
    fun testSetSecondaryAudioLanguage() {
        utilsInterface.setSecondaryAudioLanguage("fra")
        val result = utilsInterface.getSecondaryAudioLanguage()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetSecondaryAudioLanguage result is $result")
        assertThat(result, `is`("fra"))
    }

    @Test
    fun testGetSecondaryAudioLanguage() {
        utilsInterface.setSecondaryAudioLanguage("ita")
        val result = utilsInterface.getSecondaryAudioLanguage()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetSecondaryAudioLanguage result is $result")
        assertThat(result, `is`("ita"))
    }

    @Test
    fun testSetSecondarySubtitleLanguage() {
        utilsInterface.setSecondarySubtitleLanguage("fra")
        val result = utilsInterface.getSecondarySubtitleLanguage()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetSecondarySubtitleLanguage result is $result")
        assertThat(result, `is`("fra"))
    }

    @Test
    fun testGetSecondarySubtitleLanguage() {
        utilsInterface.setSecondarySubtitleLanguage("ita")
        val result = utilsInterface.getSecondarySubtitleLanguage()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetSecondarySubtitleLanguage result is $result")
        assertThat(result, `is`("ita"))
    }

    @Test
    fun testSetAudioType() {
        utilsInterface.setAudioType(1)
        val result = utilsInterface.getAudioType()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetAudioType result is $result")
        assertThat(result, `is`(1))
    }

    @Test
    fun testGetAudioType() {
        utilsInterface.setAudioType(0)
        val result = utilsInterface.getAudioType()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetAudioType result is $result")
        assertThat(result, `is`(0))
    }

    @Test
    fun testSetSubtitlesType() {
        utilsInterface.setSubtitlesType(1,false)
        val result = utilsInterface.getSubtitlesType()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetSubtitlesType result is $result")
        assertThat(result, `is`(1))
    }

    @Test
    fun testGetSubtitlesType() {
        utilsInterface.setSubtitlesType(0,false)
        val result = utilsInterface.getSubtitlesType()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetSubtitlesType result is $result")
        assertThat(result, `is`(0))
    }

    @Test
    fun testEnableAudioDescription() {
        utilsInterface.enableAudioDescription(true)
        val result = utilsInterface.getAudioDescriptionState()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testEnableAudioDescription result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(true))
        } else {
            assertThat(result, `is`(false))
        }
    }

    @Test
    fun testGetAudioDescriptionState() {
        utilsInterface.enableAudioDescription(false)
        val result = utilsInterface.getAudioDescriptionState()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetAudioDescriptionState result is $result")
        assertThat(result, `is`(false))
    }

    @Test
    fun testEnableHardOfHearing() {
        utilsInterface.enableSubtitles(true)
        utilsInterface.enableHardOfHearing(true)
        val result = utilsInterface.getHardOfHearingState()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testEnableHardOfHearing result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(true))
        } else {
            assertThat(result, `is`(false))
        }
    }

    @Test
    fun testGetHardOfHearingState() {
        utilsInterface.enableHardOfHearing(false)
        val result = utilsInterface.getHardOfHearingState()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetHardOfHearingState result is $result")
        assertThat(result, `is`(false))
    }

    @Test
    fun testEnableSubtitles() {
        utilsInterface.enableSubtitles(true)
        val result = utilsInterface.getSubtitlesState()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testEnableSubtitles result is $result")
        assertThat(result, `is`(true))
    }

    @Test
    fun testGetSubtitlesState() {
        utilsInterface.enableSubtitles(false)
        val result = utilsInterface.getSubtitlesState()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetSubtitlesState result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(false))
        } else {
            assertThat(result, `is`(true))
        }
    }

    @Test
    fun testSetTeletextDigitalLanguage() {
        utilsInterface.setTeletextDigitalLanguage(0)
        val result = utilsInterface.getTeletextDigitalLanguage()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetTeletextDigitalLanguage result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(0))
        } else {
            assertThat(result, `is`(-1))
        }
    }

    @Test
    fun testGetTeletextDigitalLanguage() {
        utilsInterface.setTeletextDigitalLanguage(0)
        val result = utilsInterface.getTeletextDigitalLanguage()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetTeletextDigitalLanguage result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(0))
        } else {
            assertThat(result, `is`(-1))
        }
    }

    @Test
    fun testSetTeletextDecodeLanguage() {
        utilsInterface.setTeletextDecodeLanguage(0)
        val result = utilsInterface.getTeletextDecodeLanguage()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testSetTeletextDecodeLanguage result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(0))
        } else {
            assertThat(result, `is`(-1))
        }
    }

    @Test
    fun testGetTeletextDecodeLanguage() {
        utilsInterface.setTeletextDecodeLanguage(0)
        val result = utilsInterface.getTeletextDecodeLanguage()

        Log.d(Constants.LogTag.CLTV_TAG + TAG,"testGetTeletextDecodeLanguage result is $result")
        if(BuildConfig.FLAVOR.equals("mtk")) {
            assertThat(result, `is`(0))
        } else {
            assertThat(result, `is`(-1))
        }
    }
}
