package com.iwedia.cltv.components

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.allViews
import androidx.core.view.doOnNextLayout
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.text_to_speech.Type

private const val TAG = "PreferenceSubMenuAdapter"
class PreferenceSubMenuAdapter(
    val items: MutableList<PrefItem<Any>>,
    val view: ViewGroup,
    val listener: Listener
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener: TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
        fun onBack(action: Action) : Boolean
        fun updateInfo()
        fun isAccessibilityEnabled(): Boolean
        fun updateHintText(status: Boolean, type: Int)
        fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink?
        fun onExit()
    }

    //used to restrict action when it is required in some special cases
    //actions like moving up,down,back etc.
    var restrictMovement = false

    var container : PrefContainer? = null
    var recyclerView : RecyclerView? = null
    var viewHolders  = mutableListOf<RecyclerView.ViewHolder>()
    var mapList: HashMap<Int, String> = hashMapOf()
    var tempChannelList: MutableList<PrefItem<Any>> = mutableListOf()
    private var previousIndex = 0

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        this.tempChannelList.addAll(items)
        // To directly open preference from deeplink
        if(listener.getPreferenceDeepLink()!=null){
            getDeeplinkPosition(items)?.let {
                val defaultNavigation = items[it].id == listener.getPreferenceDeepLink()!!.prefId
                (recyclerView as VerticalGridView).selectedPosition = it
                if(defaultNavigation){
                    recyclerView.doOnNextLayout {
                        recyclerView.requestFocus()
                    }
                }
                recyclerView.viewTreeObserver?.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        recyclerView.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                        (recyclerView.findViewHolderForAdapterPosition(recyclerView.selectedPosition) as? PrefSubCategoryViewHolder)?.let {
                            if(!defaultNavigation) it.setSelected()
                            createSubMenu(it)
                        }
                    }
                })
            }
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    private fun getDeeplinkPosition(items: MutableList<PrefItem<Any>>): Int? {
        items.forEach { it ->
            val isParentOfDefaultNavigation =
                (it.data as? RootItem)?.itemList?.let { getDeeplinkPosition(it) } != null
            if (it.id == listener.getPreferenceDeepLink()!!.prefId || isParentOfDefaultNavigation) return items.indexOf(it)
        }
        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewHolderType.VT_SWITCH.ordinal -> {
                PrefSwitchViewHolder(
                    parent,
                    ttsSetterForSelectableViewInterface = listener
                )
            }

            ViewHolderType.VT_SEEKBAR.ordinal -> {
                PrefSeekBar(parent = parent)
            }

            ViewHolderType.VT_RADIO.ordinal -> {
                PrefRadioViewHolder(
                    parent = parent,
                    ttsSetterForSelectableViewInterface = listener
                )
            }

            ViewHolderType.VT_CHECKBOX.ordinal -> {
                PrefCheckBoxViewHolder(
                    parent = parent,
                    ttsSetterForSelectableViewInterface = listener
                )
            }

            ViewHolderType.VT_MENU.ordinal -> {
                PrefSubCategoryViewHolder(
                    parent = parent,
                    ttsSetterInterface = listener
                )
            }

            ViewHolderType.VT_EDIT_CHANNEL.ordinal -> {
                PrefEditChannelViewHolderInterface(
                    parent = parent,
                    ttsSetterInterface = listener,
                    ttsSetterForSelectableViewInterface = listener
                )
            }

            else -> (null as RecyclerView.ViewHolder)
        }
    }

    var scrollListener : RecyclerView.OnScrollListener? = null

    fun open(viewHolder: RecyclerView.ViewHolder){

        if (scrollListener != null) {
            recyclerView!!.removeOnScrollListener(scrollListener!!)
            scrollListener = null
        }

        if(container!=null && container!!.parentHolder == viewHolder) return //prevent reopening

        if (recyclerView!!.scrollState != SCROLL_STATE_IDLE) {
            scrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int
                ) {
                    if (newState == SCROLL_STATE_IDLE){
                        createSubMenu(viewHolder)
                    }
                    super.onScrollStateChanged(recyclerView, newState)
                }
            }

            recyclerView!!.addOnScrollListener(scrollListener!!)
        } else {
            createSubMenu(viewHolder)
        }
    }

    fun removeSubMenu(){
        if (scrollListener != null) {
            recyclerView!!.removeOnScrollListener(scrollListener!!)
            scrollListener = null
        }
        if (container != null) {
            view.removeView(container!!.view)
            container = null
        }
    }

    fun createSubMenu(viewHolder: RecyclerView.ViewHolder){

        removeSubMenu()

        val itemList = (items[viewHolder.adapterPosition].data as RootItem).itemList

        if(itemList==null || itemList.isEmpty()) return

        container = PrefContainer(view, viewHolder, itemList,
            object : PrefContainer.PrefContainerListener {
                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    listener.showToast(text, duration)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

                override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                    return listener.getPreferenceDeepLink()
                }

                override fun onExit() {
                    return listener.onExit()
                }
            })

        view.addView(container!!.view)

    }

    fun setFocusOfViewHolders(boolean: Boolean){
        viewHolders.forEach { holder->
            holder.itemView.allViews.forEach {
                if(boolean) {
                    it!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
                }else{
                    it!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(listener.isAccessibilityEnabled()) {
            holder.itemView.allViews.forEach {
                it!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            }
        }

        viewHolders.add(holder)
        val outerItem = items[position]
        when(outerItem.viewHolderType){
            (ViewHolderType.VT_SWITCH)->{
                (holder as PrefSwitchViewHolder).refresh(outerItem.data as CompoundItem,outerItem.id,
                    object : PrefItemListener {
                        override fun onAction(action: Action,id:Pref): Boolean {
                            val item = items[holder.adapterPosition]
                            val compoundItem = item.data as CompoundItem
                            if(action==Action.FOCUSED){
                                removeSubMenu()
                            }
                            if(action==Action.RIGHT){
                                return true
                            }
                            if (action==Action.CLICK){
                                return item.listener.onAction(action,id)
                            }
                            if (action == Action.UPDATE) {
                                //to remove and add radio button based on switch status
                                if(compoundItem.isChecked){
                                    item.data.hiddenOptions.forEach {
                                        items.add(it)
                                        notifyItemInserted(items.indexOf(it))
                                    }
                                    item.data.hiddenOptions.clear()
                                }else{
                                    items.toList().forEach {
                                        if(it.viewHolderType == ViewHolderType.VT_RADIO || it.viewHolderType== ViewHolderType.VT_CHECKBOX){
                                            item.data.hiddenOptions.add(it)
                                            notifyItemRemoved(items.indexOf(it))
                                            items.remove(it)
                                        }
                                    }
                                }
                                listener.updateInfo()
                            }
                            if(action==Action.LEFT || action==Action.BACK || (action==Action.UP && items.first()==item)){
                                if (restrictMovement)return true
                                onBack(action)
                                return true
                            }
                            return item.listener.onAction(action,id)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            listener.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            listener.showToast(text, duration)
                        }
                    })
            }

            (ViewHolderType.VT_SEEKBAR)->{
                (holder as PrefSeekBar).refresh(outerItem.data as SeekBarItem,outerItem.id,
                    object : PrefItemListener {
                        override fun onAction(action: Action,id:Pref): Boolean {

                            val item = items[holder.adapterPosition]
                            if(action==Action.FOCUSED){
                                removeSubMenu()
                            }
                            if(action==Action.FOCUSED){
                                open(holder)
                            }
                            if(action==Action.RIGHT || action==Action.CLICK){
                                return true
                            }
                            if(action==Action.LEFT || action==Action.BACK || (action==Action.UP && items.first()==item)){
                                onBack(action)
                                return true
                            }
                            return item.listener.onAction(action,id)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            listener.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            listener.showToast(text, duration)
                        }
                    })
            }

            (ViewHolderType.VT_RADIO)-> {
                (holder as PrefRadioViewHolder).refresh(outerItem.data as CompoundItem,outerItem.id,
                    object : PrefItemListener {
                        override fun onAction(action: Action, id: Pref): Boolean {

                            val item = items[holder.adapterPosition]
                            val radioItem = item.data as CompoundItem

                            if (action == Action.FOCUSED) {
                                open(holder)
                            }
                            if (action == Action.UPDATE) {
                                //to uncheck all other radio buttons
                                items.forEach {
                                    if(it.viewHolderType == ViewHolderType.VT_RADIO){
                                        val otherRadioItem = it.data as CompoundItem
                                        if(it.id == item.id && radioItem.id != otherRadioItem.id && otherRadioItem.isChecked){
                                            otherRadioItem.isChecked = false
                                            notifyItemChanged(items.indexOf(it))
                                        }
                                    }
                                }
                                listener.updateInfo()
                            }
                            if (action == Action.RIGHT) {
                                return true
                            }

                            if(action == Action.CLICK) {
                                return item.listener.onAction(action,id)
                            }

                            if(action==Action.LEFT || action==Action.BACK || (action==Action.UP && items.first()==item)){
                                onBack(action)
                                return true
                            }
                            return item.listener.onAction(action,id)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            listener.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            listener.showToast(text, duration)
                        }
                    })
                holder.setListener(listener)
            }

            (ViewHolderType.VT_EDIT_CHANNEL)-> {
                (holder as PrefEditChannelViewHolderInterface).refresh(outerItem.data as ChannelItem,outerItem.id,
                    object : PrefItemListener {
                        override fun onAction(action: Action, id: Pref): Boolean {
                            try {
                                items[holder.adapterPosition]
                            }catch (E: Exception){
                                E.printStackTrace()
                                return false
                            }
                            val item = items[holder.adapterPosition]
                            val channelItem = item.data as ChannelItem

                            if (action == Action.FOCUSED) {
                                run exitForEach@ {
                                    items.forEach {
                                        if ((it.data as ChannelItem).editChannel==EditChannel.CHANNEL_MOVE) {
                                            if (it.data.isChecked && !it.data.isLongPressed) {
                                                listener.updateHintText(true, 0)
                                                return@exitForEach
                                            }
                                        }
                                    }
                                }
                            }

                            if (action == Action.LONG_CLICK) {
                                if (id == Pref.MOVE_CHANNEL_ITEM) {

                                    var list : MutableList<PrefItem<Any>> = mutableListOf()
                                    items.forEachIndexed { index, it ->
                                        if ((it.data as ChannelItem).editChannel==EditChannel.CHANNEL_MOVE) {
                                            it.data.isEnabled = it.data.isChecked
                                            it.data.isLongPressed = true

                                            if (it.data.isChecked) {
                                                list.add(it)
                                            }
                                        }
                                    }

                                    if (list.size > 0) {
                                        listener.updateHintText(true, 1)
                                        items.removeAll(list)

                                        if (holder.bindingAdapterPosition < items.size) {
                                            items.addAll(holder.bindingAdapterPosition, list)
                                        } else {
                                            items.addAll(list)
                                        }
                                        notifyDataSetChanged()
                                    }
                                }
                            }

                            if (action == Action.UPDATE) {
                                var selectedItems: MutableList<PrefItem<Any>> = mutableListOf()
                                var selectedChannelsList: ArrayList<TvChannel> = arrayListOf()
                                var targetIndex = holder.adapterPosition

                                items.toList().forEach { it ->
                                    if (it.viewHolderType == ViewHolderType.VT_EDIT_CHANNEL ){
                                        if ((it.data as ChannelItem).editChannel==EditChannel.CHANNEL_MOVE) {

                                            if ((it.data).isLongPressed) {
                                                if (it.data.isChecked) {
                                                    if(selectedChannelsList.isEmpty()){
                                                        previousIndex = it.data.tvChannel.index
                                                    }
                                                    selectedItems.add(it)
                                                    selectedChannelsList.add(it.data.tvChannel)
                                                }

                                                if(items.last() == it && selectedItems.size>0) {
                                                    tempChannelList.forEachIndexed { index, it ->
                                                        mapList[index] = (it.data as ChannelItem).tvChannel.displayNumber
                                                    }

                                                    items.removeAll(selectedItems)

                                                    if (targetIndex < items.size) {
                                                        items.addAll(targetIndex, selectedItems)
                                                    } else {
                                                        items.addAll(selectedItems)
                                                    }

                                                    items.forEachIndexed { index, prefItem ->
                                                        (prefItem.data as ChannelItem).tvChannel.displayNumber = mapList[index]!!
                                                    }

                                                    tempChannelList.clear()
                                                    tempChannelList.addAll(items)

                                                    notifyDataSetChanged()

                                                    listener.updateHintText(false, 0)

                                                    channelItem.editChannelListener?.onMove(selectedChannelsList, previousIndex, targetIndex, mapList, object :IAsyncCallback {
                                                        override fun onFailed(error: Error) {}

                                                        override fun onSuccess() {
                                                            //check this in case refresh sort is not working
                                                            items.sortBy { (it.data as ChannelItem).tvChannel.displayNumber }
                                                            notifyDataSetChanged()
                                                        }
                                                    })
                                                }

                                                it.data.isChecked = false
                                                it.data.isEnabled = true
                                                it.data.isLongPressed = false
                                            } else {
                                                if (items.last() == it) {
                                                    var isChecked = false

                                                    run forEachExit@{
                                                        items.forEach {
                                                            if ((it.data as ChannelItem).isChecked) {
                                                                isChecked = true
                                                                return@forEachExit
                                                            }
                                                        }
                                                    }
                                                    if (isChecked) listener.updateHintText(true, 0)
                                                    else listener.updateHintText(false, 0)
                                                }
                                            }
                                        }
                                        else if(it.data.editChannel==EditChannel.CHANNEL_SWAP){
                                            val otherRadioItem = it.data as CompoundItem

                                            var firstItemPosition = items.indexOf(it)
                                            var secondItemPosition = items.indexOf(item)
                                            if(it.id == item.id && channelItem.id != otherRadioItem.id && otherRadioItem.isChecked){

                                                channelItem.editChannelListener?.onSwap(it.data.tvChannel,item.data.tvChannel,firstItemPosition,secondItemPosition,object :IAsyncCallback{
                                                    override fun onFailed(error: Error) {
                                                    }

                                                    override fun onSuccess() {
                                                        otherRadioItem.isChecked = false
                                                        channelItem.isChecked = false
                                                        var firstDisplayNum = it.data.tvChannel.displayNumber
                                                        var secondDisplayNum = item.data.tvChannel.displayNumber
                                                        items[firstItemPosition]=item
                                                        items[secondItemPosition]=it
                                                        (items[firstItemPosition].data as ChannelItem).tvChannel.displayNumber = firstDisplayNum
                                                        (items[secondItemPosition].data as ChannelItem).tvChannel.displayNumber = secondDisplayNum
                                                        notifyItemChanged(firstItemPosition)
                                                        notifyItemChanged(secondItemPosition)
                                                    }


                                                })
                                            }
                                        }

                                    }

                                }
                                if((item.data).editChannel==EditChannel.CHANNEL_DELETE){
                                    val deletedIndex = items.indexOf(item)
                                    channelItem.editChannelListener?.onDelete(item.data.tvChannel,deletedIndex,object :
                                        IAsyncCallback {
                                        override fun onFailed(error: Error) {
                                        }

                                        override fun onSuccess() {
                                            notifyItemRemoved(deletedIndex)
                                            items.remove(item)
                                            if(items.isEmpty()) onBack(Action.BACK)
                                        }
                                    })
                                    return true

                                }

                                listener.updateInfo()
                            }
                            if (action == Action.RIGHT || action == Action.CLICK) {
                                return true
                            }
                            if(action==Action.LEFT || action==Action.BACK || (action==Action.UP && items.first()==item)){
                                items.forEach {
                                    if(it.viewHolderType == ViewHolderType.VT_EDIT_CHANNEL) {
                                        if ((it.data as ChannelItem).editChannel == EditChannel.CHANNEL_MOVE) {

                                            if (it.data.isLongPressed) {
                                                it.data.isEnabled = true
                                                it.data.isLongPressed = false

                                                if (items.last() == it) {
                                                    items.clear()
                                                    items.addAll(tempChannelList)
                                                    notifyDataSetChanged()

                                                    return true
                                                }
                                            }

                                            if (action != Action.UP) listener.updateHintText(false, 0)
                                            else return false
                                        }

                                    }
                                }
                                onBack(action)
                                return true
                            }
                            return item.listener.onAction(action,id)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            listener.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            listener.showToast(text, duration)
                        }
                    })
                holder.setListener(listener)
            }


            (ViewHolderType.VT_CHECKBOX)-> {

                (holder as PrefCheckBoxViewHolder).refresh(outerItem.data as CompoundItem,outerItem.id,
                    object : PrefItemListener {
                        override fun onAction(action: Action, id: Pref): Boolean {
                            try {
                                items[holder.adapterPosition]
                            }catch (E: Exception){
                                E.printStackTrace()
                                return false
                            }
                            val item = items[holder.adapterPosition]
                            if(action==Action.FOCUSED){
                                removeSubMenu()
                            }
                            if (action == Action.RIGHT) {
                                return true
                            }
                            if(action == Action.CLICK) {
                                return item.listener.onAction(action,id)
                            }

                            if (action == Action.UPDATE) {
                                listener.updateInfo()
                            }
                            if(action==Action.LEFT || action==Action.BACK || (action==Action.UP && items.first()==item)){
                                onBack(action)
                                return true
                            }
                            return item.listener.onAction(action,id)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            listener.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            listener.showToast(text, duration)
                        }
                    })
                holder.setListener(listener)
            }
            ViewHolderType.VT_MENU ->{

                (holder as PrefSubCategoryViewHolder).refresh(outerItem.data as RootItem,outerItem.id,object : PrefItemListener {
                    override fun onAction(action: Action,id:Pref): Boolean {

                        val item = items[holder.adapterPosition]
                        val categoryItem = item.data as RootItem
                        if(action==Action.FOCUSED){
                            open(holder)
                        }
                        if(action==Action.RIGHT){
                            if(scrollListener==null && categoryItem.itemList!=null&& categoryItem.itemList.isNotEmpty())  {
                                container!!.view!!.requestFocus()
                                holder.setSelected()
                            }
                            item.listener.onAction(action,id)
                            return true
                        }

                        if( action==Action.CLICK) {
                            if(scrollListener==null && categoryItem.itemList!=null&& categoryItem.itemList.isNotEmpty())  {
                                container!!.view!!.requestFocus()
                                holder.setSelected()
                                return true
                            }
                            return item.listener.onAction(action,id)
                        }

                        var firstEnabledItem = items.first()

                        if(firstEnabledItem.data is RootItem) {
                            while (!(firstEnabledItem.data as RootItem).isEnabled && items.last() != firstEnabledItem){
                                firstEnabledItem = items[items.indexOf(firstEnabledItem)+1]
                            }
                        }

                        if (action == Action.LEFT || action == Action.BACK || (action == Action.UP && firstEnabledItem == item)) {
                            val success =  onBack(action)
                            if(success) removeSubMenu()
                            return success
                        }
                        return item.listener.onAction(action,id)
                    }

                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        listener.setSpeechText(text = text, importance = importance)
                    }

                    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                        listener.showToast(text, duration)
                    }
                })
            }
            else -> {

            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }


    override fun getItemViewType(position: Int): Int {
        return items[position].viewHolderType.ordinal
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder)
    }

    fun notifyPreferenceUpdated(){
        viewHolders.forEach {
            (it as PrefComponentInterface<*>).notifyPreferenceUpdated()
        }
        container?.notifyPreferenceUpdated()
    }

    fun onBack(action: Action): Boolean {
        val result = listener.onBack(action)
        if (result && listener.getPreferenceDeepLink()!=null && getDeeplinkPosition(items)!=null) {
            listener.onExit()
        }
        return result
    }

}