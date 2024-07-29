package com.iwedia.cltv.components

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.utils.Utils.Companion.checkIfDigit

/**
 * HorizontalButtonsAdapter
 *
 * @author Vasilisa Laganin
 */

class HorizontalButtonsAdapter(
    private val preventClip: Boolean = false,
    private val ttsSetterInterface: TTSSetterInterface
) : RecyclerView.Adapter<HorizontalButtonsAdapterViewHolder>() {

    val items = mutableListOf<ButtonType>()

    var isClickable = true

    var adapterPosition = -1

    var listener: HorizontalButtonsAdapterListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HorizontalButtonsAdapterViewHolder {
        return HorizontalButtonsAdapterViewHolder(
            ConstraintLayout(parent.context),
            preventClip,
            ttsSetterInterface
        )
    }

    override fun onBindViewHolder(holder: HorizontalButtonsAdapterViewHolder, position: Int) {
        var item = items[position]

        holder.customButton.update(item)
        adapterPosition = holder.adapterPosition

        if(isClickable){
            holder.customButton.setOnClick {
                listener!!.itemClicked(
                    item,
                    object : IAsyncCallback {
                        override fun onSuccess() {
                        }

                        override fun onFailed(error: Error) {
                        }
                    })
            }
        }

        holder.customButton.setOnFocusChanged { hasFocus ->
            listener!!.onFocusChanged(hasFocus)
        }

        holder.customButton.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (holder.customButton.isAnimationInProgress) return true

                        if (listener != null) {
                            return listener!!.onKeyUp(holder.adapterPosition)
                        }
                    }else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (holder.customButton.isAnimationInProgress) return true
                            if (listener != null) {
                                return listener!!.onKeyDown(holder.adapterPosition)
                        }

                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (listener != null) {
                            if (holder.customButton.isAnimationInProgress) return true
                            return if (ViewCompat.getLayoutDirection(p0!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                listener!!.onKeyRight(holder.adapterPosition)
                            } else {
                                listener!!.onKeyLeft(holder.adapterPosition)
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (holder.customButton.isAnimationInProgress) return true

                        return if (ViewCompat.getLayoutDirection(p0!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener!!.onKeyLeft(holder.adapterPosition)
                        } else {
                            listener!!.onKeyRight(holder.adapterPosition)
                        }
                    }else if (keyCode == KeyEvent.KEYCODE_BOOKMARK) {
                        listener!!.onKeyBookmark()
                        return true
                    }else if (keyCode == KeyEvent.KEYCODE_CAPTIONS) {
                        return listener!!.onCCPressed()
                    }else if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP){
                        return listener!!.onChannelUpPressed()
                    }else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN){
                        return listener!!.onChannelDownPressed()
                    }else if (checkIfDigit(keyCode)){
                    if(listener!=null){
                        val digit = if (keyEvent.keyCode<KeyEvent.KEYCODE_NUMPAD_0){
                            keyEvent.keyCode - KeyEvent.KEYCODE_0
                        }else{
                            keyEvent.keyCode - KeyEvent.KEYCODE_NUMPAD_0
                        }
                        listener!!.onDigitPressed(digit)
                        return true
                    }
                }
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        downActionBackKeyDone = true
                    }
                }
                 if (keyEvent.action == KeyEvent.ACTION_UP) {
                     if (keyCode== KeyEvent.KEYCODE_BACK) {
                         if (holder.customButton.isAnimationInProgress ) return true
                         if (!downActionBackKeyDone) return true
                         return listener?.onBackPressed()!!
                     }
                 }

                    return false
            }
        })
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateButton(position: Int, buttonType: ButtonType){
        items[position] = buttonType
        notifyItemChanged(position)
    }

    fun setNotClickable(position: Int) {
        isClickable = false
        notifyItemChanged(position)
//        isClickable = true
    }

    fun setClickable(position: Int) {
        isClickable = true
        notifyItemChanged(position)
    }
    interface HorizontalButtonsAdapterListener: TTSSetterInterface {
        fun itemClicked(buttonType: ButtonType, callback: IAsyncCallback)
        fun onKeyUp(position: Int): Boolean
        fun onKeyDown(position: Int): Boolean
        fun onKeyRight(position: Int): Boolean
        fun onKeyLeft(position: Int) : Boolean
        fun onKeyBookmark()
        fun onCCPressed(): Boolean
        fun onBackPressed() : Boolean
        fun onChannelDownPressed() : Boolean
        fun onChannelUpPressed() : Boolean
        fun onFocusChanged(hasFocus : Boolean)
        fun onDigitPressed(digit: Int)
    }

    fun refresh(list: MutableList<ButtonType>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
