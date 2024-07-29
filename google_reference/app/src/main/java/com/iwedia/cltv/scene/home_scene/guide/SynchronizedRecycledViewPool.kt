package com.iwedia.cltv.scene.home_scene.guide

import androidx.recyclerview.widget.RecyclerView

class SynchronizedRecycledViewPool : RecyclerView.RecycledViewPool() {

    private val lock = Any()

    override fun putRecycledView(scrap: RecyclerView.ViewHolder) {
        synchronized(lock) {
            super.putRecycledView(scrap)
        }
    }

    override fun getRecycledView(viewType: Int): RecyclerView.ViewHolder? {
        return synchronized(lock) {
            super.getRecycledView(viewType)
        }
    }

    override fun clear() {
        synchronized(lock) {
            super.clear()
        }
    }

    override fun setMaxRecycledViews(viewType: Int, max: Int) {
        synchronized(lock) {
            super.setMaxRecycledViews(viewType, max)
        }
    }

    override fun getRecycledViewCount(viewType: Int): Int {
        return synchronized(lock) {
            super.getRecycledViewCount(viewType)
        }
    }
}