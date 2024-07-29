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

fun Cursor.cu(favorite: String) = getColumnIndex(favorite)

@RunWith(AndroidJUnit4::class)
class FavoriteDataProviderTest {

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        contentResolver = appContext.contentResolver
    }

    @After
    fun tearUp() {
        contentResolver.delete(FAVORITES_URI, null)
    }

    @Test // verify_add_to_favorites
    fun verify_favorite_listIds() {
        val row = contentResolver.insertFavorite(column_list_Ids = "Favorite 1, Favorite 2, Favorite 3, Favorite 4, Favorite 5")
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "verify_favorite_insertion: ${row.toString()}")
        assertThat(row, `is`(notNullValue(Uri::class.java)))
    }

    @Test
    fun verify_get_favorites() {
        contentResolver.insertFavorite()
        val cursor = contentResolver.query(
            FAVORITES_URI,
            null,
            null,
            null,
            null
        )
        cursor?.apply {
            assertThat(count, `is`(getFavoritesCount()))
            moveToNext()
            assertThat(getString(cu(ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN)),
                `is`("8572"))
            assertThat(getString(cu(ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN)),
                `is`("143"))
            assertThat(getString(cu(ReferenceContract.Favorites.SERVICE_ID_COLUMN)),
                `is`("1264"))
            assertThat(getString(cu(ReferenceContract.Favorites.COLUMN_TYPE)),
                `is`("TYPE_DVB_T"))
            assertThat(getString(cu(ReferenceContract.Favorites.COLUMN_LIST_IDS)),
                `is`("Favorite 1, Favorite 2, Favorite 3, Favorite 4, Favorite 5"))
        } ?: assert(false)
    }

    @Test
    fun verify_all_categories_found() {
        val categoriesList = arrayListOf("Favorite 1, Favorite 2, Favorite 3, Favorite 4, Favorite 5")
        categoriesList.forEach {
            contentResolver.insertFavorite(column_list_Ids = it)
        }
        val cursor = contentResolver.query(
            FAVORITES_URI,
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
            categoriesList.forEach{ categoryName ->
                moveToNext()
                assertThat(
                    getString(cu(ReferenceContract.Favorites.COLUMN_LIST_IDS)),
                    `is`(categoryName)
                )
            }
        }
    }

    @Test
    fun verify_search_by_triplet() {
        contentResolver.insertFavorite(original_network_id_column = 10, transport_stream_id_column = 11, service_id_column = 12)
        contentResolver.insertFavorite(original_network_id_column = 10, transport_stream_id_column = 11, service_id_column = 12)
        contentResolver.insertFavorite(original_network_id_column = 0, transport_stream_id_column = 0, service_id_column = 0)

        contentResolver.query(
            FAVORITES_URI,
            arrayOf(BaseColumns._ID),
            "${ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN}=? AND ${ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN}=? AND ${ReferenceContract.Favorites.SERVICE_ID_COLUMN}=?",
            arrayOf("10", "11", "12"),
            null
        )!!.apply {
            assertThat(count, `is`(2))
        }
    }

    @Test
    fun verify_favorites_delete() {
        repeat(3) { contentResolver.insertFavorite() }
        assertThat(getFavoritesCount(), `is`(getFavoritesCount()))
        contentResolver.delete(FAVORITES_URI, null)
        assertThat(getFavoritesCount(), `is`(0))
    }

    @Test
    fun verify_favorite_delete() {
        val row = contentResolver.insertFavorite()
        val deletedRows = contentResolver.delete(row!!, null)
        assertThat(deletedRows, `is`(0))
        assertThat(getFavoritesCount(), `is`(1))
    }

    @Test
    fun verify_favorite_delete_by_category_name() {
        contentResolver.insertFavorite(column_list_Ids = "Favorite 1")
        contentResolver.insertFavorite(column_list_Ids = "Favorite 2")
        contentResolver.insertFavorite(column_list_Ids = "Customized name")
        contentResolver.delete(
            FAVORITES_URI,
            "${ReferenceContract.Favorites.COLUMN_LIST_IDS}=?",
            arrayOf("Favorite 2")
        )
        assertThat(getFavoritesCount(), `is`(2))

        contentResolver.delete(
            FAVORITES_URI,
            "${ReferenceContract.Favorites.COLUMN_LIST_IDS}=?",
            arrayOf("Favorite 6")
        )
        assertThat(getFavoritesCount(), `is`(2))
    }

    @Test
    fun verify_rename_favorite_category() {
        contentResolver.insertFavorite(service_id_column = 1)
        val contentValues = ContentValues()
        contentValues.put(ReferenceContract.Favorites.COLUMN_LIST_IDS, "Customized Name")
        contentResolver.update(
            FAVORITES_URI, contentValues, null, null
        )

        getAllFavorites()?.apply {
            while(moveToNext()) {
                if(getInt(cu(ReferenceContract.Favorites.SERVICE_ID_COLUMN)) == 1) {
                    assertThat(getString(cu(ReferenceContract.Favorites.COLUMN_LIST_IDS)),
                    `is`("Customized Name")
                    )
                } else {
                    assertThat(getInt(cu(ReferenceContract.Favorites.COLUMN_LIST_IDS)),
                        `is`("Favorite 1, Favorite 2, Favorite 3, Favorite 4, Favorite 5"))
                }
            }
        }

    }

    private fun getFavoritesCount() = getAllFavorites()?.count ?: 0

    private fun getAllFavorites(): Cursor? {
        return contentResolver.query(FAVORITES_URI, null, null, null, null)
    }

    companion object {
        private const val databaseFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db"
        private const val databaseJournalFilepath = "/data/data/com.iwedia.cltv.platform.test/databases/reference.db-journal"

        private const val TAG = "FavoriteDataProviderInterfaceTest"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val favorites = 14
            val favoritesId = 15
            ReferenceContentProvider.sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, FAVORITES_TABLE, favorites)
            ReferenceContentProvider.sUriMatcher.addURI(AUTHORITY, "$FAVORITES_TABLE/#", favoritesId)
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