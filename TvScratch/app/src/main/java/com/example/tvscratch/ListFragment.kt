package com.example.tvscratch
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.FragmentListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListFragment : RowsSupportFragment(),FocusChangeListener,CardPresenter.updateView {
    private var homeFragInstance:ConnectHomeFrag?=null
//    private var dataLoadListener:DataLoadListener?=null
    override fun updateData(movie: Movie) {
          homeFragInstance?.updateViewFrag(movie)
    }

    private var listRowPresenter = object : ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM) {
        override fun isUsingDefaultListSelectEffect(): Boolean {
            return false
        }
    }.apply {
        shadowEnabled = false

    }

    val viewModel by viewModels<AppViewModel>()
    private lateinit var binding: FragmentListBinding
    private var dataAll:List<Movie> = listOf()
    private var rowsAdapter: ArrayObjectAdapter = ArrayObjectAdapter(listRowPresenter)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter=rowsAdapter
        fetchData()

    }
    private fun loadRows(){
        val movies=dataAll
        val activity=requireActivity() as MainActivity
        val cardPresenter=CardPresenter(activity,this)
        cardPresenter.setInstance(this)
        for(i in 0 until 4){
            val rowItemAdapter=ArrayObjectAdapter(cardPresenter)
            val header=HeaderItem(i.toLong(),"Section ${i}")
            for(j in 0 until 8){
                rowItemAdapter.add(movies[ j % movies.size])
            }
            rowsAdapter.add(ListRow(header,rowItemAdapter))
        }
        adapter=rowsAdapter
    }

    override fun onFocusChangeToFavItem() {
        (activity as MainActivity).onFocusChangeToFavItem()
    }

    private fun fetchData() {
        viewModel.latest_data.observe(viewLifecycleOwner, { entries ->
            entries?.let {
                if(it.isNotEmpty()){
                    Log.e("naitik","${it.toString()}")
                    dataAll=it
                    loadRows()
//                    dataLoadListener?.onDataLoaded()
                }
            }
        })
    }

    fun setMainInstance(callback:ConnectHomeFrag){
        homeFragInstance=callback
    }

    interface ConnectHomeFrag{
        fun updateViewFrag(movie: Movie)
    }
//    fun setSplashInstance(callback:DataLoadListener){
//        dataLoadListener=callback
//    }
//
//    interface DataLoadListener{
//        fun onDataLoaded()
//    }
}