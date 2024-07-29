package com.iwedia.cltv.components

import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils

class PrefContainer(
    container: ViewGroup,
    val parentHolder: RecyclerView.ViewHolder,
    itemList: MutableList<PrefItem<Any>>,
    listener: PrefContainerListener
) {
    var view: ViewGroup? = null
    var adapter: PreferenceSubMenuAdapter? = null
    var recyclerView: VerticalGridView
    var hintText: TextView? = null


    init {
        view = LayoutInflater.from(container.context).inflate(R.layout.pref_container, container, false) as ViewGroup

        recyclerView = view!!.findViewById(R.id.gridView)
        hintText = view?.findViewById(R.id.hint_text)

        hintText?.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        hintText?.setTextColor(
            Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"))
        )


/*        recyclerView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        recyclerView!!.windowAlignmentOffsetPercent =25f
        val rect = Rect()
        parentHolder.itemView.getGlobalVisibleRect(rect)
        val lp = view!!.layoutParams as ConstraintLayout.LayoutParams
        lp.topMargin = rect.top - Utils.getDimensInPixelSize(R.dimen.custom_dim_170)
        lp.height = Resources.getSystem().getDisplayMetrics().heightPixels  - rect.top
        lp.leftMargin =rect.width() + rect.left +Utils.getDimensInPixelSize(R.dimen.custom_dim_60)
        view!!.layoutParams = lp*/

        var isParentList = false
        var isEditChannel = false
        var isMoveChannel = false
        run exitForEach@{
            itemList.forEach {
                if (it.viewHolderType == ViewHolderType.VT_MENU) {
                    isParentList = true
                    return@exitForEach
                }
                if (it.viewHolderType == ViewHolderType.VT_EDIT_CHANNEL) {
                    isEditChannel = true
                    if ((it.data as ChannelItem).editChannel==EditChannel.CHANNEL_MOVE) {
                        isMoveChannel = true
                        return@exitForEach
                    }
                }
            }
        }

        recyclerView.windowAlignment = if(isParentList) BaseGridView.WINDOW_ALIGN_NO_EDGE else  BaseGridView.WINDOW_ALIGN_HIGH_EDGE
        val rect = Rect()
        parentHolder.itemView.getGlobalVisibleRect(rect)
        val lp = view!!.layoutParams as ConstraintLayout.LayoutParams

        if (isEditChannel) {
            lp.width = lp.width*2
        }
        if (isMoveChannel) {
            hintText?.visibility = View.INVISIBLE
            recyclerView.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
            lp.topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_32_5)
        } else {
            hintText?.visibility = View.GONE
            recyclerView.windowAlignmentOffset = rect.top - 2*container.height + Utils.getDimensInPixelSize(R.dimen.custom_dim_71) - (container.parent.parent as ViewGroup).translationY.toInt()
        }

        lp.leftMargin =rect.width() + rect.left +Utils.getDimensInPixelSize(R.dimen.custom_dim_40)
        view!!.layoutParams = lp

        adapter = PreferenceSubMenuAdapter(itemList,container,object :PreferenceSubMenuAdapter.Listener{
            override fun onBack(action : Action) :Boolean {
                if(action == Action.UP) return false
                parentHolder.itemView.requestFocus()
                return true
            }

            override fun updateInfo() {
                (parentHolder as PrefSubCategoryViewHolder).updateInfo()
            }

            override fun isAccessibilityEnabled(): Boolean {
                return false
            }

            /**
             * type 0 -> Hint text on item checked/clicked
             * type 1 -> Hint text on RC_OK long press after channel selection
             **/
            override fun updateHintText(status: Boolean, type: Int) {
                if (status) {
                    if (type == 0) {
                        hintText?.text = ConfigStringsManager.getStringById("long_press_to_move_channels")
                    } else {
                        hintText?.text = ConfigStringsManager.getStringById("press_up_down_to_place_channels")
                    }
                    hintText?.visibility = View.VISIBLE
                } else {
                    hintText?.text = ""
                    hintText?.visibility = View.INVISIBLE
                }
            }

            override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                return listener.getPreferenceDeepLink()
            }

            override fun onExit() {
                listener.onExit()
            }

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener.setSpeechText(text = text, importance = importance)
            }

            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                listener.showToast(text, duration)
            }

            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
            }
        })
        recyclerView.adapter = adapter
    }

    fun notifyPreferenceUpdated() {
        adapter!!.notifyPreferenceUpdated()
    }

    interface PrefContainerListener: TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
        fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink?
        // To exit directly from the scene
        fun onExit()
    }

}