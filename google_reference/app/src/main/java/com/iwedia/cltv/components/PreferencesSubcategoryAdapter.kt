package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils


class PreferencesSubcategoryAdapter : RecyclerView.Adapter<PreferenceSubcategoryItemViewHolder>() {

    var isCategoryFocus= false
    val TAG = javaClass.simpleName
    //Items
    private var items = mutableListOf<PreferenceCategoryItem>()

    //Selected item
    var selectedItem = -1

    private var selectedItemViewHolder: PreferenceSubcategoryItemViewHolder? = null
    private var focusedItemViewHolder: PreferenceSubcategoryItemViewHolder? = null

    //Keep focus flag
    var keepFocus = false
    var shoudKeepFocusOnClick = false
    var focusPosition = -1
    var selectedItemEnabled = true

    //Adapter listener
    var adapterListener: PrefrenceSubcategoryAdapterListener? = null

    var clickAnimationInProgress = false

    //Holders hash map
    var holders: HashMap<Int, PreferenceSubcategoryItemViewHolder> = HashMap()
    private val radioButtonListAdapter = ReferenceRadioButtonAdapter()


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PreferenceSubcategoryItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.preference_subcategory_item, parent, false)

        return PreferenceSubcategoryItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreferenceSubcategoryItemViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val categoryItem = items[position]

        holder.categoryText1!!.text = categoryItem.name
        holder.categoryText2!!.visibility = View.VISIBLE
        if(categoryItem.info == null){
            if (categoryItem.switchStatus==true){
                holder.categoryText2!!.text =  ConfigStringsManager.getStringById("on")
            }else if (categoryItem.switchStatus == false){
                holder.categoryText2!!.text = ConfigStringsManager.getStringById("off")
            }else{
                holder.categoryText2!!.visibility = View.GONE
            }
        }else{
            when (categoryItem.switchStatus)  {
                true -> {
                    holder.categoryText2!!.text = categoryItem.info
                }
                false -> {
                    holder.categoryText2!!.text = ConfigStringsManager.getStringById("off")
                }else ->{
                    holder.categoryText2!!.text= categoryItem.info
                }
            }
        }

        if(categoryItem.showArrow){
            holder.arrowRight!!.visibility = View.VISIBLE
        }else{
            holder.arrowRight!!.visibility = View.GONE
        }

        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
        holder.categoryText2!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )

        setBackground(holder)
        holders[position] = holder
        if (position == selectedItem) {
            selectedItemViewHolder = holder
            setActiveFilter(holder)

        }

        if (categoryItem.switchStatus==true) {
            holder.subOptionSwitchText!!.text = ConfigStringsManager.getStringById("on")
        } else if(categoryItem.switchStatus == false){
            holder.subOptionSwitchText!!.text = ConfigStringsManager.getStringById("off")
        }

        holder.subOptionSwitchButton!!.setOnCheckedChangeListener { _, isChecked ->
            if (items[holder.adapterPosition].info!=null){
                        var info = ""
                        if (isChecked && categoryItem.radioOptionSelected==null){
                           info = ConfigStringsManager.getStringById("on")

                        }else if(isChecked && categoryItem.radioOptionSelected!=null){
                            info = categoryItem.info!!
                        }else {
                          info =  ConfigStringsManager.getStringById("off")
                        }

                holders[position]!!.categoryText2?.text = info


            }else{
                holders[position]!!.categoryText2!!.text = if (isChecked ) {
                    ConfigStringsManager.getStringById("on")
                } else {
                    ConfigStringsManager.getStringById("off")
                }
            }
            if (isChecked) {
                holders[holder.adapterPosition]!!.subOptionSwitchText?.text = ConfigStringsManager.getStringById("on")
                if (items[holder.adapterPosition].subOptionsRadioBtnOptionsList!=null){
                    showRadioOptions(holder,categoryItem)
                }
                if (adapterListener!=null) adapterListener!!.onSwitchClicked(true,categoryItem.id)
            } else {
                holders[holder.adapterPosition]!!.subOptionRadioButtonGridView?.visibility = View.GONE
                holders[holder.adapterPosition]!!.subOptionSwitchText!!.text = ConfigStringsManager.getStringById("off")
                if (adapterListener!=null)adapterListener!!.onSwitchClicked(false,items[selectedItem].id)
            }
                items[holder.adapterPosition].switchStatus = isChecked

        }

        holder.subOptionSwitchButton!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (p2!!.action == KeyEvent.ACTION_DOWN) {
                    if (p1 == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (items[selectedItem].subOptionsRadioBtnOptionsList!=null){
                            holder.subOptionRadioButtonGridView!!.requestFocus()
                        }
                        return true
                    }

                    if (p1 == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true
                    }

                    if (p1 == KeyEvent.KEYCODE_DPAD_UP) {
                        return true
                    }
                    if (p1 == KeyEvent.KEYCODE_DPAD_LEFT) {
                        holder.rootView!!.requestFocus()
                        return true

                    }
                }

                if (p2.action == KeyEvent.ACTION_UP) {
                    if (p1 == KeyEvent.KEYCODE_BACK) {
                    holder.rootView!!.requestFocus()
                        return true
                    }
                }

                return false
            }
        })

        holder.subOptionSwitchButton!!.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                holder.llSwitchSubOptions!!.setBackgroundResource(R.drawable.focus_shape)
                holder.llSwitchSubOptions!!.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))

                holder.subOptionSwitchButton!!.thumbDrawable.setColorFilter(
                    Color.parseColor(ConfigColorManager.getColor("color_background")),
                    PorterDuff.Mode.SRC_IN
                )
                holder.subOptionSwitchText!!.typeface = TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_medium")
                )
                holder.subOptionSwitchButton!!.trackDrawable.setColorFilter(
                    Color.parseColor(ConfigColorManager.getColor("color_background")),
                    PorterDuff.Mode.SRC_IN
                )
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_background"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                    holder.subOptionSwitchText!!.setTextColor(
                        color_context
                    )
                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                }

            } else {
                holder.llSwitchSubOptions!!.setBackgroundResource(R.drawable.transparent_shape)

                holder.subOptionSwitchButton!!.thumbDrawable.setColorFilter(
                    Color.parseColor(ConfigColorManager.getColor("color_main_text")),
                    PorterDuff.Mode.SRC_IN
                )
                holder.subOptionSwitchText!!.typeface = TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_medium")
                )
                holder.subOptionSwitchButton!!.trackDrawable.setColorFilter(
                    Color.parseColor(ConfigColorManager.getColor("color_main_text")),
                    PorterDuff.Mode.SRC_IN
                )
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                    holder.subOptionSwitchText!!.setTextColor(
                        color_context
                    )
                } catch (ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                }

            }
        }


        holder.subOptionRadioButtonGridView!!.setNumColumns(1)
        holder.subOptionRadioButtonGridView!!.adapter = radioButtonListAdapter

        holder.subOptionRadioButtonGridView!!.preserveFocusAfterLayout = true
        radioButtonListAdapter.adapterListener =
            object : ReferenceRadioButtonAdapter.ReferenceRadioButtonListAdapterListener {
                override fun onItemClicked(position: Int) {
                    if (adapterListener!=null) {

                        adapterListener!!.radioButtonClicked(position, items[selectedItem].id)

                        items[selectedItem].radioOptionSelected = position
                        items[selectedItem].info = items[selectedItem].subOptionsRadioBtnOptionsList!![position]
                        holders[selectedItem]!!.categoryText2!!.visibility = View.VISIBLE
                        holders[selectedItem]!!.categoryText2!!.text= items[selectedItem].subOptionsRadioBtnOptionsList!![position]
                        radioButtonListAdapter.selectedItem =  position
                        radioButtonListAdapter.notifyDataSetChanged()
                    }

                }

                override fun getAdapterPosition(position: Int) {

                }

                override fun onKeyLeft(currentPosition: Int): Boolean {
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    return true
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    if (currentPosition == 0) {
                        if (items[selectedItem].switchStatus!=null){
                            holders[selectedItem]!!.subOptionSwitchButton!!.requestFocus()
                        }
                        return true
                    }
                    return false
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    return false
                }

                override fun onBackPress(currentPosition: Int): Boolean {
                    holders[selectedItem]!!.rootView!!.requestFocus()
                    return true
                }
            }

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                holder.categoryText1!!.setEllipsize(if(hasFocus)TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END);
                holder.categoryText2!!.setEllipsize(if(hasFocus)TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END);

                if(hasFocus) holder.categoryText1!!.setSingleLine(true);
                if(hasFocus) holder.categoryText2!!.setSingleLine(true);

                if(hasFocus) holder.categoryText1!!.setSelected(true);
                if(hasFocus) holder.categoryText2!!.setSelected(true);

                if (hasFocus) {
                    holder.mainLLSubOptions!!.visibility = View.VISIBLE
                    if (categoryItem.switchStatus!=null) {
                        showSwitch(holders[holder.adapterPosition]!!)
                        holder.llSwitchSubOptions?.visibility = View.VISIBLE
                        holder.subOptionSwitchButton?.isChecked = categoryItem.switchStatus!!
                    } else
                        holder.llSwitchSubOptions!!.visibility = View.GONE

                    if (categoryItem.switchStatus!=false && categoryItem.subOptionsRadioBtnOptionsList!=null && categoryItem.subOptionsRadioBtnOptionsList!!.size!=0 ){
                        showRadioOptions(holder,categoryItem)

                    }else{
                        holder.subOptionRadioButtonGridView!!.visibility = View.GONE
                    }
                    adapterListener?.getAdapterPosition(position)
                    focusPosition = holder.adapterPosition
                    setFocused(holder)

                    holder.categoryText1!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()
                    holder.categoryText2!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()
                    holder.arrowRight!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()


                }else{
                    Utils.unFocusAnimation(holder.rootView!!)
                    if(isCategoryFocus) {
                        if (selectedItem != holder.adapterPosition) {
                            holder.mainLLSubOptions!!.visibility = View.GONE
                            holder.llSwitchSubOptions!!.visibility = View.GONE
                            holder.subOptionRadioButtonGridView!!.visibility = View.GONE
                            setBackground(holder)
                            holder.categoryText1!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                            holder.categoryText2!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                            holder.arrowRight!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()

                        } else {
                            setActiveFilter(holder)
                        }
                    }
                    else{
                        if (focusPosition != selectedItem) {
                            setBackground(holder)
                            holder.categoryText1!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                            holder.categoryText2!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                            holder.arrowRight!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                        }
                    }

                }

            }

        holder.rootView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!! || clickAnimationInProgress) {
                    return true
                }
                keepFocus = false
                if (p2!!.action == KeyEvent.ACTION_DOWN) {
                    if (p1 == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            } else {
                                handleRightPressed(categoryItem,holder)

                                adapterListener!!.onKeyRight(items[selectedItem].id,position)
                            }
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        selectedItem = holder.adapterPosition
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                handleRightPressed(categoryItem,holder)
                                adapterListener!!.onKeyRight(items[selectedItem].id,position)
                            } else {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            }
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (holder.adapterPosition==  (items.size-1))return true
                            if (adapterListener != null) {
                                selectedItem=-1
                                return adapterListener!!.onKeyDown(holder.adapterPosition)

                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_UP) {
                            if (adapterListener != null) {
                                selectedItem=-1
                                return adapterListener!!.onKeyUp(holder.adapterPosition)
                            }
                    }
                }

                if (p2.action == KeyEvent.ACTION_UP) {
                    if (p1 == KeyEvent.KEYCODE_BACK) {
                        selectedItem = -1
                        return adapterListener!!.onBackPressed(holder.adapterPosition)
                    }
                }
                return false
            }
        })
        holder.rootView?.setOnClickListener {
            if (adapterListener != null) {
                clickAnimationInProgress = true

                Utils.viewClickAnimation(holder.rootView!!, object : AnimationListener{
                    override fun onAnimationEnd() {
                        clickAnimationInProgress = false

                        selectedItem = holder.adapterPosition
                        try {
                            adapterListener?.onItemClicked(holder.adapterPosition,items[holder.adapterPosition].id)
                        }catch (E: Exception){
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAnimationEnd: ${E.printStackTrace()}")
                        }
                    }
                })
            }
        }
        holder.rootView!!.alpha = if(items[holder.adapterPosition].isDisabled) 0.7f else 1f
    }

    private fun showRadioOptions(holder: PreferenceSubcategoryItemViewHolder,categoryItem:PreferenceCategoryItem) {
        holder.subOptionRadioButtonGridView!!.visibility = View.VISIBLE
        if (items[holder.adapterPosition].radioOptionSelected!=null) {
            holder.subOptionRadioButtonGridView!!.scrollToPosition(items[holder.adapterPosition].radioOptionSelected!!)
            radioButtonListAdapter.selectedItem =
                items[holder.adapterPosition].radioOptionSelected!!
        }else{
            radioButtonListAdapter.selectedItem = -1
        }


       if (categoryItem.subOptionsRadioBtnOptionsList!=null) {
           holders[holder.adapterPosition]!!.categoryText2!!.text = items[holder.adapterPosition].info
           radioButtonListAdapter.refresh(categoryItem.subOptionsRadioBtnOptionsList!!)
       }
    }

    private fun showSwitch(holder: PreferenceSubcategoryItemViewHolder) {
        holder.llSwitchSubOptions!!.setBackgroundResource(R.drawable.transparent_shape)

        holder.subOptionSwitchButton!!.thumbDrawable.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_main_text")),
            PorterDuff.Mode.SRC_IN
        )
        holder.subOptionSwitchText!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        holder.subOptionSwitchButton!!.trackDrawable.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_main_text")),
            PorterDuff.Mode.SRC_IN
        )
        try {
            val color_context =
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "showSwitch: Exception color_context $color_context")
            holder.subOptionSwitchText!!.setTextColor(
                color_context
            )
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "showSwitch: Exception color rdb $ex")
        }


    }


    fun handleRightPressed(categoryItem: PreferenceCategoryItem,holder: PreferenceSubcategoryItemViewHolder){

        holder.mainLLSubOptions!!.visibility = View.VISIBLE

        if (categoryItem.switchStatus!=null) {
            holder.llSwitchSubOptions!!.setBackgroundResource(R.drawable.focus_shape)
            holder.subOptionSwitchButton!!.requestFocus()
        }
        else {
            if (categoryItem.subOptionsRadioBtnOptionsList!=null){
                if (categoryItem.radioOptionSelected!=null){
                    holder.subOptionRadioButtonGridView!!.scrollToPosition(categoryItem.radioOptionSelected!!)
                    holder.subOptionRadioButtonGridView!!.layoutManager!!.getChildAt(categoryItem.radioOptionSelected!!)?.requestFocus()
                }
            }
        }
    }

    fun setActiveFilter(holder: PreferenceSubcategoryItemViewHolder) {
        selectedItem = holder.adapterPosition
        selectedItemViewHolder = holder
        setSelected(holder)
    }
    fun setSelected(holder: PreferenceSubcategoryItemViewHolder?) {
        if (holder!!.adapterPosition == -1 || !selectedItemEnabled) {
            return
        }
        holder!!.rootView!!.background =
            ContextCompat.getDrawable(
                getContext(),
                R.drawable.preference_subcategory_selected
            )

        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
        holder.categoryText2!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )

        holder.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.categoryText2!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder!!.arrowRight!!.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
        )
        val gdseB = GradientDrawable()
        gdseB.setShape(GradientDrawable.RECTANGLE)
        gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_main_text")).replace("#",ConfigColorManager.alfa_light)))//todo
        gdseB.cornerRadius = 17f
        holder!!.rootView!!.background = gdseB

    }


    fun setFocused(holder: PreferenceSubcategoryItemViewHolder?) {
        val gdseB = GradientDrawable()
        gdseB.setShape(GradientDrawable.RECTANGLE)
        gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_selector"))))//todo
        gdseB.cornerRadius = 17f
        holder!!.rootView!!.background = gdseB
        Utils.focusAnimation(holder.rootView!!)


        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
        holder.categoryText2!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color_context $color_context")
            holder.categoryText1!!.setTextColor(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color rdb $ex")
        }

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color_context $color_context")
            holder.categoryText2!!.setTextColor(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color rdb $ex")
        }

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color_context $color_context")
            holder.arrowRight!!.setColorFilter(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color rdb $ex")
        }
    }


    fun setChecked(holder: PreferenceSubcategoryItemViewHolder?) {
        val gdseB = GradientDrawable()
        gdseB.setShape(GradientDrawable.RECTANGLE)
        gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_selector").replace("#",ConfigColorManager.alfa_75))))
        gdseB.cornerRadius = 17f
        holder!!.rootView!!.background = gdseB


        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color_context $color_context")
            holder.categoryText1!!.setTextColor(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color rdb $ex")
        }
    }

    private fun setBackground(holder: PreferenceSubcategoryItemViewHolder) {
//        holder.rootView!!.background =
//            ContextCompat.getDrawable(
//                ReferenceApplication.applicationContext(),
//                R.drawable.preference_subcategory_background
//            )

        holder.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.categoryText2!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.arrowRight!!.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
        )

        val drawableChannelLogo = GradientDrawable()
        drawableChannelLogo.setShape(GradientDrawable.RECTANGLE)
        drawableChannelLogo.setCornerRadius(17F)
        val colorStartLight = Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))
        drawableChannelLogo.setColors(
            intArrayOf(
                colorStartLight,
                colorStartLight,
                colorStartLight
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.rootView!!.setBackground(drawableChannelLogo)
        } else{
            holder.rootView!!.setBackgroundDrawable(drawableChannelLogo)
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<PreferenceCategoryItem>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    private fun setDimension(dimension: Int): Int {
        return getContext().resources.getDimensionPixelSize(dimension)
    }

    private fun getContext(): Context {
        return ReferenceApplication.applicationContext()
    }


    /**
     * Set selected item by position
     *
     * @param position item position inside the list
     */
    fun setSelected(position: Int) {
        if (holders.containsKey(position)) {
            selectedItem = position
//            holders[position]!!.rootView!!.background =
//                ContextCompat.getDrawable(
//                    getContext(),
//                    R.drawable.preference_subcategory_background
//                )
            holders[position]!!.categoryText1!!.typeface =
                TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_medium")
                )
            holders[position]!!.categoryText2!!.typeface =
                TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_light")
                )

            holders[position]!!.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holders[position]!!.categoryText2!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holders[position]!!.arrowRight!!.setColorFilter(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )

            val drawableChannelLogo1 = GradientDrawable()
            drawableChannelLogo1.setShape(GradientDrawable.RECTANGLE)
            drawableChannelLogo1.setCornerRadius(17F)
            val colorStartLight = Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))
            drawableChannelLogo1.setColors(
                intArrayOf(
                    colorStartLight,
                    colorStartLight,
                    colorStartLight
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holders[position]!!.rootView!!.setBackground(drawableChannelLogo1)
            } else{
                holders[position]!!.rootView!!.setBackgroundDrawable(drawableChannelLogo1)
            }
        }
    }

    /**
     * sets the background color of the item
     */
    fun setBackground(position: Int) {
        if (holders.containsKey(position)) {
//            holders[position]!!.rootView!!.background =
//                ContextCompat.getDrawable(
//
//                    ReferenceApplication.applicationContext(),
//                    R.drawable.preference_subcategory_background,
//
//                    )

            holders[position]!!.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holders[position]!!.categoryText2!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holders[position]!!.arrowRight!!.setColorFilter(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )

            val drawableChannelLogo3 = GradientDrawable()
            drawableChannelLogo3.setShape(GradientDrawable.RECTANGLE)
            drawableChannelLogo3.setCornerRadius(17F)
            val colorStartLight = Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))
            drawableChannelLogo3.setColors(
                intArrayOf(
                    colorStartLight,
                    colorStartLight,
                    colorStartLight
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holders[position]!!.rootView!!.setBackground(drawableChannelLogo3)
            } else{
                holders[position]!!.rootView!!.setBackgroundDrawable(drawableChannelLogo3)
            }
        }
    }

    /**
     * Update adapter items list
     */
    fun update(list: MutableList<PreferenceCategoryItem>) {
        if ((!items.containsAll(list)) ||
            (list.isNotEmpty() && items.containsAll(list) && items.size != list.size)
        ) {
            var difference =  list.size - items.size

            if(difference!=0){
                //new added
                if(difference>0){
                    notifyItemRangeInserted(items.size,Math.abs(difference))
                }
                //removed
                if(difference<0){
                    notifyItemRangeRemoved(list.size,Math.abs(difference))
                }
            }

            if(list.size!=0) {
                //changed
                notifyItemRangeChanged(0, list.size)
            }

            items.clear()
            items.addAll(list)
        }
    }

    fun requestFocus(position: Int) {
        if (holders.containsKey(position)) {
            if (focusedItemViewHolder != null) {
                setBackground(focusedItemViewHolder!!)
            }
            focusedItemViewHolder = holders[position]
            setFocused(focusedItemViewHolder)
        }
    }

    fun clearFocus(position: Int) {
        if (holders.containsKey(position)) {
            focusedItemViewHolder = holders[position]
            val gdseB = GradientDrawable()
            gdseB.setShape(GradientDrawable.RECTANGLE)
            gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))))//todo
            gdseB.cornerRadius = 17f
            focusedItemViewHolder!!.rootView!!.background = gdseB
            Utils.focusAnimation(focusedItemViewHolder!!.rootView!!)

            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "clearFocus: Exception color_context $color_context")
                focusedItemViewHolder!!.categoryText1!!.setTextColor(
                    color_context
                )
            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "clearFocus: Exception color rdb $ex")
            }
        }
    }

    interface PrefrenceSubcategoryAdapterListener {
        fun getAdapterPosition(position: Int)
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight(itemId: String,position: Int): Boolean
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyDown(curradrentPosition: Int): Boolean
        fun onItemClicked(position: Int,itemId: String)
        fun onBackPressed(position: Int):Boolean
        fun onSwitchClicked(switchValue: Boolean, id:String)
        fun radioButtonClicked(clickedPosition:Int, radioButtonId: String)
    }
}