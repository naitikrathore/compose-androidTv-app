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
class ScheduleReminderTest {

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        contentResolver = appContext.contentResolver
    }

    @After
    fun tearUp() {
        contentResolver.delete(SCHEDULED_REMINDERS_URI, null)
    }

    @Test
    fun verify_insert_schReminder() {
        val row = contentResolver.insertReminder(name_column = "scheduled Reminder 1")
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "verify_channel_insertion: ${row.toString()}")
        assertThat(row, `is`(notNullValue(Uri::class.java)))
    }

    @Test
    fun verify_get_schReminder() {
        contentResolver.insertReminder()
        val cursor = contentResolver.query(
            SCHEDULED_REMINDERS_URI,
            null,
            null,
            null,
            null
        )
        cursor?.apply {
            assertThat(count, `is`(getSchRemindersCount()))
            moveToNext()
            assertThat(getString(ci(ReferenceContract.ScheduledReminders.NAME_COLUMN)),
                `is`("scheduledReminder"))
            assertThat(getInt(ci(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN)),
                `is`(1))
            assertThat(getInt(ci(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN)),
                `is`(2))
            /*assertThat(getString(ci(ReferenceContract.ScheduledReminders.START_TIME_COLUMN)),
                `is`(System.currentTimeMillis()))*/
        } ?: assert(false)
    }

    @Test
    fun verify_all_reminder_found() {
        val eventIds = arrayListOf(1, 2, 3)
        eventIds.forEach{ event ->
            contentResolver.insertReminder(event_id_column = event)
        }

        val cursor = contentResolver.query(
            SCHEDULED_REMINDERS_URI,
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
                    getInt(ci(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN)),
                    `is`(event)
                )
            }
        }
    }

    @Test
    fun verify_search_by_triplet() {
        contentResolver.insertReminder(name_column = "Scheduled Reminder 1", channel_id_column = 11, event_id_column = 12)
        contentResolver.insertReminder(name_column = "Scheduled Reminder 1", channel_id_column = 11, event_id_column = 12)
        contentResolver.insertReminder(name_column = "", channel_id_column = 0, event_id_column = 0)

        contentResolver.query(
            SCHEDULED_REMINDERS_URI,
            arrayOf(BaseColumns._ID),
            "${ReferenceContract.ScheduledReminders.NAME_COLUMN}=? AND " +
                    "${ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN}=? AND " +
                    "${ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN}=?",
            arrayOf("Scheduled Reminder 1", "11", "12"),
            null
        )!!.apply {
            assertThat(count, `is`(2))
        }
    }

    @Test
    fun verify_schReminder_delete() {
        repeat(3) { contentResolver.insertReminder() }
        assertThat(getSchRemindersCount(), `is`(3))
        contentResolver.delete(SCHEDULED_REMINDERS_URI, null)
        assertThat(getSchRemindersCount(), `is`(0))
    }

    @Test
    fun verify_Reminder_delete() {
        val row = contentResolver.insertReminder()
        val deletedRows = contentResolver.delete(row!!, null)
        assertThat(deletedRows, `is`(0))
        assertThat(getSchRemindersCount(), `is`(1))
    }

    @Test
    fun verify_channel_delete_by_some_param() {
        contentResolver.insertReminder(name_column = "com.google.android.tv.dtvinput/.DtvInputService")
        contentResolver.insertReminder(name_column = "com.haystack.android/.tv.livechannel.LiveChannelInputService")
        contentResolver.insertReminder(name_column = "com.haystack.android/.tv.ott.LiveChannelInputService")
        contentResolver.delete(
            SCHEDULED_REMINDERS_URI,
            "${ReferenceContract.ScheduledReminders.NAME_COLUMN}=?",
            arrayOf("com.google.android.tv.dtvinput/.DtvInputService")
        )
        assertThat(getSchRemindersCount(), `is`(2))

        contentResolver.delete(
            SCHEDULED_REMINDERS_URI,
            "${ReferenceContract.ScheduledReminders.NAME_COLUMN}=?",
            arrayOf("Rai 3 TGR Lazio")
        )
        assertThat(getSchRemindersCount(), `is`(2))
    }

    @Test
    fun upgrade_channel_columns() {
        contentResolver.insertReminder(name_column = "ScheduledReminder")

        val contentValues = ContentValues()
        contentValues.put(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN, 3)
        contentValues.put(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN, 4)
        contentResolver.update(
            SCHEDULED_REMINDERS_URI, contentValues, null, null
        )

        getAllSchReminders()?.apply {
            while (moveToNext()) {
                if (getString(ci(ReferenceContract.ScheduledReminders.NAME_COLUMN)).equals("ScheduledReminder")) {  // Updated channel
                    assertThat(getInt(ci(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN)), `is`(3))
                    assertThat(getInt(ci(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN)), `is`(4))
                } else {
                    assertThat(getInt(ci(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN)), `is`(2))
                    assertThat(getInt(ci(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN)), `is`(1))
                }
            }
        }
    }

    private fun getSchRemindersCount() = getAllSchReminders()?.count ?: 0

    private fun getAllSchReminders(): Cursor? {
        return contentResolver.query(SCHEDULED_REMINDERS_URI, null, null, null, null)
    }

    companion object {
        private const val databaseFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db"
        private const val databaseJournalFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db-journal"

        private const val TAG = "ScheduleReminderInterfaceTest"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val scheduledReminders = 9
            val scheduledRemindersId = 10
            ReferenceContentProvider.sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, SCHEDULED_REMINDERS_TABLE, scheduledReminders)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, "$SCHEDULED_REMINDERS_TABLE/#", scheduledRemindersId)
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