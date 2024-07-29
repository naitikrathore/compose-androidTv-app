package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.InputInformation
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.parental.InputSourceData

class InputSourceAdapter(
    var context: Context, val listener: InputSourceListener
) : RecyclerView.Adapter<InputItemViewHolder>() {

    //Items
    private var inputList = ArrayList<InputItem>()
    private var availableinputList = ArrayList<Int>()
    private var inputImgs = ArrayList<Int>()
    private var inputFocusImgs = ArrayList<Int>()
    var holders: HashMap<Int, InputItemViewHolder> = HashMap()
    private var focusedItemViewHolder: InputItemViewHolder? = null
    private var blockedInputList = ArrayList<InputSourceData>()
    private var isFactoryMode: Boolean = false
    private var isParentalEnabled: Boolean = false


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InputItemViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.input_item, parent, false)

        return InputItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: InputItemViewHolder, position: Int) {
        val item = inputList[position]
        holders[position] = holder
        if(item.inputSourceName == "TV") {
            if (isFactoryMode) {
                holder.categoryText?.text = item.inputSourceName
            } else {
                holder.categoryText?.text = ConfigStringsManager.getStringById("input_tv")
            }
        } else if(item.inputSourceName == "Home") {
            holder.categoryText?.text = ConfigStringsManager.getStringById("input_home")
        } else if(item.inputSourceName == "Google TV Home") {
            holder.categoryText?.text = ConfigStringsManager.getStringById("google_tv_home")
        } else {
            holder.categoryText?.text = item.inputSourceName
            if (!isFactoryMode) {
                if (blockedInputList[position].isBlocked) {
                    holder.categoryText?.maxWidth = 280
                } else {
                    holder.categoryText?.maxWidth = 500
                }
                holder.categoryText?.setSingleLine()
            }
        }
        getConnectedInputs(position, holder)

        getIsLocked(holder,position)
        if (position == (inputList.size - 1)) {
            listener.adapterSet()
        }
        holder.inputLayout?.setOnClickListener {
            if(isParentalEnabled) {
                if (!isFactoryMode) {
                    listener.onClicked(
                        position,
                        inputList[position],
                        blockedInputList[position].isBlocked
                    )
                }
            }
            else {
                listener.onClicked(
                    position,
                    inputList[position],
                   true
                )
            }
        }
        holder.inputLayout?.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            listener.getAdapterPosition(position)
            if (hasFocus) {
                holder.inputLayout?.setBackgroundResource(R.drawable.input_item_focused)
                holder.categoryText?.maxWidth = 200
                holder.categoryText?.setSingleLine()

                holder.categoryText?.setTextColor(
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            "color_gradient"
                        )
                    )
                )

                if (!isFactoryMode) {
                    listener.focusedItem(
                        position, inputList[position],
                        blockedInputList[position].isBlocked
                    )
                    getBlockedLayout(holder, position)
                }
                holder.inputImg?.setBackgroundResource(inputFocusImgs[inputList[position].id!!])

            } else {
                if(!isFactoryMode){
                    if(blockedInputList[position].isBlocked) {
                        holder.categoryText?.maxWidth = 280
                    } else {
                        holder.categoryText?.maxWidth = 500
                    }
                    holder.categoryText?.setSingleLine()
                }
                listener.unfocusedItem()
                holder.inputLayout?.setBackgroundResource(R.drawable.input_item_background)
                holder.categoryText?.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.fti_gtv_relative_layout_edit_text_text_color_off
                    )

                )
                holder.inputImg?.setBackgroundResource(inputImgs[inputList[position].id!!])
                if (!isFactoryMode) {
                    holder.blockedInputLayout?.visibility = View.GONE
                    getIsLocked(holder, position)
                }
                getConnectedInputs(position, holder)

            }
        }

        if(isParentalEnabled) {
            if (!isFactoryMode) {
                holder.inputLayout?.setOnKeyListener { view, keyCode, keyEvent ->
                    if (!inputList[position].inputSourceName.contains("Home")) {
                        if (keyEvent.action == KeyEvent.ACTION_UP) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                if (holder.unlockText?.text == "Unlocked") {
                                    return@setOnKeyListener true
                                } else {
                                    holder.unlockText?.text = ConfigStringsManager.getStringById("unlocked")
                                    holder.leftArrow?.alpha = 0.4F
                                    holder.rightArrow?.alpha = 1F
                                    listener.blockInput(blockedInputList[position], false, position)
                                    blockedInputList[position].isBlocked = false
                                }
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                if (holder.unlockText?.text == "Locked") {
                                    return@setOnKeyListener true
                                } else {
                                    holder.unlockText?.text = ConfigStringsManager.getStringById("locked")
                                    holder.rightArrow?.alpha = 0.4F
                                    holder.leftArrow?.alpha = 1F
                                    listener.blockInput(blockedInputList[position], true, position)
                                    blockedInputList[position].isBlocked = true
                                }
                            }
                        }
                        false
                    }
                    false
                }
            }
        }

    }

    private fun getBlockedLayout(holder: InputItemViewHolder, position: Int) {
        if(isParentalEnabled) {
            if (!isFactoryMode) {
                if (blockedInputList[position].inputSourceName.contains("Home")) {
                    holder.blockedInputLayout?.visibility = View.GONE
                } else {
                    holder.blockedInputLayout?.visibility = View.VISIBLE
                }
                holder.isLocked?.visibility = View.GONE
                if (blockedInputList[position].isBlocked) {
                    holder.unlockText?.text = ConfigStringsManager.getStringById("locked")
                    holder.rightArrow?.alpha = 0.4F
                    holder.leftArrow?.alpha = 1F

                } else {
                    holder.unlockText?.text = ConfigStringsManager.getStringById("unlocked")
                    holder.leftArrow?.alpha = 0.4F
                    holder.rightArrow?.alpha = 1F

                }
            }
        } else {
            holder.blockedInputLayout?.visibility = View.GONE
        }
    }

    private fun getConnectedInputs(position: Int, holder: InputItemViewHolder) {
        if(position < inputList.size) {
            val value = inputList[position].isAvailable
            holder.inputImg?.setBackgroundResource(inputImgs[inputList[position].id!!])
            if (value == 0) {
                holder.categoryText?.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.fti_gtv_relative_layout_edit_text_text_color_off
                    )

                )
            } else {
                holder.categoryText?.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
        }
    }

    private fun getIsLocked(holder: InputItemViewHolder, position: Int) {
        if(isParentalEnabled) {
            if (!isFactoryMode) {
                if (blockedInputList[position].isBlocked) {
                    holder.isLocked?.visibility = View.VISIBLE
                } else {
                    holder.isLocked?.visibility = View.GONE
                }
            }
        } else {
            holder.isLocked?.visibility = View.GONE
        }

    }

    override fun getItemCount(): Int {
        return inputList.size
    }

    fun requestFocus(position: Int) {
        if (holders.containsKey(position)) {
            if (focusedItemViewHolder != null) {
                focusedItemViewHolder?.inputLayout?.setBackgroundResource(R.drawable.input_item_background)
                focusedItemViewHolder?.categoryText?.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.fti_gtv_relative_layout_edit_text_text_color_off
                    )

                )
                focusedItemViewHolder?.inputImg?.setBackgroundResource(inputImgs[inputList[position].id!!])
            }

            focusedItemViewHolder = holders[position]
            focusedItemViewHolder?.inputLayout?.setBackgroundResource(R.drawable.input_item_focused)
            focusedItemViewHolder?.categoryText?.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        "color_gradient"
                    )
                )
            )
            focusedItemViewHolder?.inputImg?.setBackgroundResource(inputFocusImgs[inputList[position].id!!])
            getBlockedLayout(focusedItemViewHolder!!,position)
        }
    }

    fun getFocusedViewHolder(): InputItemViewHolder? {
        return focusedItemViewHolder
    }

    fun refresh(adapterItems: InputInformation) {
        this.inputList.clear()
        adapterItems.inputData?.let { this.inputList.addAll(it) }
        this.availableinputList.clear()
        this.inputImgs.clear()
        adapterItems.inputDataImg?.let { inputImgs.addAll(it) }
        this.inputFocusImgs.clear()
        adapterItems.inputDataFocusImg?.let { inputFocusImgs.addAll(it) }
        this.blockedInputList.clear()
        adapterItems.blockedInputData?.let { blockedInputList.addAll(it) }
        this.isFactoryMode = adapterItems.isFactoryMode == true
        this.isParentalEnabled = adapterItems.isParentalEnabled == true
        notifyDataSetChanged()
    }

    interface InputSourceListener {
        fun getAdapterPosition(position: Int)
        fun onClicked(position: Int, inputData: InputItem, blocked: Boolean)
        fun adapterSet()
        fun blockInput(inputData: InputSourceData, isBlock : Boolean, position: Int)

        fun unfocusedItem()

        fun focusedItem(position: Int, inputData: InputItem, blocked: Boolean)

    }


}