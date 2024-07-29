package com.iwedia.cltv.scene.custom_recording.channel


import android.util.Log
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.scene.custom_recording.ChannelAdapter
import world.widget.GWidget
import world.widget.GWidgetListener


class CustomRecordingsChannelSceneWidget(listener: ChannelSceneWidgetListener) :
    GWidget<ConstraintLayout, GWidgetListener>(0, 0, listener) {
    var configParam: SceneConfig? = null

    var requestFocus = false
    var focusedPosition = 0

    var channelAdapter: ChannelAdapter? = null
    var channelGridView: VerticalGridView? = null

    init {

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_channel, null) as ConstraintLayout

        focusedPosition = listener.getFocusPosition()
    }

    override fun refresh(data: Any) {

        if (data is MutableList<*>) {
            channelGridView =
                view!!.findViewById(R.id.channel_recycler)
            channelAdapter = ChannelAdapter()
            Log.i("TAG", "refresh: ${data.size}")
            channelAdapter?.refresh(data as MutableList<TvChannel>)

            Log.i("TAG", "refresh:data size ${data.size} ")
            channelGridView?.apply {

                setNumColumns(1)
                channelAdapter!!.selectedItem = focusedPosition
                adapter = channelAdapter

                channelAdapter?.adapterListener =
                    object : ChannelAdapter.ChannelAdapterListener {
                        override fun getAdapterPosition(position: Int) {
                        }

                        override fun onKeyLeft(currentPosition: Int): Boolean {
                            (listener as ChannelSceneWidgetListener).onLeftClicked()
                            return true
                        }

                        override fun onKeyRight(currentPosition: Int): Boolean {
                            return false
                        }

                        override fun onKeyUp(currentPosition: Int): Boolean {
                            return false
                        }

                        override fun onKeyDown(currentPosition: Int): Boolean {
                            return false
                        }

                        override fun onItemClicked(position: Int) {
                            (listener as ChannelSceneWidgetListener).onChannelItemClicked(position)
                        }

                        override fun setFocusedPosition(position: Int) {
                            focusedPosition = position
                            (listener as ChannelSceneWidgetListener).setFocusPosition(
                                focusedPosition
                            )

                        }


                    }
                scrollToPosition(focusedPosition)
            }

        }

    }
}