package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import kotlin.math.log

/**
 * Reference radio button adapter
 *
 * @author Aleksandar Lazic
 */
class ReferenceRadioButtonAdapter : RecyclerView.Adapter<ReferenceRadioButtonListViewHolder>() {

    //Items
    private var items = mutableListOf<String>()

    //Focused item index
    private var focusedItem = -1

    var selectedItem = -1

    //Adapter listener
    var adapterListener: ReferenceRadioButtonListAdapterListener? = null

    //is recycler enabled
    private var isEnabled: Boolean = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReferenceRadioButtonListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.radio_button_list_item, parent, false)
        return ReferenceRadioButtonListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReferenceRadioButtonListViewHolder, position: Int) {
        val item = items[position]
        holder.listItem!!.setText(item)
        if (position == selectedItem) {
            val draw = ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_filled
            )

            if (position == focusedItem) {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
            } else {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            }

            holder.listItem!!.setDrawable(
                draw
            )
        } else {
            val draw = ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_empty
            )
            if (position == focusedItem) {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
            } else {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            }
            holder.listItem!!.setDrawable(
                draw
            )
        }

        holder.rootView?.setOnClickListener {
            val draw = ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_filled
            )

            draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))


            holder.listItem!!.setDrawable(
                draw
            )
            selectedItem = holder.adapterPosition
            focusedItem = holder.adapterPosition
            setSelected(holder)
            adapterListener!!.onItemClicked(selectedItem)

        }

        holder.rootView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }

                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                focusedItem = -1
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            } else {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            } else {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyDown(holder.adapterPosition)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (holder.adapterPosition==0)  focusedItem = -1

                        if (adapterListener != null) {
                            return adapterListener!!.onKeyUp(holder.adapterPosition)
                        }
                    }else if (keyCode == KeyEvent.KEYCODE_BACK){
                        downActionBackKeyDone = true
                    }
                } else if (keyEvent.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!downActionBackKeyDone)return true
                        downActionBackKeyDone = false
                        focusedItem = -1
                        return adapterListener!!.onBackPress(holder.adapterPosition)
                    }
                }
                return false
            }
        })

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                holder.listItem!!.onFocusChange(hasFocus)
                if (hasFocus) {
                    focusedItem = holder.adapterPosition
                    holder.listItem!!.getTextView().animate().scaleY(1.06f).scaleX(1.06f).duration =
                        0
                    holder.listItem!!.background = ConfigColorManager.generateButtonBackground()

                    if (selectedItem == holder.adapterPosition) {
                        val draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_filled
                        )
                        draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
                        holder.listItem!!.setDrawable(
                            draw
                        )

                    } else {
                        val draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_empty
                        )
                        draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
                        holder.listItem!!.setDrawable(
                            draw
                        )
                    }
                } else {

                    if (position == selectedItem) {
                        val draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_filled
                        )
                        draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                        holder.listItem!!.setDrawable(
                            draw
                        )
                        holder.listItem!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    } else {
                        val draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_empty
                        )
                        draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                        holder.listItem!!.setDrawable(
                            draw
                        )
                        holder.listItem!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    }

                    holder.listItem!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.listItem!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )

//                    holder.listItem!!.getTextView()
//                        .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                }
            }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun setSelected(holder: ReferenceRadioButtonListViewHolder) {
        val draw = ContextCompat.getDrawable(
            ReferenceApplication.applicationContext(),
            R.drawable.ic_small_radio_filled
        )
        draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
        holder.listItem!!.setDrawable(
            draw
        )
//        holder.listItem!!.getTextView()
//            .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        notifyItemRangeChanged(0, items.size)
    }

    fun setRecyclerEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        notifyItemRangeChanged(0, items.size)
    }

    //Refresh
    @SuppressLint("NotifyDataSetChanged")
    fun refresh(adapterItems: MutableList<String>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    /**
     * Listener
     */
    interface ReferenceRadioButtonListAdapterListener {
        fun onItemClicked(position: Int)
        fun getAdapterPosition(position: Int)
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight(currentPosition: Int): Boolean
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyDown(currentPosition: Int): Boolean
        fun onBackPress(currentPosition: Int): Boolean
    }
}

/**
 * ViewHolder of the ReferenceMultiCheckListAdapter
 */
class ReferenceRadioButtonListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //List item
    var listItem: ReferenceDrawableButton? = null

    init {

        //Set references
        rootView = view.findViewById(R.id.radio_button_list_item_view)
        listItem = view.findViewById(R.id.radio_button_list_item)

        //background
        listItem!!.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )

        //title
        listItem!!.getTextView().setTextColor(
            Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"))
        )
        listItem!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        val textSize =
            ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_13)
        listItem!!.getTextView().setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            textSize
        )

        //Set root view to be focusable and clickable
        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true
    }

    private fun setDimension(dimension: Int): Int {
        return ReferenceApplication.applicationContext().resources.getDimensionPixelSize(dimension)
    }
}