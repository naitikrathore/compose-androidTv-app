package com.iwedia.cltv.platform.base.provider

import android.content.ContentResolver
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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScheduledRecordingsTest {

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        contentResolver = appContext.contentResolver
    }

    @After
    fun tearUp() {
        contentResolver.delete(SCHEDULED_RECORDINGS_URI, null)
    }

    @Test
    fun verify_insert_schRecording() {
        val row = contentResolver.insertRecordings(name_column = "scheduled Reminder 1")
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "verify_recording_insertion: ${row.toString()}")
        assertThat(row, `is`(notNullValue(Uri::class.java)))
    }

    @Test
    fun verify_get_schReminder() {
        contentResolver.insertRecordings()
        val cursor = contentResolver.query(
            SCHEDULED_RECORDINGS_URI,
            null,
            null,
            null,
            null
        )
        cursor?.apply {
            assertThat(count, `is`(getSchRecordingsCount()))
            moveToNext()
            assertThat(getString(ci(ReferenceContract.ScheduledRecordings.NAME_COLUMN)),
                `is`("scheduledReminder"))
            assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN)),
                `is`(1))
            assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN)),
                `is`(2))
            /*assertThat(getLong(ci(ReferenceContract.ScheduledRecordings.START_TIME_COLUMN)),
                `is`(System.currentTimeMillis()))
            assertThat(getLong(ci(ReferenceContract.ScheduledRecordings.END_TIME_COLUMN)),
                `is`(System.currentTimeMillis() + 1000000))*/
            assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.DATA_COLUMN)),
                `is`(0))
        } ?: assert(false)
    }

    @Test
    fun verify_all_recordings_found() {
        val eventIds = arrayListOf(1, 2, 3)
        eventIds.forEach{ event ->
            contentResolver.insertRecordings(event_id_column = event)
        }

        val cursor = contentResolver.query(
            SCHEDULED_RECORDINGS_URI,
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
            eventIds.forEach{ event ->
                moveToNext()
                assertThat(
                    getInt(ci(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN)),
                    `is`(event)
                )
            }
        }
    }

    @Test
    fun verify_search_by_triplet() {
        contentResolver.insertRecordings(name_column = "Scheduled Reminder 1", channel_id_column = 11, event_id_column = 12)
        contentResolver.insertRecordings(name_column = "Scheduled Reminder 1", channel_id_column = 11, event_id_column = 12)
        contentResolver.insertRecordings(name_column = "", channel_id_column = 0, event_id_column = 0)

        contentResolver.query(
            SCHEDULED_RECORDINGS_URI,
            arrayOf(BaseColumns._ID),
            "${ReferenceContract.ScheduledRecordings.NAME_COLUMN}=? AND " +
                    "${ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN}=? AND " +
                    "${ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN}=?",
            arrayOf("Scheduled Reminder 1", "11", "12"),
            null
        )!!.apply {
            assertThat(count, `is`(2))
        }
    }

    @Test
    fun verify_schRecording_delete() {
        repeat(3) { contentResolver.insertRecordings() }
        assertThat(getSchRecordingsCount(), `is`(3))
        contentResolver.delete(SCHEDULED_RECORDINGS_URI, null)
        assertThat(getSchRecordingsCount(), `is`(0))
    }

    @Test
    fun verify_recording_delete() {
        val row = contentResolver.insertRecordings()
        val deletedRows = contentResolver.delete(row!!, null)
        assertThat(deletedRows, `is`(0))
        assertThat(getSchRecordingsCount(), `is`(1))
    }

    @Test
    fun verify_recordings_delete_by_some_param() {
        contentResolver.insertRecordings(name_column = "com.google.android.tv.dtvinput/.DtvInputService")
        contentResolver.insertRecordings(name_column = "com.haystack.android/.tv.livechannel.LiveChannelInputService")
        contentResolver.insertRecordings(name_column = "com.haystack.android/.tv.ott.LiveChannelInputService")
        contentResolver.delete(
            SCHEDULED_RECORDINGS_URI,
            "${ReferenceContract.ScheduledRecordings.NAME_COLUMN}=?",
            arrayOf("com.google.android.tv.dtvinput/.DtvInputService")
        )
        assertThat(getSchRecordingsCount(), `is`(2))

        contentResolver.delete(
            SCHEDULED_RECORDINGS_URI,
            "${ReferenceContract.ScheduledReminders.NAME_COLUMN}=?",
            arrayOf("Rai 3 TGR Lazio")
        )
        assertThat(getSchRecordingsCount(), `is`(2))
    }

    @Test
    fun upgrade_recordings_columns() {
        contentResolver.insertRecordings(name_column = "ScheduledReminder")
        val contentValues = ContentValues()
        contentValues.put(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN, 3)
        contentValues.put(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN, 4)
        contentResolver.update(
            SCHEDULED_RECORDINGS_URI, contentValues, null, null
        )

        getAllSchRecordings()?.apply {
            while (moveToNext()) {
                if (getString(ci(ReferenceContract.ScheduledRecordings.NAME_COLUMN)).equals("ScheduledReminder")) {
                    assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN)), `is`(3))
                    assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN)), `is`(4))
                } else {
                    assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN)), `is`(2))
                    assertThat(getInt(ci(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN)), `is`(1))
                }
            }
        }
    }

    private fun getSchRecordingsCount() = getAllSchRecordings()?.count ?: 0

    private fun getAllSchRecordings(): Cursor? {
        return contentResolver.query(SCHEDULED_RECORDINGS_URI, null, null, null, null)
    }

    companion object {
        private const val databaseFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db"
        private const val databaseJournalFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db-journal"

        private const val TAG = "ScheduleRecordingsInterfaceTest"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val scheduledRecordings = 11
            val scheduledRecordingsId = 12
            ReferenceContentProvider.sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, SCHEDULED_RECORDINGS_TABLE, scheduledRecordings)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, "$SCHEDULED_RECORDINGS_TABLE/#", scheduledRecordingsId)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            val dbFile = File(databaseFilepath)
            val dbJournalFile = File(databaseJournalFilepath)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Test database files are removed: ${dbFile.delete()} ${dbJournalFile.delete()}")
        }

    }
}