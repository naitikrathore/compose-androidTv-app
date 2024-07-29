package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import world.SceneData
import world.SceneManager
import world.widget.GWidget
import world.widget.GWidgetListener

class MmiMenuWidget :
    GWidget<ConstraintLayout, MmiMenuWidget.MmiMenuWidgetListener> {

    private var title: TextView? = null
    private var subTitle: TextView? = null
    private var bottom: TextView? = null

    private val leftOptionsList = mutableListOf<PreferenceCategoryItem>()
    private val leftOptionsAdapter = MmiMenuAdapter()
    private var leftOptionsGridView: VerticalGridView? = null

    interface MmiMenuWidgetListener : GWidgetListener {
        fun onSelectMenuItem(position: Int)
        fun onCancelCurrentMenu()
    }

    @SuppressLint("SetTextI18n")
    constructor(context: Context, listener: MmiMenuWidget.MmiMenuWidgetListener) : super(
        ReferenceWorldHandler.WidgetId.MMI_MENU,
        ReferenceWorldHandler.WidgetId.MMI_MENU,
        listener) {

        findRefs(context)
    }

    @SuppressLint("SetTextI18n")
    private fun findRefs(context: Context) {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_mmi_menu_widget, null) as ConstraintLayout
        println("MMi MmiMenuWidget")

        leftOptionsGridView = view!!.findViewById(R.id.left_options_list)

        val channel_list_bg: ConstraintLayout = view!!.findViewById(R.id.mmi_menu_bg)
        val bg = ConfigColorManager.getColor("color_background").replace("#","#CC")
        channel_list_bg.setBackgroundColor(Color.parseColor(bg))

        title = view!!.findViewById(R.id.title)
        title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        title!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        subTitle = view!!.findViewById(R.id.subTitle)
        subTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        subTitle!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        bottom = view!!.findViewById(R.id.bottom)
        bottom!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        bottom!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        // Setup left options list
        leftOptionsGridView!!.setNumColumns(1)
        leftOptionsAdapter.isCategoryFocus = true
        leftOptionsGridView!!.adapter = leftOptionsAdapter
        leftOptionsAdapter.selectedItem = -1

        leftOptionsAdapter.adapterListener =
            object : MmiMenuAdapter.PrefrenceSubcategoryAdapterListener {

                override fun getAdapterPosition(position: Int) {
                    return
                }


                override fun onKeyLeft(currentPosition: Int): Boolean {
                    leftOptionsGridView!!.requestFocus()
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    return false

                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    if (currentPosition == 0) {
                        return true
                    }
                    return false
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    return false
                }

                override fun onItemClicked(position: Int) {
                    println("MMi2 onItemClicked $position")
                    listener.onSelectMenuItem(position)
                    return
                }

                override fun onBackPressed(position: Int): Boolean {
                    println("MMi2 onBackPressed")
                    try {
                        listener.onCancelCurrentMenu()
                    }catch (E: Exception){
                        println("MMi EXCEPTION")
                        println(E.printStackTrace())
                    }
                    return true
                }
            }

    }

    override fun refresh(data: Any) {
        super.refresh(data)

        println("MMi MmiMenuWidget refresh")
        if (data is CiMenu) {
            println("MMi MmiMenuWidget refresh data is GList")
            leftOptionsList.clear()
            println("MMi MmiMenuWidget refresh data.value.size ${data.tempList.size}")
            if (data.tempList.isNotEmpty()) {
                data.tempList.forEach { item ->
                    leftOptionsList.add(item)
                }

                leftOptionsAdapter.refresh(leftOptionsList)
            }

            if(data.menu != null) {
                println(
                    "MMiW MmiMenuWidget refresh ReferenceSdk.ciPlusHandler!!.mmiTitleText ${data.menu!!.title} " +
                            "ReferenceSdk.ciPlusHandler!!.mmiSubTitleText ${data.menu!!.subTitle} " +
                            "ReferenceSdk.ciPlusHandler!!.mmiBottomText ${data.menu!!.bottom}"
                )

                if ((title?.text == "") && data.menu!!.title != "") {
                    title?.text = data.menu!!.title
                }
                if ((subTitle?.text == "") && data.menu!!.subTitle != "") {
                    subTitle?.text = data.menu!!.subTitle
                }
                if ((bottom?.text == "") && data.menu!!.bottom != "") {
                    bottom?.text = data.menu!!.bottom
                }
            }
            try {
                println("MMiW4 leftOptionsAdapter ${leftOptionsAdapter.selectedItem}")
                leftOptionsAdapter.setSelected(0)
                leftOptionsGridView?.post {
                    leftOptionsGridView?.layoutManager?.findViewByPosition(0)
                        ?.requestFocus()
                }
            } catch (E: Exception) {
                println("MMiW2 Exception " + E.printStackTrace())
            }


        }
    }


    fun setFocusToGrid() {
        leftOptionsGridView!!.requestFocus()
    }

    fun requestFocus() {
        if (leftOptionsGridView!!.getChildAt(leftOptionsAdapter.selectedItem) != null) {
            leftOptionsAdapter.requestFocus(leftOptionsAdapter.selectedItem)
            leftOptionsGridView!!.getChildAt(leftOptionsAdapter.selectedItem).requestFocus()
        }
    }

    fun requestFocus(position: Int) {
        println("MMi2 requestFocus")
        if (leftOptionsGridView!!.getChildAt(position) != null) {
            leftOptionsAdapter.requestFocus(position)
            leftOptionsGridView!!.getChildAt(position).requestFocus()
        }
    }
}