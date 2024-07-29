package com.iwedia.cltv.platform.refplus5

import android.app.usage.StorageStatsManager
import android.content.Context
import android.media.tv.TvView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.base.TimeshiftInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.mediatek.dm.MountPoint
import com.mediatek.wwtv.tvcenter.util.MtkLog
import com.mediatek.wwtv.tvcenter.util.TVAsyncExecutor
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.R)
internal class TimeshiftInterfaceImpl(private var playerInterface: PlayerInterface, private var  utilsInterface: UtilsInterface , private var context: Context): TimeshiftInterfaceBaseImpl(playerInterface, utilsInterface, context){

    private val TAG = javaClass.simpleName
    private var addLoading = false
    private var startedTimeShift = false
    private var formatListener: UtilsInterface.FormattingProgressListener? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun timeShiftPause(callback: IAsyncCallback) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "timeShiftPause: TIMESHIFT PUASE IN PLAYERHANDLER $isTimeShiftPaused")

        var list: ArrayList<MountPoint>? = getMountPointListLite(context) //get list of all usb devices - taken from mtk app
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "LIST size = " + list?.size);

        //in case the list is empty send failed callback
        if(list == null || list?.size == 0){
            callback.onFailed(Error("No usb device detected"))
            return
        }

        list.forEachIndexed { index, mountPoint ->
            val tspath: String = mountPoint.mMountPoint
            if(tspath == utilsInterface.getTimeshiftStoragePath()){
                selection = index
            }
        }

        //for timeshift to work we need to have tv view set
        if(getLiveTvView() == null){
            callback.onFailed(Error("Live tv view is not initialized"))
            return
        }

        //try to check if timeshift file exists on device
        try {
            checkForFile(callback)
        }catch (E: Exception){
            println(E)
            callback.onFailed(Error("Exception in checking the file system"))
            return
        }

        //if it we dont have a ts file block ts module until its created
        if (addLoading){
            callback.onFailed(Error("Please wait for file to be made"))
            return
        }

        //check state of timeshift (if its paused or resumed)
        if (isTimeShiftPaused) {
            getLiveTvView()!!.timeShiftResume()
            isTimeShiftPaused = false
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftPause: LIVETVVIEW TIMESHIFT PAUSE CALLED")

            //we need to set up bundle and call the bundle for timeshift to work on ref 5 boards - this is not tif api mtk
            setUsbForTimeshift()

            if(startedTimeShift){
                getLiveTvView()!!.timeShiftPause()
                isTimeShiftPaused = true
            }

            if (timeShiftPositionCallback == null) {
                setTimeShiftPositionCallback()
            }
        }
        isTimeShiftActive = true
        callback.onSuccess()
    }

    //the first timeshift call can only be called once tv view is set to timeshift mode internally - this is not how tif api works mtk
    override fun realPause(callback: IAsyncCallback){
        setUsbForTimeshift() //this might be an extra
        getLiveTvView()!!.timeShiftPause()
        isTimeShiftPaused = true
        startedTimeShift = true
        callback.onSuccess()
    }

    //since we are using non tif methods for timeshift to work we need on timeshift exit to call these methods so ts file on usb is not damaged/corrupted
    override fun stopTimeshift() {
        getLiveTvView()!!.sendAppPrivateCommand("STOP_TIMESHIFT_PB", Bundle())
        getLiveTvView()?.sendAppPrivateCommand("SET_SCREEN_MODE", Bundle())
        getLiveTvView()?.sendAppPrivateCommand("SET_FILE_PATH", Bundle());
        SaveValue.saveWorldValue(context, "timeshift_record", "0", false)
        startedTimeShift = false
    }

    override fun timeShiftStop(callback: IAsyncCallback) {
        /**
         * When ever timeshift is stopped, we need to handle sending the data to middleware
         */
        stopTimeshift()
        super.timeShiftStop(callback)
    }

    fun setUsbForTimeshift(){
        val path: String? = SaveValue.readWorldStringValue(context, "timeshift_path") //get path of usb
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPreferenceTreeClick path = " + path);

        SaveValue.saveWorldValue(context, "timeshift_mode", "1", false) //save timeshift mode to on

        var filepath = path + "/timeshift/filecontext_0000.ts"; //set path of ts file for timeshift
        val testFile = File(filepath)
        Log.d(Constants.LogTag.CLTV_TAG + "TimeShiftModeFragment", "testFile.length() = " + testFile.length());
        Log.d(Constants.LogTag.CLTV_TAG + "TimeShiftModeFragment", "testFile.exists() = " + testFile.exists());
//        val array = intArrayOf(1, 1, 1, 1)//todo izvuci - pretpostavka da je ovo za parental kanal
        val bundle = Bundle()
//        bundle.putIntArray(CAM_PIN, array);
        bundle.putString("tshift_file_path", "$path/timeshift") //place file path to bundle

        getLiveTvView()?.sendAppPrivateCommand("SET_FILE_PATH", bundle); //send bundle to tv view
        SaveValue.saveWorldValue(context, "timeshift_record", "1", false) //save timeshift recorde to on
    }

    //get list of all usb devices - taken from mtk app
    @RequiresApi(Build.VERSION_CODES.R)
    fun getMountPointList(ctxt: Context): ArrayList<MountPoint>? {
        Log.i(TAG, "getMountPointList")
        val list = ArrayList<MountPoint>()
        val storageManager = ctxt.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val stats = ctxt.getSystemService(
            StorageStatsManager::class.java
        )
        try {
            val volumes = storageManager.storageVolumes
            if (volumes == null || 0 == volumes.size) {
            } else {
                Log.i(TAG, "volumes.length:" + volumes.size.toString())
                for (item in volumes) {
                    val path = item.uuid
                    val state = item.state
                    var directory: String? = null
                    if (item.directory != null) {
                        directory = item.directory.toString()
                    }
                    Log.i(TAG, "StorageVolume path state:$path  $state")
                    Log.i(TAG, "StorageVolume getUuid:" + item.uuid)
                    Log.i(TAG, "StorageVolume getDescription:" + item.getDescription(ctxt))
                    Log.i(TAG, "StorageVolume getDirectory():" + item.directory)
                    Log.i(
                        TAG,
                        "StorageVolume getMediaStoreVolumeName():" + item.mediaStoreVolumeName
                    )
                    Log.i(TAG, "StorageVolume getState():" + item.state)
                    if (state == null || path == null || state != Environment.MEDIA_MOUNTED) {
                        continue
                    }
                    //String curFilePath = currentDir.toString();

                    //directory = directory.replace("storage","mnt/media_rw");
                    val stat = StatFs(directory)
                    Log.i(TAG, "StorageVolume getDirectory():$directory")
                    //final UUID uuid = storageManager.getUuidForPath(new File(directory));
                    //final long total = stats.getTotalBytes(uuid);
                    //final long free = stats.getFreeBytes(uuid);
                    val bytesAvailable = stat.availableBytes
                    val bytesTotal = stat.totalBytes
                    val megaBytesAvailable = bytesAvailable / (1024 * 1024)
                    val megaBytesTotal = bytesTotal / (1024 * 1024)
                    val mountpoint = MountPoint("","","")
                    mountpoint.mMountPoint = directory
                    mountpoint.mVolumeLabel = item.getDescription(ctxt)
                    mountpoint.mTotalSize = bytesTotal
                    mountpoint.mFreeSize = bytesAvailable
                    //mountpoint.mStatus = state;
                    Log.i(TAG, "**************************")
                    Log.i(TAG, "StorageVolume path state:$path  $state")
                    Log.i(TAG, "StorageVolume total free:$bytesTotal/$bytesAvailable")
                    Log.i(TAG, "StorageVolume getUuid:" + item.uuid)
                    Log.i(TAG, "StorageVolume getDescription:" + item.getDescription(ctxt))
                    Log.i(TAG, "StorageVolume getDirectory():$directory")
                    Log.i(
                        TAG,
                        "StorageVolume getMediaStoreVolumeName():" + item.mediaStoreVolumeName
                    )
                    Log.i(TAG, "StorageVolume getState():" + item.state)
                    Log.i(TAG, "**************************")
                    //DvrLog.i(TAG, "StorageVolume getStorageUuid():" + item.getStorageUuid());
                    list.add(mountpoint)
                }
            }
        } catch (e: SecurityException) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "permission.PACKAGE_USAGE_STATS")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception $e")
        }
        Log.i(TAG, "list size = " + list.size)
        return list
    }

    //register listener for usb creation of timeshift ts file
    override fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener) {
        if(selection == null){
            return
        }
        formatListener = listener
        InformationBus.informationBusEventListener?.submitEvent(Events.START_USB_FORMATING)

        CoroutineHelper.runCoroutine({
            //create a ts file and call finish to remove the overlay screen
            val realPath: String = getMountPointList(context)?.get(selection!!)?.mMountPoint!!//get list of all usb devices - taken from mtk app as is
            var path_temp: String = realPath + "/timeshift/filecontext_0000.ts"
            createTimeshiftFile(path_temp, 1024)
            addLoading = false
            formatListener?.onFinished(true)
        })
    }

    private fun checkForFile(callback: IAsyncCallback){
        if(selection == null){
            callback.onFailed(Error("Please select storage path for Recording Timeshift in Preferences"))
            return
        }
        try {
            //check the usb device - taken from mtk app as is
            checkForUsb(selection!!)
        }catch(E: Exception){
            println(E)
            callback.onFailed(Error("Some thing is wrong with the usb device"))
            return
        }

        val realPath: String = getMountPointList(context)?.get(selection!!)?.mMountPoint!!
        val mSize = getMountPointList(context)?.get(selection!!)?.mFreeSize!!
        println("checkForFile realPath $realPath mSize $mSize")

        val dir: File = File(realPath + "/timeshift")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        var path_temp: String = realPath + "/timeshift/filecontext_0000.ts"
        val timeshiftFile = File(path_temp)
        if (!timeshiftFile.exists()) {
            addLoading = true
            //this is the space needed to create the ts file that's why we have a check here
            if(mSize < 1024L * 1024 * 1024){
                //todo add toast no more space on device
                callback.onFailed(Error("No space on usb device"))
                return
            }
            InformationBus.informationBusEventListener?.submitEvent(Events.SET_FILE_TO_USB)
            callback.onFailed(Error("please wait for usb to be set"))
            return
        }
    }

    //this function is used to create a ts file needed for timeshift to work
    private fun createTimeshiftFile(path: String, size: Int) {
        val testFile_temp = File(path)
        var raf: RandomAccessFile? = null
        if (testFile_temp.exists()) {
            testFile_temp.delete()
        }
        try {
            testFile_temp.createNewFile()
            raf = RandomAccessFile(testFile_temp, "rw")
            raf.setLength(1024L * 1024 * size) //this eats the cpu up and takes long time(3 minutes on average)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                raf?.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    //bellow are functions taken from mtk app
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkForUsb(selection: Int): Boolean {
        MtkLog.i("DiskSettingFragement", "setTSHIFT() selection = $selection")
        val diskPath: String? = getMountPointList(context)?.get(selection)?.mMountPoint
        MtkLog.i("DiskSettingFragement", "setTSHIFT() diskPath = $diskPath")
        tempSetTHIFT(context, diskPath ?: "", getMountPointList(context)?.get(selection)?.mFreeSize ?: 0)
        return false
    }

    private fun tempSetTHIFT(context: Context, diskPath: String, freesize: Long) {
        Log.i(TAG, "tempSetTHIFT diskPath = $diskPath, freesize = $freesize")
        if(diskPath != "" && !freesize.equals(0)) {
            SaveValue.saveWorldValue(context, "timeshift_path", diskPath, false)
            SaveValue.saveWorldValue(context, "timeshift_freesize", freesize.toString(), false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getMountPointListLite(ctxt: Context): ArrayList<MountPoint>? {
        Log.i(TAG, "getMountPointListLite")
        val list = ArrayList<MountPoint>()
        val storageManager = ctxt.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val stats = ctxt.getSystemService(
            StorageStatsManager::class.java
        )
        try {
            val volumes = storageManager.storageVolumes
            if (volumes == null || 0 == volumes.size) {
            } else {
                Log.i(TAG, "volumes.length:" + volumes.size.toString())
                for (item in volumes) {
                    val path = item.uuid
                    val state = item.state
                    var directory: String? = null
                    if (item.directory != null) {
                        directory = item.directory.toString()
                    }
                    Log.i(TAG, "StorageVolume path state:$path  $state")
                    Log.i(TAG, "StorageVolume getUuid:" + item.uuid)
                    Log.i(TAG, "StorageVolume getDescription:" + item.getDescription(ctxt))
                    Log.i(TAG, "StorageVolume getDirectory():" + item.directory)
                    Log.i(
                        TAG,
                        "StorageVolume getMediaStoreVolumeName():" + item.mediaStoreVolumeName
                    )
                    Log.i(TAG, "StorageVolume getState():" + item.state)
                    if (state == null || path == null || state != Environment.MEDIA_MOUNTED) {
                        continue
                    }
                    val mountpoint: MountPoint = MountPoint("","","")
                    mountpoint.mMountPoint = directory
                    mountpoint.mVolumeLabel = item.getDescription(ctxt)

                    //mountpoint.mStatus = state;
                    Log.i(TAG, "**************************")
                    Log.i(TAG, "StorageVolume path state:$path  $state")
                    Log.i(TAG, "StorageVolume getUuid:" + item.uuid)
                    Log.i(TAG, "StorageVolume getDescription:" + item.getDescription(ctxt))
                    Log.i(TAG, "StorageVolume getDirectory():$directory")
                    Log.i(
                        TAG,
                        "StorageVolume getMediaStoreVolumeName():" + item.mediaStoreVolumeName
                    )
                    Log.i(TAG, "StorageVolume getState():" + item.state)
                    Log.i(TAG, "**************************")
                    //DvrLog.i(TAG, "StorageVolume getStorageUuid():" + item.getStorageUuid());
                    list.add(mountpoint)
                }
            }
        } catch (e: SecurityException) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "permission.PACKAGE_USAGE_STATS")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception $e")
        }
        Log.i(TAG, "list size = " + list.size)
        return list
    }
}