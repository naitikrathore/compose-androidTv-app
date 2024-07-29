package com.example.tvscratch

import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvscratch.data.FocusValue
import com.example.tvscratch.data.Movie
import com.example.tvscratch.data.MovieRow
import com.example.tvscratch.databinding.SingleRowBinding

class VerticalAdapter(private var movieRows: List<MovieRow>, private val sourceFragment: String) :
    RecyclerView.Adapter<VerticalAdapter.RowViewHolder>(), HorizontalAdapter.ParentCall {
    private var mainActivityCall: MainActivityCall? = null
    private var homeFragCall: HomeFragCall? = null
    private var searchFrag:SearchFrag?=null
    var posi=0

    //    private var watchListCall:WatchListCall?=null
    private lateinit var bindng: SingleRowBinding
    var selectedpos = 0
    private var focusValue: FocusValue? = null


    inner class RowViewHolder(private val binding: SingleRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: MovieRow) {
            val horizontalAdapter = HorizontalAdapter(row.items,sourceFragment)
            horizontalAdapter.setFocusPosi(adapterPosition)
            horizontalAdapter.setParetnCall(this@VerticalAdapter)
            binding.innerRecyclerView.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
             posi=adapterPosition
            if (focusValue != null && adapterPosition == focusValue?.verticalPos) {
                Log.e("RTRK", "inside if")
                horizontalAdapter.setFocusValues(focusValue)
                selectedpos = focusValue?.verticalPos!!
                binding.innerRecyclerView.smoothScrollToPosition(focusValue?.horiPos!!)
            }
            binding.innerRecyclerView.adapter = horizontalAdapter



            Log.e("RTRK", "${focusValue?.verticalPos}")
            Log.e("positt", "${adapterPosition}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        bindng = SingleRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowViewHolder(bindng)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(movieRows[position])
    }

    override fun getItemCount(): Int {
        return movieRows.size
    }

    fun updateData(newRows: List<MovieRow>) {
        movieRows = newRows
        notifyDataSetChanged()
    }

    override fun onCenter(movie: Movie) {
        mainActivityCall?.callDetail(movie)
    }

    override fun updateView(movie: Movie) {
        homeFragCall?.updateView(movie)
    }

    override fun onNavigateUp() {

        if (selectedpos == 0) {
            if (sourceFragment == "WatchList")
                mainActivityCall?.onFocusChangeToFav()
            if (sourceFragment == "HomeFrag")
                mainActivityCall?.onFocusChangeToAll()
            if (sourceFragment == "RecentFrag")
                mainActivityCall?.onFocusChangeToRec()
            if(sourceFragment=="SearchFragment"){
                Log.e("SrhEr", "callupVerti")
                searchFrag?.onFocusUp()
            }
        } else {
            if(selectedpos>0)
            selectedpos -= 1
        }
    }


    override fun onNavigateDown() {
        if(selectedpos<posi)
        selectedpos += 1
    }

    fun setMainActivityCall(callback: MainActivityCall) {
        mainActivityCall = callback
    }

    override fun onNavigateBack() {
        if (sourceFragment == "WatchList")
            mainActivityCall?.onFocusChangeToFav()
        if (sourceFragment == "HomeFrag")
            mainActivityCall?.onFocusChangeToAll()
        if (sourceFragment == "RecentFrag")
            mainActivityCall?.onFocusChangeToRec()
        if(sourceFragment=="SearchFragment"){
            searchFrag?.onFocusUp()
        }
    }

    override fun saveFocus(verti: Int, hori: Int) {
        mainActivityCall?.saveFocustoMain(verti, hori)
    }

    interface MainActivityCall {
        fun onFocusChangeToFav()
        fun onFocusChangeToAll()
        fun onFocusChangeToRec()
        fun onFocusChangeToSrh()
        fun saveFocustoMain(verti: Int, hori: Int)
        fun callDetail(movie: Movie)
    }
    fun setSearchFrag(callback: SearchFrag){
        searchFrag=callback
    }
    interface SearchFrag{
        fun onFocusUp()
    }

    fun setHomeFragInstance(callback: HomeFragCall) {
        homeFragCall = callback
    }

    interface HomeFragCall {
        fun updateView(movie: Movie)
    }

    fun setFocusVal(positionData: FocusValue?) {
        if (positionData != null) {
            Log.e("poiii", "${positionData.verticalPos}")
            Log.e("poiii", "${positionData.horiPos}")

            if (focusValue == null) {
                focusValue = FocusValue()
            }

            focusValue?.verticalPos = positionData.verticalPos
            focusValue?.horiPos = positionData.horiPos

            Log.e("pouuu", "${focusValue?.verticalPos}")
            Log.e("pouuu", "${focusValue?.horiPos}")
        }

    }

}