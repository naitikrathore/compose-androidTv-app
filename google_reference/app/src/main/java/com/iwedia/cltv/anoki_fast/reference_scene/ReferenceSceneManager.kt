package com.iwedia.cltv.anoki_fast.reference_scene

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import world.SceneData

private const val TAG = "ReferenceSceneManager"

abstract class ReferenceSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    id: Int,
    private val networkModule: NetworkInterface
) : GAndroidSceneManager(
    context, worldHandler, id
) {

    @RequiresApi(Build.VERSION_CODES.R)
    val isFastOnly = worldHandler.isFastOnly()

    init {
        registerGenericEventListener(Events.ETHERNET_EVENT)
        registerGenericEventListener(Events.NO_ETHERNET_EVENT)
    }

    /**
     * Checks if the network is available.
     *
     * @return `true` if the network is not in a `NoConnection` state, `false` otherwise.
     */
    private fun isNetworkAvailable(): Boolean {
        return networkModule.networkStatus.value != NetworkData.NoConnection
    }

    /**
     * Called when the Ethernet connection is restored.
     * Override this method to define custom behavior upon regaining Ethernet connectivity.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun onEthernetRestored() {
        Log.d(CLTV_TAG + TAG,"onEthernetRestored")

        destroyNoInternetDialog()
    }

    /**
     * Called when the Ethernet connection is lost.
     * Override this method to define custom behavior upon losing Ethernet connectivity.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun onEthernetLost() {
        Log.d(CLTV_TAG + TAG, "onEthernetLost")

        showNoInternetDialog()
    }

    /**
     * Handles received events and updates the scene accordingly.
     *
     * @param event The received event.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {

        when (event?.type) {
            Events.NO_ETHERNET_EVENT -> {
                onEthernetLost()
            }

            Events.ETHERNET_EVENT -> {
                onEthernetRestored()
            }
        }

        super.onEventReceived(event)
    }

    /**
     * Destroys the currently displayed no internet connection dialog.
     *
     * If the `isFastOnly` condition is true, it calls the `destroyNoInternetDialog` method
     * from the `MainActivity`. Otherwise, it triggers an action to destroy the dialog scene
     * in the `ReferenceApplication` world handler.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun destroyNoInternetDialog() {

        if (isFastOnly) {
            (ReferenceApplication.getActivity() as MainActivity).destroyNoInternetDialog()
            return
        }

        ReferenceApplication.worldHandler?.triggerAction(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY
        )
    }

    /**
     * Displays a dialog to notify the user that there is no internet connection.
     *
     * If the `isFastOnly` condition is true, it directly calls the `showNoInternetDialog` method
     * from the `MainActivity`. Otherwise, it creates and displays a custom dialog with options
     * to retry the connection or exit the on-demand content.
     *
     * The back button is disabled while this dialog is displayed.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showNoInternetDialog() {

        if (isFastOnly) {
            (ReferenceApplication.getActivity() as MainActivity).showNoInternetDialog()
            return
        }

        DialogSceneData(id, instanceId).apply {
            type = DialogSceneData.DialogType.YES_NO
            title = ConfigStringsManager.getStringById("no_internet_connection")
            message = ConfigStringsManager.getStringById("please_try_to_reconnect")
            positiveButtonText = ConfigStringsManager.getStringById("Retry")
            negativeButtonText = ConfigStringsManager.getStringById("exit_on_demand")
            isBackEnabled = false
            dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)

                    val sceneData = SceneData(id, instanceId, 4)

                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.HOME_SCENE, Action.SHOW, sceneData
                    )
                }

                override fun onPositiveButtonClicked() {
                    if (isNetworkAvailable()) {
                        destroyNoInternetDialog()
                    }
                }
            }

            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW_OVERLAY, this
            )
        }
    }

}