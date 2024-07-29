package com.iwedia.cltv.platform.base.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iwedia.cltv.platform.base.content_provider.ReferenceContentProvider
import com.iwedia.cltv.platform.base.content_provider.ReferenceContract
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers.greaterThan
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RefChannelsProviderTest {
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        contentResolver = appContext.contentResolver
    }

    @After
    fun tearUp() {
        contentResolver.delete(CHANNELS_URI, null)
    }

    @Test
    fun verify_channel_insertion() {
        val row = contentResolver.insertChannel(display_name = "Rai 1")
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "verify_channel_insertion: ${row.toString()}")
        assertThat(row, `is`(notNullValue(Uri::class.java)))
    }

    @Test
    fun verify_specific_channel_query() {
        val row = contentResolver.insertChannel()
        val cursor = contentResolver.query(
            row!!,
            null,
            null,
            null,
            null
        )
        cursor?.apply {
            assertThat(count, `is`(1))

            moveToNext()
            assertThat(getString(ci(ReferenceContract.Channels.PACKAGE_NAME_COLUMN)),
                            `is`("com.google.android.tv.dtvinput"))
            assertThat(getString(ci(ReferenceContract.Channels.INPUT_ID_COLUMN)),
                            `is`("com.google.android.tv.dtvinput/.DtvInputService"))
            assertThat(getString(ci(ReferenceContract.Channels.TYPE_COLUMN)), `is`("TYPE_DVB_T"))
            assertThat(getString(ci(ReferenceContract.Channels.SERVICE_TYPE_COLUMN)), `is`("SERVICE_TYPE_AUDIO_VIDEO"))
            assertThat(getInt(ci(ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN)), `is`(8572))
            assertThat(getInt(ci(ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN)), `is`(143))
            assertThat(getInt(ci(ReferenceContract.Channels.SERVICE_ID_COLUMN)), `is`(1264))
            assertThat(getString(ci(ReferenceContract.Channels.DISPLAY_NUMBER_COLUMN)),`is`("8"))
            assertThat(getString(ci(ReferenceContract.Channels.NAME_COLUMN)), `is`("Rai 3 TGR Lazio"))
            assertThat(isNull(ci(ReferenceContract.Channels.NETWORK_AFFILIATION_COLUMN)),`is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.DESCRIPTION_COLUMN)), `is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.VIDEO_FORMAT_COLUMN)), `is`(true))
            assertThat(getInt(ci(ReferenceContract.Channels.BROWSABLE_COLUMN)), `is`(1))
            assertThat(getInt(ci(ReferenceContract.Channels.SEARCHABLE_COLUMN)), `is`(1))
            assertThat(getInt(ci(ReferenceContract.Channels.LOCKED_COLUMN)), `is`(0))
            assertThat(isNull(ci(ReferenceContract.Channels.APP_LINK_POSTER_ART_URI_COLUMN)), `is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.APP_LINK_TEXT_COLUMN)), `is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.APP_LINK_COLOR_COLUMN)), `is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.APP_LINK_INTENT_URI_COLUMN)),`is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.INTERNAL_PROVIDER_DATA_COLUMN)),`is`(true))
            assertThat(getInt(ci(ReferenceContract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN)),`is`(3))
            //assertThat(isNull(ci(Channels.COLUMN_LOGO)), `is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.VERSION_NUMBER_COLUMN)), `is`(true))
            assertThat(getInt(ci(ReferenceContract.Channels.TRANSIENT_COLUMN)), `is`(0))
            assertThat(getInt(ci(ReferenceContract.Channels.SKIP_COLUMN)), `is`(0))
            assertThat(isNull(ci(ReferenceContract.Channels.INTERNAL_PROVIDER_ID_COLUMN)), `is`(true))
            assertThat(isNull(ci(ReferenceContract.Channels.REFERENCE_NAME_COLUMN)), `is`(true))
            assertThat(getInt(ci(ReferenceContract.Channels.DISPLAY_NUMBER_CHANGED_COLUMN)), `is`(0))
            assertThat(getInt(ci(ReferenceContract.Channels.ORDINAL_NUMBER_COLUMN)), `is`(0))
            assertThat(getInt(ci(ReferenceContract.Channels.DELETED_COLUMN)), `is`(0))
            //assertThat(getString(ci(Channels.COLUMN_CANONICAL_GENRE)), `is`(""))
        } ?: assert(false)
    }

    @Test
    fun verify_all_channels_found() {
        val channelNames = arrayListOf("Rai 1", "Rai 2", "Rai 3")
        channelNames.forEach{ channelName ->
            contentResolver.insertChannel(display_name = channelName)
        }

        val cursor = contentResolver.query(
            CHANNELS_URI,
            null,
            null,
            null,
            BaseColumns._ID
        )

        assertThat(
            cursor,
            `is`(notNullValue(Cursor::class.java))
        )
        cursor?.apply {
            channelNames.forEach{ channelName ->
                moveToNext()
                assertThat(
                    getString(ci(ReferenceContract.Channels.NAME_COLUMN)),
                    `is`(channelName)
                )
            }
        }
    }

    @Test
    fun verify_channels_are_sorted_by_display_number() {
        val channelNumbers = arrayListOf("1", "5", "2")
        channelNumbers.forEach{ channelNumber ->
            contentResolver.insertChannel(display_number = channelNumber)
        }
        channelNumbers.sort()

        val cursor = contentResolver.query(
            CHANNELS_URI,
            null,
            null,
            null,
            ReferenceContract.Channels.DISPLAY_NUMBER_COLUMN
        )

        cursor!!.apply {
            channelNumbers.forEach{ channelNumber ->
                moveToNext()
                assertThat(
                    getString(ci(ReferenceContract.Channels.DISPLAY_NUMBER_COLUMN)),
                    `is`(channelNumber)
                )
            }
        }
    }

    @Test
    fun verify_search_by_triplet() {
        contentResolver.insertChannel(original_network_id = 10, transport_stream_id = 11, service_id = 12)
        contentResolver.insertChannel(original_network_id = 10, transport_stream_id = 11, service_id = 12)
        contentResolver.insertChannel(original_network_id = 0, transport_stream_id = 0, service_id = 0)

        contentResolver.query(
            CHANNELS_URI,
            arrayOf(BaseColumns._ID),
            "${ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN}=? AND ${ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN}=? AND ${ReferenceContract.Channels.SERVICE_ID_COLUMN}=?",
            arrayOf("10", "11", "12"),
            null
        )!!.apply {
            assertThat(count, `is`(2))
        }
    }

    @Test
    fun verify_channels_delete() {
        repeat(3) { contentResolver.insertChannel() }
        // Get inserted channels
        assertThat(getChannelCount(), `is`(3))
        // Delete all channels
        contentResolver.delete(CHANNELS_URI, null)
        assertThat(getChannelCount(), `is`(0))
    }

    @Test
    fun verify_channel_delete() {
        val row = contentResolver.insertChannel()
        val deletedRows = contentResolver.delete(row!!, null)
        assertThat(deletedRows, greaterThan(0))
        assertThat(getChannelCount(), `is`(0))
    }

    @Test
    fun verify_channel_delete_by_some_param() {
        contentResolver.insertChannel(input_id = "com.google.android.tv.dtvinput/.DtvInputService")
        contentResolver.insertChannel(input_id = "com.haystack.android/.tv.livechannel.LiveChannelInputService")
        contentResolver.insertChannel(input_id = "com.haystack.android/.tv.ott.LiveChannelInputService")
        contentResolver.delete(
            CHANNELS_URI,
            "${ReferenceContract.Channels.INPUT_ID_COLUMN}=?",
            arrayOf("com.google.android.tv.dtvinput/.DtvInputService")
        )
        assertThat(getChannelCount(), `is`(2))

        contentResolver.delete(
            CHANNELS_URI,
            "${ReferenceContract.Channels.NAME_COLUMN}=?",
            arrayOf("Rai 3 TGR Lazio")
        )
        assertThat(getChannelCount(), `is`(0))
    }

    @Test
    fun upgrade_channel_columns() {
        repeat(2) { contentResolver.insertChannel() }
        val uri = contentResolver.insertChannel(service_id = 1)

        // Update by id
        var contentValues = ContentValues()
        contentValues.put(ReferenceContract.Channels.BROWSABLE_COLUMN, 0)
        contentValues.put(ReferenceContract.Channels.DELETED_COLUMN, 1)
        contentResolver.update(
            uri!!, contentValues, null, null
        )

        getAllChannels()?.apply {
            while(moveToNext()) {
                if(getInt(ci(ReferenceContract.Channels.SERVICE_ID_COLUMN)) == 1) {  // Updated channel
                    assertThat(getInt(ci(ReferenceContract.Channels.BROWSABLE_COLUMN)), `is`(0))
                    assertThat(getInt(ci(ReferenceContract.Channels.DELETED_COLUMN)), `is`(1))
                } else {
                    assertThat(getInt(ci(ReferenceContract.Channels.BROWSABLE_COLUMN)), `is`(1))
                    assertThat(getInt(ci(ReferenceContract.Channels.DELETED_COLUMN)), `is`(0))
                }
            }
        }

        // Updated by condition
        contentValues = ContentValues()
        contentValues.put(ReferenceContract.Channels.PACKAGE_NAME_COLUMN, "CHANGED.PACKAGE")
        contentResolver.update(
            CHANNELS_URI,
            contentValues,
            "package_name=?",
            arrayOf("com.google.android.tv.dtvinput")
        )
        getAllChannels()?.apply {
            while (moveToNext()) {
                assertThat(getString(ci(ReferenceContract.Channels.PACKAGE_NAME_COLUMN)), `is`("CHANGED.PACKAGE"))
            }
        }
    }

    @Test
    fun verify_get_type_method() {
        assertThat(
            contentResolver.getType(ContentUris.withAppendedId(CHANNELS_URI, 1)),
            `is`(ReferenceContract.Channels.CONTENT_ITEM_TYPE)
        )
        assertThat(
            contentResolver.getType(CHANNELS_URI),
            `is`(ReferenceContract.Channels.CONTENT_TYPE)
        )
    }

    private fun getChannelCount() = getAllChannels()?.count ?: 0

    private fun getAllChannels(): Cursor? {
        return contentResolver.query(CHANNELS_URI, null, null, null, null)
    }

    companion object {
        private const val databaseFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db"
        private const val databaseJournalFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db-journal"

        private const val TAG = "ChannelProviderTests"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val channels = 1
            val channelsId = 2
            ReferenceContentProvider.sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, CHANNELS_TABLE, channels)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, "$CHANNELS_TABLE/#", channelsId)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            val dbFile = File(databaseFilepath)
            val dbJournalFile = File(databaseJournalFilepath)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Test database files are removed: ${dbFile.delete()}, ${dbJournalFile.delete()}")
        }

    }
}