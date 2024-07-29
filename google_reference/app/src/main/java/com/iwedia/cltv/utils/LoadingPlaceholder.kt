package com.iwedia.cltv.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Creates LoadingPlaceholder which can be shown and hidden MULTIPLE TIMES. Initially LoadingPlaceholder is not shown - in order to be shown, showLoadingPlaceholder() method must be called.
 * @param placeholderViewId id which points to the layout file in app resource that should be used as placeholder.
 * @param parentConstraintLayout ConstraintLayout which will be used to host placeholderView.
 * @param name used for internally saving LoadingPlaceholder objects in order to avoid creating multiple instances with the same id (for one widget there is no need to have multiple instances for it's placeholder)
 * @author Boris Tirkajla
 */
class LoadingPlaceholder(
    private val context: Context,
    private val placeholderViewId: Int,
    private val parentConstraintLayout: ConstraintLayout,
    private val name: PlaceholderName
) {
    private lateinit var loadingView: View

    companion object {
        private val startedLoadingPlaceholdersMap = HashMap<PlaceholderName, Boolean>()
        private val listOfRegisteredPlaceholders = mutableListOf<LoadingPlaceholder>()

        /**
         * Every LoadingPlaceholder which was created is automatically registered internally. This method will hide all Placeholders which have been created.
         */
        fun hideAllRegisteredLoadingPlaceholders() {
            listOfRegisteredPlaceholders.forEach {
                it.hideLoadingPlaceholder()
            }
        }

        /**
         * @param onHidden executes right before hiding the LoadingPlaceholder.
         * * Example: visibility of some Views can be changed here.
         * */
        fun hideLoadingPlaceholder(
            placeholderName: PlaceholderName, onHidden: () -> Unit = {}
        ) {
            val placeholder = listOfRegisteredPlaceholders.filter {
                it.name == placeholderName
            }
            if (placeholder.isEmpty()) {
                throw IllegalArgumentException("There is no registered placeholder with specified name.")
            }
            placeholder[0].hideLoadingPlaceholder(onHidden)
        }

        /**
         * @param onShown executes right before showing the LoadingPlaceholder.
         * * Example: visibility of some Views can be changed here.
         * */
        fun showLoadingPlaceholder(
            placeholderName: PlaceholderName, onShown: () -> Unit = {}
        ) {
            val placeholder = listOfRegisteredPlaceholders.filter {
                it.name == placeholderName
            }
            if (placeholder.isEmpty()) {
                throw IllegalArgumentException("There is no registered placeholder with specified name.")
            }
            placeholder[0].showLoadingPlaceholder(onShown)
        }

        /**
         * @return true if LoadingPlaceholder with passed name is being shown, otherwise false
         * * Example: this method can be called when some code have to be executed only if LoadingPlaceholder is being visible
         */
        fun isCurrentStateShow(placeholderName: PlaceholderName) =
            startedLoadingPlaceholdersMap[placeholderName]
    }

    init {
        initializeLoadingPlaceholder()
    }

    private fun registerLoadingPlaceholder(placeholder: LoadingPlaceholder) {
        listOfRegisteredPlaceholders.add(placeholder)
    }
    private fun showLoadingPlaceholder(onShown: () -> Unit = {}) {
        onShown()
        startedLoadingPlaceholdersMap[name] = true
        loadingView.visibility = View.VISIBLE // show placeholder View
    }


    private fun hideLoadingPlaceholder(onHidden: () -> Unit = {}) {
        onHidden()
        startedLoadingPlaceholdersMap[name] = false
        loadingView.visibility = View.INVISIBLE // hide placeholder View
    }

    private fun initializeLoadingPlaceholder() {

        var duplicatedPlaceholder: LoadingPlaceholder? = null

        listOfRegisteredPlaceholders.forEach {
            if (it.name == this.name) { // loadingPlaceholder with this name has been already created before
                duplicatedPlaceholder = it
            }
        }
        duplicatedPlaceholder?.let { // if there was placeholder from before, it is being reused - it's loadingView is detached from parent and added to new parentConstraintLayout that has been passed
            val placeholderLoadingView = duplicatedPlaceholder!!.loadingView // this view should be detached from parent and then attached to new parent
            (placeholderLoadingView.parent as ViewGroup).removeView(placeholderLoadingView)
            parentConstraintLayout.addView(placeholderLoadingView)
            // set it's startedLoadingPlaceholdersMap to false in case it was true from before
            startedLoadingPlaceholdersMap[name] = false
            return
        }
        // this code will be executed only if there wasn't placeholder with specified name created already before.
        loadingView = LayoutInflater.from(context).inflate(placeholderViewId, null, false)
        parentConstraintLayout.addView(loadingView)
        loadingView.visibility = View.INVISIBLE // initially hide placeholder View
        registerLoadingPlaceholder(this) // register it only if it is new instance
    }
}

/**
 * PlaceholderName enum class is used to save all loadingPlaceholder IDs.
 * * Usage: when creating new LoadingPlaceholder, it's name must be created in this enum class.
 *
 * * Example: FOR_YOU - is used for LoadingPlaceholder created for For You widget in HomeScene.kt
 */

enum class PlaceholderName {
    FOR_YOU,
    RECORDINGS,
    GUIDE,
    CHANNEL_LIST,
    INFO_BANNER,
    SEARCH_SCENE,
    CHANNEL_LIST_SEARCH_SCENE
}