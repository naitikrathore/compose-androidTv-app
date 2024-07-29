package com.iwedia.cltv.platform.gretzky.provider

import android.content.ContentResolver
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.iwedia.cltv.platform.base.content_provider.ReferenceContentProvider
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.gretzky.util.TAG
import com.iwedia.cltv.platform.model.channel.TunerType
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import java.io.File

internal class ChannelDataProviderTest{
    private lateinit var contentResolver: ContentResolver
    private lateinit var tifDataProvider: TifDataProvider

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        contentResolver = appContext.contentResolver
        tifDataProvider = TifDataProvider(contentResolver);
    }

    @After
    fun tearUp() {
        contentResolver.delete(ReferenceContentProvider.CHANNELS_URI, null)
    }

    @Test
    fun verify_channel_insertion() {
        val tvChannel : TvChannel;
        tvChannel = TvChannel(
            1, 1, "TestChannel", "", "", ArrayList(2), ArrayList( 2),  ArrayList( 2),
            ArrayList( 2), ArrayList( 2), ArrayList( 2), 1, "", "", 1, 1,
            ArrayList(2), false, TunerType.DEFAULT, false, false, 1, 1, 1, 1, 1,
            false, "", "", "", "", "", ArrayList(2));
        var count: Int =contentResolver.query(ReferenceContentProvider.CHANNELS_URI, null, null, null, null)?.count ?: 0;
        tifDataProvider.insertChannel(tvChannel);
        var newCount: Int =contentResolver.query(ReferenceContentProvider.CHANNELS_URI, null, null, null, null)?.count ?: 0;
        assertThat(newCount- count, CoreMatchers.`is`(1))
    }

    companion object {
        private const val databaseFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference_temp.db"
        private const val databaseJournalFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference_temp.db-journal"
        private const val databaseBackupFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference_temp_BACKUP.db"
        private const val databaseJournalBackupFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference_temp_BACKUP.db-journal"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            backupDatabase()
        }

        private fun backupDatabase() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "on backupDatabase")
            val dbFile = File(databaseFilepath)
            if (dbFile.exists())
                dbFile.renameTo(File(databaseBackupFilepath))

            val dbJournalFile = File(databaseJournalFilepath)
            if(dbJournalFile.exists())
                dbJournalFile.renameTo(File(databaseJournalBackupFilepath))
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            val dbFile = File(databaseFilepath)
            val dbJournalFile = File(databaseJournalFilepath)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Test database files are removed: ${dbFile.delete()}, ${dbJournalFile.delete()}")
            restoreDatabase()
        }

        private fun restoreDatabase() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "on restoreDatabase")
            val dbFile = File(databaseBackupFilepath)
            if (dbFile.exists())
                dbFile.renameTo(File(databaseFilepath))

            val dbJournalFile = File(databaseJournalBackupFilepath)
            if(dbJournalFile.exists())
                dbJournalFile.renameTo(File(databaseJournalFilepath))
        }
    }
}