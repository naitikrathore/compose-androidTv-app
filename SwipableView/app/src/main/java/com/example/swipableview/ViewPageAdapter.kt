package com.example.swipableview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView



class ViewPageAdapter (
    val images:List<Int>
):RecyclerView.Adapter<ViewPageAdapter.ViewPagerHolder>(){
    inner class ViewPagerHolder(itemView: View):RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerHolder {
//        now we will inflate (create view object of XML) our item_view_paper Layout to show it in viewPagerAdapter
        val view=LayoutInflater.from(parent.context).inflate(R.layout.item_view_paper,parent,false)
        return ViewPagerHolder(view)
    }
    override fun getItemCount(): Int {
        return images.size
    }
    override fun onBindViewHolder(holder: ViewPagerHolder, position: Int) {
        val curimage = images[position]
        val imageView = holder.itemView.findViewById<ImageView>(R.id.ivimage)
        imageView.setImageResource(curimage)

    }



}
