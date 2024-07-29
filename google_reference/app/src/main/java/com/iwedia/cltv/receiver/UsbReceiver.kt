package com.iwedia.cltv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import utils.information_bus.Event
import utils.information_bus.InformationBus

import world.SceneManager
import java.io.File

class UsbReceiver {
    val TAG = "UsbReceiver"
    val USB_ROOT_PATH = "/mnt/media_rw"
    var usbFormatDialogShown = false
    private val USB_DEVICES_PREFS = "UsbDevicesPreferences"
    private val KNOWN_USB_DEVICES = "KnownUsbDevices"

    private fun findDevicePath(): String {
        var mntMedia: File? = null
        if (File(USB_ROOT_PATH).exists()) {
            mntMedia = File(USB_ROOT_PATH)
        }

        if (mntMedia?.listFiles() != null) {
            val usbList: Array<File> = mntMedia.listFiles()
            if (usbList != null) {
                for (usb in usbList) {
                    if (!usb.name.contains("emulated") && !usb.name.contains("self")) {
                        return usb.path
                    }
                }
            }
        }
        return ""
    }

    private fun isNewDevices(context: Context?, uniqueId: String): Boolean {
        context?.let {
            val sharedPreferences = it.getSharedPreferences(USB_DEVICES_PREFS, Context.MODE_PRIVATE)
            val knownDevices = sharedPreferences.getStringSet(KNOWN_USB_DEVICES, mutableSetOf())?.toMutableSet()

            return if (knownDevices?.contains(uniqueId) == true) {
                false
            } else {
                knownDevices?.clear()
                knownDevices?.add(uniqueId)
                sharedPreferences.edit().putStringSet(KNOWN_USB_DEVICES, knownDevices).apply()
                true
            }
        }
        return false
    }
    fun registerForUSB() {
        // BroadcastReceiver for USB port
        val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                    InformationBus.submitEvent(Event(Events.USB_DEVICE_DISCONNECTED))
                    if (usbFormatDialogShown) {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            SceneManager.Action.DESTROY
                        )
                        usbFormatDialogShown = false
                    }
                }
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb attached");

                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb device device name: " + device!!.deviceName);
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb device manufacturer name: " + device!!.manufacturerName);
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb device product name: " + device!!.productName);
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb device version name: " + device!!.version);

                    val usbDevice: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    usbDevice?.let { device ->
                        val uniqueId = "${device.vendorId}:${device.productId}"

                        if (isNewDevices(context, uniqueId)) {
                            showDialogForUsbFormat()
                        }
                    }
                    InformationBus.submitEvent(Event(Events.USB_DEVICE_CONNECTED))
                }
            }
        }
        // listen for USB device
        var filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        ReferenceApplication.applicationContext()!!.registerReceiver(mUsbReceiver, filter)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showDialogForUsbFormat() {
        Handler().postDelayed({
            val storageManager: StorageManager = ReferenceApplication.applicationContext()
                .getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes: MutableList<StorageVolume> = storageManager.storageVolumes

            for (storageVolume in storageVolumes) {
                var volumePath = storageVolume.directory
                if (volumePath != null) {
                    if (!(storageVolume.directory.toString()
                            .contains("emulated"))
                    ) {     //invert for system memory
                        val statFs: StatFs = StatFs(volumePath.toString())
                        val availableBytes = statFs.availableBytes
                        if (availableBytes != null) {
                            ReferenceApplication.runOnUiThread {
                                var currentActiveScene = worldHandler!!.active
                                var sceneData = DialogSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
                                sceneData.type = DialogSceneData.DialogType.YES_NO
                                sceneData.title = ConfigStringsManager.getStringById("usb_format")
                                sceneData.positiveButtonText = ConfigStringsManager.getStringById("yes")
                                sceneData.negativeButtonText = ConfigStringsManager.getStringById("no")
                                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                                    override fun onNegativeButtonClicked() {
                                        worldHandler!!.triggerAction(
                                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                            SceneManager.Action.DESTROY
                                        )
                                        usbFormatDialogShown = false
                                    }

                                    override fun onPositiveButtonClicked() {
                                        var path = findDevicePath()
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb path $path")
                                        worldHandler!!.triggerAction(
                                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                            SceneManager.Action.DESTROY
                                        )
                                        usbFormatDialogShown = false

                                        val intent = Intent(Settings.ACTION_MEMORY_CARD_SETTINGS)
                                        ReferenceApplication.get().activity!!.startActivityForResult(intent, 0)
                                    }
                                }
                                worldHandler!!.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    SceneManager.Action.SHOW, sceneData
                                )
                                usbFormatDialogShown = true
                            }
                        }
                    }
                }
            }
        }, 10000)
    }
}