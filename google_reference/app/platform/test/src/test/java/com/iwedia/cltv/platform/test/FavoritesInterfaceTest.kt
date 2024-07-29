package com.iwedia.cltv.platform.test

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Semaphore
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FavoritesInterfaceTest {

    val TAG = javaClass.simpleName
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var favInterface: FavoritesInterface
    private lateinit var mockChannelDataProviderInterface: ChannelDataProviderInterface

    private lateinit var favoritesOriginal: Any
    private lateinit var sharedPreferences: SharedPreferences
    private val categoriesList = ArrayList<String>()
    private lateinit var tvChannel: TvChannel
    private lateinit var tvChannelList: ArrayList<TvChannel>
    private lateinit var favoriteItem: FavoriteItem

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        mockChannelDataProviderInterface = Mockito.mock(ChannelDataProviderInterface::class.java)


        sharedPreferences =
            context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favData1 = "Favorite"
        val FAVORITE_1 = "$favData1 1"
        val FAVORITE_2 = "$favData1 2"
        val FAVORITE_3 = "$favData1 3"
        val FAVORITE_4 = "$favData1 4"
        val FAVORITE_5 = "$favData1 5"

        var favorites = sharedPreferences.getString("favorites", "")!!
        favoritesOriginal = "$FAVORITE_1 ,$FAVORITE_2,$FAVORITE_3,$FAVORITE_4,$FAVORITE_5"
        if (favorites == "") {
            favorites = "$FAVORITE_1,$FAVORITE_2,$FAVORITE_3,$FAVORITE_4,$FAVORITE_5"
            sharedPreferences.edit().putString("favorites", favorites).apply()
        }
        val temp = favorites.split(",")
        temp.forEach { item ->
            if (item != " " && item != "")
                categoriesList.add(item)
        }

        tvChannelList = arrayListOf(
            TvChannel(name = "Rai4", lcn = 41, favListIds = categoriesList),
            TvChannel(name = "Rai2", lcn = 4, favListIds = categoriesList),
            TvChannel(name = "Rai3", displayNumber = "5", favListIds = categoriesList),
            TvChannel(name = "Rai5", displayNumber = "4", favListIds = categoriesList)
        )
        tvChannel = TvChannel(id = 1, name = "Rai1", lcn = 52/*, favListIds = categoriesList*/)

        favoriteItem = FavoriteItem(
            tvChannel.id,
            FavoriteItemType.TV_CHANNEL,
            categoriesList,
            tvChannel,
            categoriesList
        )

        val className = "com.iwedia.cltv.platform.base.FavoritesInterfaceBaseImpl" // the name of the class you want to create
        val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
        val runtimeClass = clazz.constructors.find { it.parameters.size == 2 }

        MatcherAssert.assertThat(runtimeClass, CoreMatchers.notNullValue())
        val instance = runtimeClass!!.call(context, mockChannelDataProviderInterface)
        MatcherAssert.assertThat(mockChannelDataProviderInterface, CoreMatchers.notNullValue())
        favInterface = instance as FavoritesInterface
    }

    @Test
    fun geFavoriteCategoriesTest() {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val cat = favInterface.geFavoriteCategories()

        MatcherAssert.assertThat(cat, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(cat.size, CoreMatchers.`is`(5))
    }

    @Test
    fun getChannelListTest() {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val channelList = favInterface.getChannelList()

        MatcherAssert.assertThat(channelList, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(channelList.size, CoreMatchers.`is`(4))
        MatcherAssert.assertThat(channelList.get(0).name, CoreMatchers.`is`("Rai4"))
    }


    @Test
    fun updateFavItemTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null

        favInterface.updateFavoriteItem(favoriteItem, object: IAsyncCallback {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
            }

            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: $categoriesList")
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
            }

        })
        semaphore.acquire()
    }

    @Test
    fun isInFavTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null
        var isInFav = false

        favInterface.isInFavorites(favoriteItem, object: IAsyncDataCallback<Boolean> {
            override fun onFailed(error: Error) {
                errorMessage = error
                isInFav = false
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(isInFav, CoreMatchers.`is`(false))
            }

            override fun onReceive(data: Boolean) {
                isInFav = true
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(isInFav, CoreMatchers.`is`(true))
            }


        })
        semaphore.acquire()
    }

    @Test
    fun getFavItemsTest() = runTest {
        /*Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null

        favInterface.getFavoriteItems(object: IAsyncDataCallback<List<FavoriteItem>> {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
            }

            override fun onReceive(data: List<FavoriteItem>) {
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
            }


        })
        semaphore.acquire()*/
    }

    @Test
    fun getFavoriteListByType() = runTest {
        /*Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null

        favInterface.getFavoriteListByType(1, object: IAsyncDataCallback<List<FavoriteItem>> {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
            }

            override fun onReceive(data: List<FavoriteItem>) {
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
            }


        })
        semaphore.acquire()*/
    }

    @Test
    fun addFavCategoryTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null
        var favorites: String
        var favCatList: ArrayList<String>? = ArrayList()

        favInterface.addFavoriteCategory("Favorite 6", object: IAsyncCallback {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(0))
            }

            override fun onSuccess() {
                favorites = sharedPreferences.getString("favorites", "")!! //"Favorite 1, Favorite 2"
                val temp = favorites.split(",")
                temp.forEach { item ->
                    if (item != " " && item != "")
                        favCatList?.add(item)
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: ${favCatList?.size}")
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(6))
            }
        })
        semaphore.acquire()
    }

    @Test
    fun removeFavCategoryTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null
        var favorites: String
        var favCatList: ArrayList<String>? = ArrayList()

        favInterface.removeFavoriteCategory("Favorite 6", object: IAsyncCallback {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(0))
            }

            override fun onSuccess() {
                favorites = sharedPreferences.getString("favorites", "")!!
                val temp = favorites.split(",")
                temp.forEach { item ->
                    if (item != " " && item != "")
                        favCatList?.add(item)
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: ${favCatList?.size}")
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(6))
            }
        })
        semaphore.acquire()
    }

    @Test
    fun renameFavCategoryTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null
        var favorites: String
        var favCatList: ArrayList<String>? = ArrayList()

        favInterface.renameFavoriteCategory("FAVORITE 1", "Favorite 1", object: IAsyncCallback {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(0))
            }

            override fun onSuccess() {
                favorites = sharedPreferences.getString("favorites", "")!!
                val temp = favorites.split(",")
                temp.forEach { item ->
                    if (item != " " && item != "")
                        favCatList?.add(item)
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: ${favCatList?.size}")
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(5))

            }
        })
        semaphore.acquire()
    }

    @Test
    fun getAvailableCategoryTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null
        var favorites: String
        var favCatList: ArrayList<String>? = ArrayList()

        favInterface.getAvailableCategories(object: IAsyncDataCallback<ArrayList<String>> {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(0))
            }

            override fun onReceive(data: ArrayList<String>) {
                favCatList = data
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(5))
            }
        })
        semaphore.acquire()
    }

    @Test
    fun getFavoritesForCategoryTest() = runTest {
        Mockito.`when`(mockChannelDataProviderInterface.getChannelList()).thenReturn(tvChannelList)

        val semaphore = Semaphore(0)
        var errorMessage: Error? = null
        var favCatList: ArrayList<FavoriteItem>? = ArrayList()

        favInterface.getFavoritesForCategory("Favorite 1", object: IAsyncDataCallback<ArrayList<FavoriteItem>> {
            override fun onFailed(error: Error) {
                errorMessage = error
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(favCatList,CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(0))
            }

            override fun onReceive(data: ArrayList<FavoriteItem>) {
                favCatList = data
                semaphore.release()
                MatcherAssert.assertThat(errorMessage, CoreMatchers.nullValue())
                MatcherAssert.assertThat(favCatList, CoreMatchers.notNullValue())
                //MatcherAssert.assertThat(favCatList?.size, CoreMatchers.`is`(5))
            }
        })
        semaphore.acquire()
    }

}
