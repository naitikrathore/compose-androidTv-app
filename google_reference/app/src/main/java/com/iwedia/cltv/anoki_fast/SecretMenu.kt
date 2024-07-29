package com.iwedia.cltv.anoki_fast

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.components.CheckListAdapter
import com.iwedia.cltv.components.CheckListItem
import com.iwedia.cltv.components.FadeAdapter
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.fast_backend_utils.FastUrlHelper
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.tis.helper.ScanHelper

class SecretMenu(
    private val context: Context,
    private val secretMenuListener: SecretMenuListener,
    backCallback: (buttonId: Int) -> Unit
) : ConstraintLayout(context) {

    private var serverGridView: VerticalGridView
    var serverWrapperLinearLayout: LinearLayout? = null
    private var serverCheckListAdapter: CheckListAdapter
    private var title: TextView? = null
    var selectedServer: String? = null
    private var fadingEdgeLayout: FadingEdgeLayout
    private var serverList: Map<Int, String>? = null

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.fast_audio_subtitles_list_layout, this, true)
        title = findViewById(R.id.title)

        serverGridView = findViewById(R.id.side_view_vertical_grid_view)
        serverWrapperLinearLayout = findViewById(R.id.audio_and_subtitles_container)

        Utils.makeGradient(
            view = serverWrapperLinearLayout!!,
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            listOfColor = intArrayOf(
                Color.parseColor("#1a202b"),
                Color.parseColor("#22272f"),
                Color.parseColor("#00000000")
            )
        )

        serverGridView.setNumColumns(1)
        fadingEdgeLayout = findViewById(R.id.fading_edge_layout)
        serverCheckListAdapter = CheckListAdapter(
            fadingEdgeLayout = fadingEdgeLayout,
            FadeAdapter.FadeAdapterType.VERTICAL,
            preventClip = true
        )

        serverCheckListAdapter.adapterListener =
            object : CheckListAdapter.CheckListAdapterListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                    selectedServer = serverList!![position]
                    FastUrlHelper.saveSelectedUrlIndex(context, position)
                    ScanHelper.deleteChannelsAndAllTimeStampsAndRestartApp(context)
                }

                override fun onAdditionalItemClicked() {}

                override fun onUpPressed(position: Int): Boolean {
                    val nextPosition = if (position > 0) position - 1 else position
                    serverGridView.layoutManager?.findViewByPosition(nextPosition)
                        ?.requestFocus()
                    return true
                }

                override fun onDownPressed(position: Int): Boolean {
                    val nextPosition = if (position < serverList!!.size) position + 1 else position
                    serverGridView.layoutManager?.findViewByPosition(nextPosition)
                        ?.requestFocus()
                    return true
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    secretMenuListener.setSpeechText(text = text, importance = importance)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    secretMenuListener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

                override fun onBackPressed(): Boolean {
                    serverWrapperLinearLayout!!.visibility = View.GONE
                    backCallback(1)
                    return true
                }
            }


    }

    private fun setUpUrlList(): Boolean {
        serverList = FastUrlHelper.getServerList()
        serverGridView.adapter = serverCheckListAdapter

        if (serverList.isNullOrEmpty()) {
            secretMenuListener.showToast("No available Urls ")
            return false
        }
        return true
    }

    fun showUrlList(context: Context?) {
        val areUrlListSuccessfullyLoaded  = setUpUrlList()
        if (areUrlListSuccessfullyLoaded.not()) return

        visibility = View.VISIBLE

        selectedServer = serverList?.get(FastUrlHelper.getSelectedUrlIndex(context!!))

        var currentSelectedPosition = 0

        val urlCheckListItems = mutableListOf<CheckListItem>()
        serverList!!.forEach { (index, server) ->
            if (server == selectedServer) {
                urlCheckListItems.add(CheckListItem(server, true))
                currentSelectedPosition = index
            } else {
                urlCheckListItems.add(CheckListItem(server, false))
            }
        }

        title!!.text = "Choose server"
        serverCheckListAdapter.refresh(urlCheckListItems)

        serverWrapperLinearLayout!!.postDelayed({
            serverWrapperLinearLayout!!.visibility = View.VISIBLE
            serverGridView.layoutManager!!.scrollToPosition(currentSelectedPosition)
            serverGridView.requestFocus()

        }, 100)
    }

    interface SecretMenuListener: TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {

    }
}