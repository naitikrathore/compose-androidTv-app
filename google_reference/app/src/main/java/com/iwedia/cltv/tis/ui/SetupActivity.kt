package com.iwedia.cltv.tis.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.tv.TvInputManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.tis.helper.ScanHelper
import com.iwedia.cltv.tis.main.AnokiTvInputService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * Setup Activity for TvInputService
 *
 * @author Abhilash M R
 */
class SetupActivity : Activity() {
    private val TAG: String = this.toString()

    /**
     * Request code for runtime permissions
     */
    private val REQUEST_CODE = 123

    private  var btn: Button?= null
    private var scanStarted = false
    private var tuneBtn: Button?= null
    private var deleteBtn: Button? =null
    private var scanResults: TextView?= null

    companion object {
        var INPUT_ID = if (BuildConfig.FLAVOR.equals("base")) "com.cltv.fast/com.iwedia.cltv.tis.main.AnokiTvInputService"
        else if (BuildConfig.FLAVOR.equals("refplus5") || BuildConfig.FLAVOR.equals("rtk") || BuildConfig.FLAVOR.equals("t56"))
            "com.cltv.hybrid/com.iwedia.cltv.tis.main.AnokiTvInputService"
        else if (BuildConfig.FLAVOR.equals("mtk"))
            "com.cltv.cltv/com.iwedia.cltv.tis.main.AnokiTvInputService"
        else if (BuildConfig.FLAVOR.equals("mal_service")) {
            if (ReferenceWorldHandler.isFastOnly()) {
                "com.cltv.fast/com.iwedia.cltv.tis.main.AnokiTvInputService"
            } else {
                "com.cltv.hybrid/com.iwedia.cltv.tis.main.AnokiTvInputService"
            }
        } else "com.cltv.hybrid/com.iwedia.cltv.tis.main.AnokiTvInputService"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tis_setup_activity)

        checkRuntimePermissions()
        startService(Intent(this, AnokiTvInputService::class.java))

        for (input in (applicationContext.getSystemService(TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            val inputId = input.id
            Log.d(Constants.LogTag.CLTV_TAG + TAG, inputId)
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "mInputId is $INPUT_ID")

        btn = findViewById(R.id.btn_scan)
        tuneBtn = findViewById(R.id.btn_tune)
        tuneBtn?.clearFocus()
        deleteBtn = findViewById(R.id.btn_delete)
        deleteBtn?.clearFocus()
        scanResults = findViewById(R.id.scan_result)

        btn?.setOnClickListener {
            if (!scanStarted) {
                scanStarted = true
                Toast.makeText(
                    applicationContext,
                    "Scan started. Please wait to be finished.",
                    Toast.LENGTH_LONG
                ).show()
                // trigger server API through coroutine.
                CoroutineScope(Dispatchers.IO).launch {
                    getChannelData()
                }
                var prefs : SharedPreferences = applicationContext.getSharedPreferences("shared_prefs",0)
                var editor = prefs.edit()
                editor.putInt("scan_on_reboot",1)
                editor.commit()
            }
        }

        tuneBtn?.setOnClickListener {
            //val intent = Intent(this, PlayerActivity::class.java)
            //startActivity(intent)
        }

        deleteBtn?.setOnClickListener{
            ScanHelper.deleteChannels(this)
            var prefs : SharedPreferences = applicationContext.getSharedPreferences("shared_prefs",0)
            var editor = prefs.edit()
            editor.putInt("scan_on_reboot",0)
            editor.commit()
        }
    }

    private fun checkRuntimePermissions() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkRuntimePermissions")
        //Check runtime permissions
        var appPermissions = arrayListOf(
            "android.permission.INTERNET",
            "com.android.providers.tv.permission.READ_EPG_DATA",
            "com.android.providers.tv.permission.WRITE_EPG_DATA",
            "com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA"
        )

        if (BuildConfig.FLAVOR!="base") {
            appPermissions.add( "android.permission.READ_TV_LISTINGS")
            appPermissions.add("android.permission.WRITE_EXTERNAL_STORAGE")
            appPermissions.add("android.permission.READ_EXTERNAL_STORAGE")
        }

        val notGrantedPermissions = ArrayList<String>()
        for (permissionId in appPermissions) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    permissionId
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notGrantedPermissions.add(permissionId)
            }
        }

        if (!notGrantedPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                notGrantedPermissions.toTypedArray(),
                REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {

        if (grantResults.isNotEmpty()) {
            when (requestCode) {
                REQUEST_CODE -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Permission granted")
                    } else {
                        //TODO Handle scenario if denied
                    }
                }
                else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun getChannelData() {
        //todo - fetch country code from System api
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelData")
        ScanHelper.scanChannels(applicationContext) {
            runOnUiThread {
                scanResults?.text = "Number of found channels $it"
                btn?.visibility = View.GONE
                tuneBtn?.visibility = View.GONE
                deleteBtn?.visibility = View.GONE
                scanResults?.visibility = View.VISIBLE
                Timer().schedule(timerTask {
                    setResult(RESULT_OK)
                    finish()
                }, 2000)
            }
        }
    }
}