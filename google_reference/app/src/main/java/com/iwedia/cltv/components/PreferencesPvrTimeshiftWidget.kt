package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.PreferencesPvrTimeshiftInformation
import com.iwedia.cltv.entities.ReferenceDeviceItem
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.SceneData
import world.SceneManager
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Preferences setup widget
 *
 * @author Gaurav Jain
 */
class PreferencesPvrTimeshiftWidget
    (context: Context, listener: PreferencesPvrTimeshiftWidgetListener) :
    GWidget<ConstraintLayout, PreferencesPvrTimeshiftWidget.PreferencesPvrTimeshiftWidgetListener>(
        ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT,
        ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT,
        listener
    ) {

    private var noItem: TextView? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()
    private var deviceInfoListView: VerticalGridView? = null
    private var timeShiftRightContainer: RelativeLayout? = null
    private var deviceItemList = mutableListOf<ReferenceDeviceItem>()
    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var context: Context? = context
    var adapter: PreferenceSubMenuAdapter? = null
    var list = mutableListOf<PrefItem<Any>>()
    var pvrTimeshiftListener: PreferencesPvrTimeshiftWidgetListener? = listener


    init {
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_preferences_pvr_timeshift, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_left_options)
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset =
            Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        timeShiftRightContainer = view!!.findViewById(R.id.time_shift_right_container)
        noItem = view!!.findViewById(R.id.no_item)
        noItem!!.text = ConfigStringsManager.getStringById("no_device")
        noItem!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        noItem!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        setUpAdapter()
    }

    //To handle visibility of right side menu based on left menu selection
    fun handleVisibility(hasFocus: Boolean) {
        if (hasFocus) {
            deviceInfoListView!!.visibility = VISIBLE
            noItem!!.visibility = if (list.isEmpty()) VISIBLE else GONE
        } else {
            deviceInfoListView!!.visibility = GONE
            noItem!!.visibility = GONE
        }
    }

    override fun refresh(data: Any) {
        if (data is PreferencesPvrTimeshiftInformation) {
            val listener = object : PrefItemListener {
                override fun onAction(action: Action, id: Pref): Boolean {
                    if (action == Action.FOCUSED) {
                        listener.requestDeviceList()
                        handleVisibility(id == Pref.DEVICE_INFO)
                    }
                    if (action == Action.RIGHT && id == Pref.DEVICE_INFO) {
                        deviceInfoListView!!.requestFocus()
                    }
                    return false
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    listener.showToast(text, duration)
                }
            }

            val prefCompoundListener = object : PrefCompoundListener {
                override fun onChange(
                    isSelected: Boolean,
                    index: Int,
                    id: Pref,
                    callback: IAsyncDataCallback<Boolean>
                ) {

                    callback.onReceive(isSelected)

                    if (id == Pref.TIMESHIFT_MODE_SWITCH) {
                        pvrTimeshiftListener!!.onTimeshiftStatusChanged(isSelected)
                    }
                }
            }

            initSubCategories(data, prefCompoundListener, listener)
            updateDeviceList(data.usbDeviceList)
        }
    }

    private fun initSubCategories(
        data: PreferencesPvrTimeshiftInformation,
        prefCompoundListener: PrefCompoundListener,
        listener: PrefItemListener
    ) {
        val zerothColumn = mutableListOf<PrefItem<Any>>()
        if (data.subCategories.isNotEmpty()) {
            categories.clear()
            data.subCategories.forEach { item ->
                val itemTitle = item.name

                if (item.id == PrefSubMenu.TIMESHIFT_MODE) {
                    addTimeshiftModePrefItem(
                        data,
                        prefCompoundListener,
                        zerothColumn,
                        itemTitle,
                        listener
                    )
                } else if (item.id == PrefSubMenu.DEVICE_INFO) {
                    addDeviceInfoPrefItem(zerothColumn, itemTitle, listener)
                }

                setPreferenceLeftOptionsAdapter(zerothColumn)
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }
    }

    private fun addDeviceInfoPrefItem(
        zerothColumn: MutableList<PrefItem<Any>>,
        itemTitle: String,
        listener: PrefItemListener
    ) {
        zerothColumn.add(
            PrefItem(
                ViewHolderType.VT_MENU,
                Pref.DEVICE_INFO,
                RootItem(
                    itemTitle, null, true
                ),
                listener
            )
        )
    }

    private fun addTimeshiftModePrefItem(
        data: PreferencesPvrTimeshiftInformation,
        prefCompoundListener: PrefCompoundListener,
        zerothColumn: MutableList<PrefItem<Any>>,
        itemTitle: String,
        listener: PrefItemListener
    ) {
        val switchStatus = data.timeshiftModeEnabled!!
        val status =
            ConfigStringsManager.getStringById(if (switchStatus) "on" else "off")

        val compoundItem =
            CompoundItem(status, switchStatus, 0, prefCompoundListener)

        zerothColumn.add(
            PrefItem(ViewHolderType.VT_MENU,
                Pref.TIMESHIFT_MODE,
                RootItem(
                    itemTitle, status, true,
                    mutableListOf(
                        PrefItem(
                            ViewHolderType.VT_SWITCH,
                            Pref.TIMESHIFT_MODE_SWITCH,
                            compoundItem,
                            listener
                        )
                    ), object : InfoListener {
                        override fun getInfo(): String {
                            return ConfigStringsManager.getStringById(if (compoundItem.isChecked) "on" else "off")
                        }
                    }
                ),
                listener
            )

        )
    }

    private fun setPreferenceLeftOptionsAdapter(zerothColumn: MutableList<PrefItem<Any>>) {
        preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
            zerothColumn,
            view!!,
            object : PreferenceSubMenuAdapter.Listener {
                override fun onBack(action: Action): Boolean {

                    if (action == Action.LEFT) return false
                    pvrTimeshiftListener?.onPrefsCategoriesRequestFocus()
                    return true
                }

                override fun updateInfo() {
                    //ignore
                }

                override fun isAccessibilityEnabled(): Boolean {
                    return false
                }

                override fun updateHintText(status: Boolean, type: Int) {
                }

                override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                    return listener?.getPreferenceDeepLink()
                }

                override fun onExit() {
                    listener?.onExit()

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
            }
        )
    }


    private fun updateDeviceList(deviceListTemp: MutableList<ReferenceDeviceItem>) {
        //to shift focus when device list become empty
        if (deviceListTemp.size == 0 && deviceInfoListView != null && deviceInfoListView!!.hasFocus()) {
            setFocusToGrid()
        }

        list.clear()
        deviceItemList.clear()
        deviceItemList.addAll(deviceListTemp)
        deviceItemList.forEachIndexed { index, it ->

            val info = StringBuilder()

            info.append(String.format("%.1f/%.1f GB", it.availableSize, it.totalSize))
            if (it.isPvr!! || it.isTimeshift!!) {
                info.append("   ")
                if (it.isTimeshift!!) info.append(ConfigStringsManager.getStringById("tshift"))
                if (it.isTimeshift!! && it.isPvr!!) info.append("/")
                if (it.isPvr!!) info.append(ConfigStringsManager.getStringById("pvr"))
            }
            info.append("   ")
            info.append(
                if (it.speed != null) String.format("%3.1f MB/S", it.speed!!)
                else ConfigStringsManager.getStringById("na")
            )

            list.add(
                PrefItem(ViewHolderType.VT_MENU,
                    Pref.DEVICE_INFO_ITEM,
                    RootItem(
                        if (it.label == null) ConfigStringsManager.getStringById("no_name") else it.label + " ",
                        info.toString(), false
                    ),
                    object : PrefItemListener {
                        override fun onAction(action: Action, id: Pref): Boolean {
                            if (action == Action.CLICK) {

                                val currentActiveScene = ReferenceApplication.worldHandler!!.active
                                val sceneData = SceneData(
                                    currentActiveScene!!.id,
                                    currentActiveScene!!.instanceId,
                                    it
                                )
                                ReferenceApplication.worldHandler!!.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.DEVICE_OPTION_SCENE,
                                    SceneManager.Action.SHOW_OVERLAY, sceneData
                                )

                            }
                            if (action == Action.FOCUSED && id == Pref.DEVICE_INFO) {
                                deviceInfoListView!!.requestFocus()
                            }
                            return false
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            listener.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            listener.showToast(text, duration)
                        }
                    }
                )
            )
        }

        adapter = PreferenceSubMenuAdapter(
            list,
            view!!,
            object : PreferenceSubMenuAdapter.Listener {
                override fun onBack(action: Action): Boolean {
                    if (action == Action.UP) return false
                    setFocusToGrid()
                    return true
                }

                override fun updateInfo() {
                    //ignore
                }

                override fun isAccessibilityEnabled(): Boolean {
                    return false
                }

                override fun updateHintText(status: Boolean, type: Int) {
                }

                override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                    return  listener?.getPreferenceDeepLink()

                }

                override fun onExit() {
                    TODO("Not yet implemented")
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
            }
        )
        deviceInfoListView!!.adapter = adapter
    }

    private fun setUpAdapter() {
        deviceInfoListView = VerticalGridView(context)
        val params = ConstraintLayout.LayoutParams(
            Utils.getDimensInPixelSize(R.dimen.custom_dim_400),
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 0)
        deviceInfoListView!!.layoutParams = params

        // Setup left options list
        deviceInfoListView!!.setNumColumns(1)

        deviceInfoListView!!.preserveFocusAfterLayout = true

        deviceInfoListView!!.overScrollMode = OVER_SCROLL_NEVER
        deviceInfoListView!!.clipToPadding = false

        timeShiftRightContainer!!.addView(deviceInfoListView)
    }


    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferencesPvrTimeshiftWidgetListener : GWidgetListener,
        TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun requestDeviceList()
        fun onTimeshiftStatusChanged(enabled: Boolean)

        fun onExit()

        fun getPreferenceDeepLink() : ReferenceWidgetPreferences.DeepLink?
    }
}