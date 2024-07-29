package com.iwedia.cltv.scene.parental_control

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider.Companion.getTypeFace
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager.Companion.getFont
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants


/**
 * Enter pin adapter
 *
 * @author Aleksandar Lazic
 */
class EnterPinAdapter(val items: MutableList<PinItem>) :
    RecyclerView.Adapter<EnterPinViewHolder<TextView>>() {

    /**
     * Enter pin listener
     */
    var listener: EnterPinListener? = null

    /**
     * Focused item position
     */
    private var focusedPosition = 0

    /**
     * Is confirm enabled
     */
    private var isConfirmEnabled = false
    val TAG = javaClass.simpleName
    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    @SuppressLint("ResourceType")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EnterPinViewHolder<TextView> {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pin_item, parent, false)
        val viewHolder = EnterPinViewHolder<TextView>(view)
        viewHolder.digitUp!!.typeface =
            getTypeFace(ReferenceApplication.applicationContext(), getFont("font_medium"))
        viewHolder.digitDown!!.typeface =
            getTypeFace(ReferenceApplication.applicationContext(), getFont("font_medium"))
        viewHolder.digitUp!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        viewHolder.digitDown!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        view.isClickable = true
        view.isFocusable = true

        if (listener!!.isAccessibilityEnabled()) {
            viewHolder.rootView!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)

            viewHolder.digitUp!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
            viewHolder.digitUp!!.isFocusable = false
            viewHolder.digitUp!!.visibility = View.VISIBLE
            viewHolder.digitUp!!.setOnClickListener {
                val pinItem = items.get(viewHolder.adapterPosition)
                var value = pinItem!!.getValue()
                if (value == 0) {
                    value = 9
                } else {
                    value--
                }
                pinItem.setValue(value)
                if (value - 1 < 0) {
                    viewHolder.digitUp!!.text = (9).toString()
                } else {
                    viewHolder.digitUp!!.text = (value - 1).toString()
                }
                if (value == 9) {
                    viewHolder.digitDown!!.text = (0).toString()
                } else {
                    viewHolder.digitDown!!.text = (value + 1).toString()
                }
                viewHolder.pinTextView!!.text = value.toString()
            }
            viewHolder.digitUp!!.text = "9"

            viewHolder.digitDown!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
            viewHolder.digitDown!!.isFocusable = false
            viewHolder.digitDown!!.visibility = View.VISIBLE

            viewHolder.digitDown!!.setOnClickListener {
                val pinItem = items.get(viewHolder.adapterPosition)
                var value = pinItem!!.getValue()
                if (value == 9) {
                    value = 0
                } else {
                    value++
                }
                pinItem.setValue(value)
                if (value == 0) {
                    viewHolder.digitUp!!.text = (9).toString()
                } else {
                    viewHolder.digitUp!!.text = (value - 1).toString()
                }
                if (value == 9) {
                    viewHolder.digitDown!!.text = (0).toString()
                } else {
                    viewHolder.digitDown!!.text = (value + 1).toString()
                }
                viewHolder.pinTextView!!.text = value.toString()
            }
            viewHolder.digitDown!!.text = "1"

            viewHolder.pinTextView!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
            viewHolder.pinTextView!!.isFocusable = true

            viewHolder.pinTextView!!.setOnClickListener {
                if (viewHolder.adapterPosition == 3) {
                    if (listener != null) {
                        listener!!.onPinConfirmed(getPinCode())
                    }
                }
            }
        }

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateViewHolder: Exception color_context $color_context")
            viewHolder.pinTextView!!.setTextColor(color_context)
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateViewHolder: Exception color rdb $ex")
        }


        if(!listener!!.isAccessibilityEnabled()) {
            view.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (viewHolder.adapterPosition < 0) {
                    return@OnFocusChangeListener
                }
                val pinItem = items.get(viewHolder.adapterPosition)
                if (hasFocus) {
                    focusedPosition =
                        if (viewHolder.adapterPosition < 0) 0 else viewHolder.adapterPosition
                    listener!!.getAdapterPosition(focusedPosition)
                    if (focusedPosition == items.size - 1) {
                        isConfirmEnabled = true
                        listener!!.validationEnabled()
                    }

                    viewHolder.pinTextViewWrapper!!.background =
                        ContextCompat.getDrawable(
                            getContext(),
                            R.drawable.pin_rounded_focused_layout
                        )
                    viewHolder.pinTextViewWrapper!!.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_background"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateViewHolder: Exception color_context $color_context")
                        viewHolder.pinTextView!!.setTextColor(color_context)
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateViewHolder: Exception color rdb $ex")
                    }
                    viewHolder.pinTextView!!.text = java.lang.String.valueOf(pinItem!!.getValue())
                    viewHolder.digitUp!!.visibility = View.VISIBLE
                    viewHolder.digitDown!!.visibility = View.VISIBLE
                    if (pinItem.getValue() == 9) {
                        viewHolder.digitDown!!.text = String.format("%d", 0)
                    } else {
                        viewHolder.digitDown!!.text = String.format("%d", pinItem.getValue() + 1)
                    }
                    if (pinItem.getValue() == 0) {
                        viewHolder.digitUp!!.text = String.format("%d", 9)
                    } else {
                        viewHolder.digitUp!!.text = String.format("%d", pinItem.getValue() - 1)
                    }
                    listener!!.setSpeechText(
                        viewHolder.pinTextView!!.text.toString()
                    )
                } else {
                    viewHolder.pinTextViewWrapper!!.background =
                        ContextCompat.getDrawable(getContext(), R.drawable.pin_rounded_layout)
                    viewHolder.pinTextViewWrapper!!.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_not_selected")))

                    viewHolder.pinTextView!!.doOnPreDraw {
                        if (pinItem.getType() == PinItem.TYPE_PASSWORD && focusedPosition != viewHolder.adapterPosition) {
                            viewHolder.pinTextView!!.text = PinItem.MASK_VALUE
                        }
                    }
                    viewHolder.digitUp!!.visibility = View.INVISIBLE
                    viewHolder.digitDown!!.visibility = View.INVISIBLE
                }
            }
        }

        view.setOnClickListener {
            if (focusedPosition == itemCount - 1) {
                if (isConfirmEnabled) {
                    if (listener != null) {
                        listener!!.onPinConfirmed(getPinCode())
                    }
                }
            } else {
                recyclerView?.let {
                    if (it.size > focusedPosition + 1) {
                        val item = it.findViewHolderForAdapterPosition(focusedPosition + 1)
                        item?.itemView?.requestFocus()
                    }
                }
            }
        }

        view.setOnKeyListener(View.OnKeyListener { _, keyCode, keyEvent ->
            val pinItem = items.get(focusedPosition)
            var value = pinItem!!.getValue()
            if (!ReferenceApplication.isInputPaused) {
                if (!ReferenceApplication.worldHandler!!.isEnableUserInteraction) {
                    return@OnKeyListener true
                }
            }
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                        val digit = keyCode - KeyEvent.KEYCODE_0
                        pinItem.setValue(digit)

                        if (digit == 0) {
                            viewHolder.digitUp!!.text = (9).toString()
                        } else {
                            viewHolder.digitUp!!.text = (digit - 1).toString()
                        }

                        if (digit == 9) {
                            viewHolder.digitDown!!.text = (0).toString()
                        } else {
                            viewHolder.digitDown!!.text = (digit + 1).toString()
                        }

                        viewHolder.pinTextView!!.text = digit.toString()
                        if (listener != null) {
                            listener!!.next()
                        }
                        if (viewHolder.adapterPosition == itemCount - 1) {
                            listener!!.onPinConfirmed(getPinCode())
                            return@OnKeyListener true
                        }
                        return@OnKeyListener true
                    }

                    KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                        val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                        pinItem.setValue(digit)
                        viewHolder.pinTextView!!.text = digit.toString()
                        if (listener != null) {
                            listener!!.next()
                        }
                        if (viewHolder.adapterPosition == itemCount - 1) {
                            listener!!.onPinConfirmed(getPinCode())
                            return@OnKeyListener true
                        }
                        return@OnKeyListener true
                    }
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (value == 0) {
                        value = 9
                    } else {
                        value--
                    }


                    pinItem.setValue(value)


                    if (value - 1 < 0) {
                        viewHolder.digitUp!!.text = (9).toString()
                    } else {
                        viewHolder.digitUp!!.text = (value - 1).toString()
                    }

                    if (value == 9) {
                        viewHolder.digitDown!!.text = (0).toString()
                    } else {
                        viewHolder.digitDown!!.text = (value + 1).toString()
                    }

                    viewHolder.pinTextView!!.text = value.toString()

                    listener!!.setSpeechText(
                        viewHolder.pinTextView!!.text.toString()
                    )
                    return@OnKeyListener true
                }
                else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {

                    if (value == 9) {
                        value = 0
                    } else {
                        value++
                    }

                    pinItem.setValue(value)

                    if (value == 0) {
                        viewHolder.digitUp!!.text = (9).toString()
                    } else {
                        viewHolder.digitUp!!.text = (value - 1).toString()
                    }

                    if (value == 9) {
                        viewHolder.digitDown!!.text = (0).toString()
                    } else {
                        viewHolder.digitDown!!.text = (value + 1).toString()
                    }


                    viewHolder.pinTextView!!.text = value.toString()

                    listener!!.setSpeechText(
                        viewHolder.pinTextView!!.text.toString()
                    )

                    return@OnKeyListener true
                }
                else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (listener != null) {
                        if (viewHolder.adapterPosition == 0) {
                            listener!!.previous()
                            return@OnKeyListener true
                        }
                    }
                    return@OnKeyListener false
                }
                else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (listener != null) {
                        if (viewHolder.adapterPosition == itemCount - 1) {
                            listener!!.next()
                            return@OnKeyListener true
                        }
                    }
                    return@OnKeyListener false
                }
            }
            if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (isConfirmEnabled) {
                        if (listener != null) {
                            listener!!.onPinConfirmed(getPinCode())
                        }
                    }
                }
            }
            false
        })

        return viewHolder
    }

    override fun onBindViewHolder(holder: EnterPinViewHolder<TextView>, position: Int) {
//        super.onBindViewHolder(holder, position)
        val pinItem = items.get(position)

        holder.pinTextView!!.typeface = getTypeFace(ReferenceApplication.applicationContext(), getFont("font_medium"))

        holder.pinTextView!!.text = java.lang.String.valueOf(items.get(position)!!.getValue())

        if(!listener!!.isAccessibilityEnabled()) {
            if (position == focusedPosition) {
                holder.pinTextView!!.text = java.lang.String.valueOf(items.get(position)!!.getValue())
                holder.itemView.requestFocus()
            } else {
                if (pinItem!!.getType() == PinItem.TYPE_PASSWORD) {
                    holder.pinTextView!!.text = "0"
                } else {
                    holder.pinTextView!!.text = java.lang.String.valueOf(items.get(position)!!.getValue())
                    holder.pinTextView!!.text = PinItem.EMPTY_VALUE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Reset
     */
    fun reset() {
        focusedPosition = 0
        isConfirmEnabled = false
        for (i in 0 until items.size) {
            items[i].setValue(0)
        }
        notifyDataSetChanged()
    }

    /**
     * Focus last
     */
    fun focusLast() {
        focusedPosition = items.size - 1
        isConfirmEnabled = true
        notifyDataSetChanged()
    }

    /**
     * Get pin code
     *
     * @return Pin code
     */
    fun getPinCode(): String? {
        var pin: String? = ""
        for (i in 0 until items.size) {
            if (items.get(i)
                    .getValue() != -1 && java.lang.String.valueOf(items[i].getValue()) != PinItem.EMPTY_VALUE
            ) {
                pin += java.lang.String.valueOf(items[i].getValue())
            }
        }
        return pin
    }

    fun refresh(newItems: List<PinItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    //Context
    private fun getContext(): Context {
        return ReferenceApplication.applicationContext()
    }

    /**
     * Register listener
     *
     * @param listener Listener
     */
    fun registerListener(listener: EnterPinListener) {
        this.listener = listener
    }


    /**
     * Enter pin listener
     */
    interface EnterPinListener: TTSSetterInterface {
        /**
         * On pin confirmed
         *
         * @param pinCode pin code
         */
        fun onPinConfirmed(pinCode: String?)

        /**
         * Get adapter position
         *
         * @param position position
         */
        fun getAdapterPosition(position: Int)

        /**
         * On key left
         */
        fun previous()

        /**
         * On key right
         */
        operator fun next()

        /**
         * Validation enabled
         */
        fun validationEnabled()

        fun isAccessibilityEnabled(): Boolean
    }

}