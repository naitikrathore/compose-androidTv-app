package com.iwedia.cltv.platform.test.player

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.*
import com.iwedia.cltv.platform.model.player.track.ITrack
import com.iwedia.cltv.platform.test.test.R

class TrackAdapter: Adapter<TrackAdapter.TrackViewHolder>() {
    interface TrackAdapterListener {
        fun onItemSelected(itemPosition: Int)
    }

    inner class TrackViewHolder(view: View): ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.trackTitle)
        val selectIcon: ImageView = view.findViewById(R.id.selected_img)

        fun onBind(track: ITrack) {
            title.text = track.getName()
        }
    }

    private var tracks: ArrayList<ITrack> = ArrayList()
    private var selectedIndex = -1

    private val listeners: MutableList<TrackAdapterListener> = mutableListOf()
    fun registerListener(client: TrackAdapterListener) = listeners.add(client)
    fun unregisterListeners(client: TrackAdapterListener) = listeners.remove(client)

    @SuppressLint("NotifyDataSetChanged")
    fun onTracksDataUpdate(newDogList: List<ITrack>, currentActive: Int) {
        tracks.clear()
        tracks.addAll(newDogList)
        selectedIndex = currentActive
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val item = inflater.inflate(R.layout.track_item, parent, false)
        item.focusable = FOCUSABLE
        return TrackViewHolder(item)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.onBind(tracks[position])
        holder.itemView.setOnClickListener { onTrackSelected(position) }
        holder.selectIcon.visibility = if(position == selectedIndex) VISIBLE else INVISIBLE
    }

    private fun onTrackSelected(position: Int) {
        listeners.forEach { it.onItemSelected(position) }
        val oldIndex = selectedIndex
        selectedIndex = position
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }

    override fun getItemCount(): Int = tracks.size
}