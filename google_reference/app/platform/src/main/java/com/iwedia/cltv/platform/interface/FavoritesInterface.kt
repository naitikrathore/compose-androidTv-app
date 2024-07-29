/**
 * Interface for managing TV channel favorites and categories.
 *
 * This interface provides methods to set up and manage favorite TV channels and their categories.
 */
package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.favorite.FavoriteItem

interface FavoritesInterface {

    /**
     * Set up the FavoritesInterface.
     */
    fun setup()

    /**
     * Dispose of resources used by the FavoritesInterface.
     */
    fun dispose()

    /**
     * Update a favorite item.
     *
     * @param item The [FavoriteItem] to update.
     * @param callback Callback to receive the result of the update operation.
     */
    fun updateFavoriteItem(item: FavoriteItem, callback: IAsyncCallback)

    /**
     * Check if a [FavoriteItem] is in the list of favorites.
     *
     * @param favoriteItem The [FavoriteItem] to check.
     * @param callback Callback to receive the result of the check operation.
     */
    fun isInFavorites(favoriteItem: FavoriteItem, callback: IAsyncDataCallback<Boolean>)

    /**
     * Get a list of favorite items.
     *
     * @param callback Callback to receive the list of favorite items.
     */
    fun getFavoriteItems(callback: IAsyncDataCallback<List<FavoriteItem>>)

    /**
     * Get a list of favorite items by type.
     *
     * @param type The type of favorites to retrieve.
     * @param callback Callback to receive the list of favorite items.
     */
    fun getFavoriteListByType(type: Int, callback: IAsyncDataCallback<List<FavoriteItem>>)

    /**
     * Get a list of favorite categories.
     *
     * @return An [ArrayList] of favorite category names.
     */
    fun geFavoriteCategories(): ArrayList<String>

    /**
     * Add a new favorite category.
     *
     * @param category The name of the new category.
     * @param receiver Callback to receive the result of the add operation.
     */
    fun addFavoriteCategory(category: String, receiver: IAsyncCallback)

    /**
     * Remove a favorite category.
     *
     * @param category The name of the category to remove.
     * @param receiver Callback to receive the result of the remove operation.
     */
    fun removeFavoriteCategory(category: String, receiver: IAsyncCallback)

    /**
     * Rename a favorite category.
     *
     * @param newName The new name for the category.
     * @param oldName The current name of the category.
     * @param receiver Callback to receive the result of the rename operation.
     */
    fun renameFavoriteCategory(newName: String, oldName: String, receiver: IAsyncCallback)

    /**
     * Get the available favorite categories.
     *
     * @param callback Callback to receive the list of available categories.
     */
    fun getAvailableCategories(callback: IAsyncDataCallback<ArrayList<String>>)

    /**
     * Get the list of favorite items for a specific category.
     *
     * @param category The name of the category.
     * @param callback Callback to receive the list of favorite items for the category.
     */
    fun getFavoritesForCategory(category: String, callback: IAsyncDataCallback<ArrayList<FavoriteItem>>)

    /**
     * Get a list of TV channels.
     *
     * @return An [ArrayList] of [TvChannel] objects.
     */
    fun getChannelList() : ArrayList<TvChannel>

    /**
     * Add favorite information to TV channels.
     */
    fun addFavoriteInfoToChannels()

    /**
     * To clear favorites while deleting all TV channels.
     */
    fun clearFavourites()
}