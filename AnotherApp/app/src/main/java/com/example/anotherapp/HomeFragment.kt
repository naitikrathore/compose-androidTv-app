package com.example.anotherapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import com.example.anotherapp.databinding.FragmentHomeBinding


class HomeFragment : BrowseSupportFragment() {

    val TAG:String = HomeFragment::class.java.simpleName

private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        binding=FragmentHomeBinding.inflate(layoutInflater)
//        binding.root.requestFocus()
//        binding.root.setOnKeyListener(object : View.OnKeyListener{
//            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
//               if(event?.action == KeyEvent.ACTION_DOWN){
//                   when (keyCode){
//                       KeyEvent.KEYCODE_DPAD_CENTER-> {
//                           Log.d(TAG, "onKey: Center")
//                           return true
//                       }
//
//                   }
//               }
//                return false
//            }
//        })
//        return binding.root
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = "My Player App"
        setupUi()
        loadRows()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupUi() {
        brandColor = resources.getColor(R.color.search_opaque, null)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

    }

    private fun loadRows() {

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
//        val gridItemPresenter = GridItemPresenter()
//        val header=HeaderItem(0,"Grid Item Presenter")
//        val rowItemAdapterGrid=ArrayObjectAdapter(gridItemPresenter)
//        rowItemAdapterGrid.add("Movie 1")
//        rowItemAdapterGrid.add("Movie 2")
//        rowItemAdapterGrid.add("Movie 3")
//        rowItemAdapterGrid.add("Movie 4")
//        rowsAdapter.add(ListRow(header,rowItemAdapterGrid))


        val cardPresenter =CardPresenter()
        for (j in 0 until 3) {
            val rowItemAdapter = ArrayObjectAdapter(cardPresenter)
            val header2 = HeaderItem(j.toLong(), "CardPresenter ${j}")
            for (k in 0 until 8) {
                val movie = Movie(
                    id = k.toLong(),
                    title = "Title$k",
                    studio = "Studio$k"
                )
                rowItemAdapter.add(movie)
            }
            rowsAdapter.add(ListRow(header2, rowItemAdapter))
        }
        adapter = rowsAdapter

    }


    class GridItemPresenter : Presenter() {
        companion object {
            private const val GRID_ITEM_WIDTH = 300
            private const val GRID_ITEM_HEIGHT = 200
        }

        @SuppressLint("ResourceAsColor")
        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            val view = TextView(parent?.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(parent!!.context, R.color.search_opaque))
            view.setTextColor(
                ContextCompat.getColor(
                    parent!!.context,
                    R.color.background_gradient_end
                )
            )
            view.gravity = Gravity.CENTER
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            (viewHolder?.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {

        }
    }

}