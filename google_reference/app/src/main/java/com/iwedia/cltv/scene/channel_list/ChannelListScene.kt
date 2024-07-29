package com.iwedia.cltv.scene.channel_list

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.ReferenceWidgetChannelList
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * Class ChannelListScene
 *
 * @author Aleksandar Milojevic
 */
class ChannelListScene(
    context: Context,
    sceneListener: ChannelListSceneListener
) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.CHANNEL_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CHANNEL_SCENE),
    sceneListener
) {

    /**
     * Scene container
     */
    var sceneContainer: ConstraintLayout? = null

    /**
     * Widget
     */
    var widget: ReferenceWidgetChannelList? = null

    var lastBackKeyTime = 0L

    //Current category position
    private var currentCategoryPosition: Int = -1
    private val TAG = "ChannelListScene"
    private var lastFocusedView: View? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            name,
            R.layout.layout_channel_list_scene,
            object : GAndroidSceneFragmentListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onCreated() {
                    try {
                        //todo view is null
                        sceneContainer = view!!.findViewById(R.id.scene_container)
                    }catch (E: Exception){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: ${E.printStackTrace()}")
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        return
                    }

                    widget = ReferenceWidgetChannelList(
                        context,
                        object : ReferenceWidgetChannelList.ChannelListWidgetListener {
                            override fun getActiveCategory(): String {
                                return (sceneListener as ChannelListSceneListener).getActiveCategory()
                            }

                            override fun getDateTimeFormat(): DateTimeFormat {
                                return (sceneListener as ChannelListSceneListener).getDateTimeFormat()
                            }

                            override fun isPvrPathSet(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isPvrPathSet()
                            }

                            override fun isUsbFreeSpaceAvailable(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isUsbFreeSpaceAvailable()
                            }

                            override fun isUsbStorageAvailable(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isUsbStorageAvailable()
                            }

                            override fun isUsbWritableReadable(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isUsbWritableReadable()
                            }

                            override fun onClickEditChannel() {
                                return (sceneListener as ChannelListSceneListener).onClickEditChannel()
                            }

                            override fun getConfigInfo(nameOfInfo: String): Boolean {
                                return (sceneListener as ChannelListSceneListener).getConfigInfo(nameOfInfo)
                            }

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as ChannelListSceneListener).setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                (sceneListener as ChannelListSceneListener).showToast(text, duration)
                            }

                            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                                (sceneListener as ChannelListSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                            }

                            override fun showRecordingStopPopUp(callback: IAsyncCallback) {
                                (sceneListener as ChannelListSceneListener).showRecordingStopPopUp(callback)
                            }

                            override fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>) {
                                (sceneListener as ChannelListSceneListener).getRecordingInProgress(object : IAsyncDataCallback<RecordingInProgress>{
                                    override fun onFailed(error: Error) { }

                                    override fun onReceive(data: RecordingInProgress) {
                                        callback.onReceive(data)
                                    }

                                })
                            }

                            override fun getChannelSourceType(tvChannel: TvChannel): String{
                                return (sceneListener as ChannelListSceneListener).getChannelSourceType(tvChannel)
                            }

                            override fun sortChannelList(channelList: MutableList<ChannelListItem>): MutableList<ChannelListItem> {
                                return (sceneListener as ChannelListSceneListener).sortChannelList(channelList)
                            }

                            override fun onAddFavoritesClicked(
                                tvChannel: TvChannel,
                                favListIds: java.util.ArrayList<String>
                            ) {
                                (sceneListener as ChannelListSceneListener).onAddFavoritesClicked(
                                    tvChannel,
                                    favListIds
                                )
                            }

                            override fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
                                (sceneListener as ChannelListSceneListener).getFavoritesCategory(callback)
                            }

                            override fun isAudioDescription(type: Int): Boolean {
                                return (sceneListener as ChannelListSceneListener).getIsAudioDescription(type)
                            }

                            override fun isHOH(type: Int): Boolean {
                                return (sceneListener as ChannelListSceneListener).getIsHOH(type)
                            }

                            override fun isDolby(type: Int): Boolean {
                                return (sceneListener as ChannelListSceneListener).getIsDolby(type)
                            }

                            override fun isTeleText(type: Int): Boolean {
                                return (sceneListener as ChannelListSceneListener).getTeleText(type)
                            }

                            override fun isParentalEnabled(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isParentalEnabled()
                            }

                            override fun getChannelList(): ArrayList<TvChannel> {
                                return (sceneListener as ChannelListSceneListener).getChannelList()
                            }

                            override fun onSelectedSortListPosition(position: Int) {
                                (sceneListener as ChannelListSceneListener).saveSelectedSortListPosition(
                                    position
                                )
                            }

                            override fun getSelectedSortListPosition(): Int {
                                return (sceneListener as ChannelListSceneListener).getSelectedSortListPosition()
                            }

                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
                            }

                            override fun onCategoryChannelClicked(position: Int) {
                                currentCategoryPosition = position
                                (sceneListener as ChannelListSceneListener).onCategoryChannelClicked(
                                    position
                                )
                            }

                            override fun onChannelClicked(tvChannel: core_entities.TvChannel) {

                            }

                            override fun channelClicked(tvChannel: TvChannel) {
                                (sceneListener as ChannelListSceneListener).onChannelItemClicked(
                                    tvChannel
                                )
                            }

                            override fun deleteChannel(tvChannel: TvChannel): Boolean {
                                return (sceneListener as ChannelListSceneListener).deleteChannel(
                                    tvChannel
                                )
                            }

                            override fun lockUnlockChannel(
                                tvChannel: TvChannel,
                                lockUnlock: Boolean,
                                callback: IAsyncCallback
                            ) {
                                 (sceneListener as ChannelListSceneListener).lockUnlockChannel(
                                    tvChannel,
                                    lockUnlock,
                                    callback
                                )
                            }

                            override fun skipUnskipChannel(
                                tvChannel: TvChannel,
                                skipUnskip: Boolean
                            ): Boolean {
                                return (sceneListener as ChannelListSceneListener).skipUnskipChannel(
                                    tvChannel,
                                    skipUnskip
                                )
                            }

                            override fun onSearchClicked() {
                                (sceneListener as ChannelListSceneListener).onSearchClicked()
                            }

                            override fun onRecordButtonPressed(tvEvent: TvEvent) {
                                (sceneListener as ChannelListSceneListener).onRecordButtonPressed(
                                    tvEvent
                                )
                            }

                            override fun onChannelListEmpty() {
                                (sceneListener as ChannelListSceneListener).onChannelListEmpty()
                            }

                            override fun getAvailableAudioTracks(): List<IAudioTrack> {
                                return (sceneListener as ChannelListSceneListener).getAvailableAudioTracks()
                            }

                            override fun getAvailableSubtitleTracks(): List<ISubtitle> {
                                return (sceneListener as ChannelListSceneListener).getAvailableSubtitleTracks()
                            }

                            override fun getActiveChannel(): TvChannel {
                                return (sceneListener as ChannelListSceneListener).getActiveChannel()
                            }

                            override fun addDeletedChannel(tvChannel: TvChannel) {
                                (sceneListener as ChannelListSceneListener).addDeletedChannel(tvChannel)
                            }

                            override fun getWatchlist(): MutableList<ScheduledReminder>? {
                                return (sceneListener as ChannelListSceneListener).getWatchlist()
                            }

                            override fun removeScheduledReminder(reminder: ScheduledReminder){
                                (sceneListener as ChannelListSceneListener).removeScheduledReminder(reminder)
                            }

                            override fun onActiveChannelDeleted() {
                                (sceneListener as ChannelListSceneListener).onActiveChannelDeleted()
                            }

                            override fun isClosedCaptionEnabled(): Boolean? {
                                return (sceneListener as ChannelListSceneListener).isClosedCaptionEnabled()
                            }

                            override fun getClosedCaption(): String? {
                                return (sceneListener as ChannelListSceneListener).getClosedCaption()
                            }

                            override fun setClosedCaption(): Int? {
                                return (sceneListener as ChannelListSceneListener).setClosedCaption()
                            }

                            override fun getAudioChannelInfo(type: Int): String {
                                return (sceneListener as ChannelListSceneListener).getAudioChannelInfo(type)
                            }

                            override fun getAudioFormatInfo(): String {
                                return (sceneListener as ChannelListSceneListener).getAudioFormatInfo()
                            }

                            override fun getVideoResolution(): String {
                                return (sceneListener as ChannelListSceneListener).getVideoResolution()
                            }

                            override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                                return (sceneListener as ChannelListSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                            }

                            override fun getCurrentTime(tvChannel: TvChannel): Long {
                                return (sceneListener as ChannelListSceneListener).getCurrentTime(tvChannel)
                            }

                            override fun getCurrentAudioTrack(): IAudioTrack? {
                                return (sceneListener as ChannelListSceneListener).getCurrentAudioTrack()
                            }

                            override fun isCCTrackAvailable(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isCCTrackAvailable()
                            }
                            
                            override fun getInstalledRegion(): Region {
                                return (sceneListener as ChannelListSceneListener).getInstalledRegion()
                            }

                            override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as ChannelListSceneListener).isEventLocked(tvEvent)

                            override fun getCurrentSubtitleTrack(): ISubtitle? {
                                return (sceneListener as ChannelListSceneListener).getCurrentSubtitleTrack()
                            }

                            override fun isSubtitlesEnabled(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isSubtitlesEnabled()
                            }

                            override fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback) {
                                (sceneListener as ChannelListSceneListener).stopRecordingByChannel(
                                    tvChannel, callback
                                )
                            }

                            override fun isScrambled(): Boolean {
                                return (sceneListener as ChannelListSceneListener).isScrambled()
                            }
                        })

                    sceneContainer!!.addView(widget!!.view)
                    sceneListener.onSceneInitialized()
                }
            })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        if(widget != null && data != null) {
            widget!!.refresh(data!!)
        }
        when (lastFocusedView) {
            widget?.channelCategoryGridView -> widget?.channelCategoryGridView?.requestFocus()
            widget?.channelListGridView -> widget?.channelListGridView?.requestFocus()
            widget?.searchCustomButton -> widget?.searchCustomButton?.requestFocus()
            widget?.filterCustomButton -> widget?.filterCustomButton?.requestFocus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun refreshCurrentEvent(currentTime: Long) {
        widget?.refreshCurrentEvent(currentTime)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        if (!widget!!.isLockedScene && lastFocusedView == null && !widget!!.isPerformingChannelEdit) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: currentCategoryPosition $currentCategoryPosition")
            if (currentCategoryPosition >= 0) {
                (sceneListener as ChannelListSceneListener).onCategoryChannelClicked(
                    currentCategoryPosition
                )
            }
            widget!!.isLockedScene = false
            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDestroy() {
        sceneContainer = null

        if(widget != null && widget!!.favoritesOverlay!!.isVisible){
            widget!!.saveChannelInFav()
        }
        try {
            if(widget!=null) {
                widget!!.dispose()
                widget = null
            }
        } catch (E: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onDestroy: ${E.printStackTrace()}")
        }
        super.onDestroy()
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        //this method is called to restart inactivity timer for channel list scene
        (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()
        try {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
                if (widget != null && widget!!.searchCustomButton != null && widget!!.searchCustomButton!!.hasFocus()) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

                        if (ViewCompat.getLayoutDirection(view!!.fragmentView!!) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                            return true
                        } else {
                            widget!!.channelCategoryGridView!!.requestFocus()
                            return true
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {

                        if (ViewCompat.getLayoutDirection(view!!.fragmentView!!) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                            widget!!.channelCategoryGridView!!.requestFocus()
                            return true
                        } else {
                            return true
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        widget!!.channelListGridView!!.requestFocus()
                        return true
                    }
                }
                if (widget != null && widget!!.filterCustomButton != null && widget!!.filterCustomButton!!.hasFocus()) {

                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return if (ViewCompat.getLayoutDirection(view!!.fragmentView!!) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                            widget!!.channelCategoryGridView!!.requestFocus()
                            Utils.focusAnimation(
                                widget!!.channelCategoryGridView!!.get(
                                    currentCategoryPosition
                                )
                            )
                            true
                        } else {
                            true
                        }
                    }

                }

                when (keyCode) {
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                        val digit = keyCode - KeyEvent.KEYCODE_0
                        (sceneListener as ChannelListSceneListener).digitPressed(digit)
                        return true
                    }

                    KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                        val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                        (sceneListener as ChannelListSceneListener).digitPressed(digit)
                        return true
                    }
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (System.currentTimeMillis() - lastBackKeyTime < 100) {
                    return true
                }
                lastBackKeyTime = System.currentTimeMillis()
                if (widget!!.channelListGridView!!.hasFocus()) {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
                            widget!!.channelCategoryGridView!!.requestFocus()
                            widget!!.eventDetailsContainer!!.visibility = View.GONE
                        }
                        return true
                    }
                    return true
                }
                if (widget!!.sortByContainer!!.hasFocus()) {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        widget!!.sortByContainer!!.visibility = View.GONE
                        widget!!.sortByContainer!!.translationZ = 0f
                        widget!!.filterCustomButton!!.requestFocus()
                        return true
                    }
                    return true
                }
            }
        }catch (E: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "dispatchKeyEvent: ${E.printStackTrace()}")
            ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            return false
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun onPause() {
        (ReferenceApplication.getActivity() as MainActivity).stopSceneInactivityTimer()
        super.onPause()
        lastFocusedView = (ReferenceApplication.getActivity()).currentFocus
    }

    /**
     * Refresh guide details favorite button
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun refreshFavButton(channelList: MutableList<TvChannel>) {
        if (widget != null) {
            ReferenceApplication.runOnUiThread(Runnable {
                widget?.refreshFavoriteButton(channelList)
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun refreshCategoryList(categoryList: ArrayList<Category>) {
        if (widget != null) {
            widget?.refreshCategoryList(categoryList)
        }
    }

    /**
     * Refresh guide details recording button
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun refreshRecButton(tvChannel: TvChannel?) {
        if (widget != null) {
            ReferenceApplication.runOnUiThread(
                Runnable {
                    widget?.refreshRecordButton()
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun blockRecordButton() {
        if (widget != null) {
            ReferenceApplication.runOnUiThread {
                widget!!.blockRecordButton()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun unblockRecordButton() {
        if (widget != null) {
            ReferenceApplication.runOnUiThread {
                widget!!.unblockRecordButton()
            }
        }
    }

    /**
     * Get active category name
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun getActiveCategoryName(): String {
        return widget!!.getActiveCategoryName()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setActiveCategory(index: Int) {
        widget?.setActiveCategory(index)
    }
}