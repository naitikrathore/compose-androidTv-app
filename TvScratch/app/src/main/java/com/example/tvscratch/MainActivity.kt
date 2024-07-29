package com.example.tvscratch

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvscratch.data.FocusValue
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity(), FocusChangeListener, VerticalAdapter.MainActivityCall,
    GridAdapter.OnItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private var lastVerti = -1
    private var lastHori = -1
    private var focusValue: FocusValue? = null
    private var handleSearch: SearchCall? = null


    override fun onFocusChangeToFavItem() {
        binding.tvAll.requestFocus()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("actlife", "onCreate")
        val homeFragment = HomeFragment()
        val recentFragment = RecentFragment()
        val searchFragment = SearchFragment()
        val watchlistFragment = WatchlistFragment()
        val detailFragment = DetailFragment()
        val splashFragment = SplashFragment()
        binding.LLnavigation.visibility = View.GONE
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, splashFragment)
//            .addToBackStack(null)
            .commit()


//        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, homeFragment)
//            .addToBackStack(null)
//            .commit()
//
        binding.tvAll.requestFocus()
        updateAppearanceText(binding.tvAll, true)

        binding.tvAll.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
//                focusValue = FocusValue(-1, -1)
                binding.tvAll.setTextColor(ContextCompat.getColor(this, R.color.black))
                binding.tvAll.setBackgroundResource(R.drawable.button_focused)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
//                    .addToBackStack(null)
                    .commit()
            } else {
                binding.tvAll.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvAll.setBackgroundResource(R.drawable.button_unfocused)
            }
        }

        binding.tvRecent.setOnFocusChangeListener { v, hasFocus ->
            updateAppearanceText(binding.tvRecent, hasFocus)
            if (hasFocus) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, recentFragment)
//                    .addToBackStack(null)
                    .commit()
            }
        }
        binding.tvWList.setOnFocusChangeListener { v, hasFocus ->
            updateAppearanceText(binding.tvWList, hasFocus)
            if (hasFocus) {
                val currentFragment =
                    supportFragmentManager.findFragmentById(R.id.fragment_container)
                Log.e("fragcheck", "${currentFragment}")
                if (currentFragment != watchlistFragment) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, watchlistFragment)
//                        .addToBackStack(null)
                        .commit()
                }
            }
        }
        binding.search.setOnFocusChangeListener { v, hasFocus ->
            updateAppearanceImage(binding.search, hasFocus)
//            if (hasFocus) {
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, searchFragment)
//                    .commit()
//            }
        }
        binding.settings.setOnFocusChangeListener { v, hasFocus ->
            updateAppearanceImage(binding.settings, hasFocus)
        }


        binding.tvAll.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        val exitFragment = ExitFragment()
                        binding.LLnavigation.visibility = View.GONE
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, exitFragment)
                            .commit()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        binding.tvAll.isSelected = true
                        return@setOnKeyListener false
                    }

                    else -> return@setOnKeyListener false
                }
            } else {
                false
            }
        }
        binding.tvRecent.setOnKeyListener { v, keyCode, event ->
            handleNavi(binding.tvRecent, keyCode, event)
        }
        binding.tvWList.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val recyclerView =
                    supportFragmentManager.findFragmentById(R.id.fragment_container)?.view?.findViewById<RecyclerView>(
                        R.id.innerRecyclerView
                    )
                recyclerView?.let {
                    it.requestFocus()
                    val layoutManager = it.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(0, 0)
                    return@setOnKeyListener true
                }
            }
            handleNavi(binding.tvWList, keyCode, event)
//            return@setOnKeyListener false
        }
        binding.search.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, searchFragment)
                            .commit()
                        Log.e("tttt","yp")
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val nextFocus = v.focusSearch(View.FOCUS_LEFT)
                        nextFocus?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        val nextFocus = v.focusSearch(View.FOCUS_RIGHT)
                        nextFocus?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_BACK -> {
                        val nextFocus = v.focusSearch(View.FOCUS_LEFT)
                        nextFocus?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
//                        val searchFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? SearchFragment
//                        searchFragment?.onFocusUp()
                        Log.e("PoError","Ofmain")

//                        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
//                        if(currentFragment==SearchFragment()) {
                            Log.e("PoError","main")
                            handleSearch=searchFragment
                            handleSearch?.handleFocus()
                            return@setOnKeyListener true
//                        }
                        return@setOnKeyListener false
                    }

                    else -> return@setOnKeyListener false
                }
            } else {
                false
            }
        }
        binding.settings.setOnKeyListener { v, keyCode, event ->
//            handleNavi(binding.settings, keyCode, event)
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                return@setOnKeyListener true
            }
            handleNavi(binding.settings, keyCode, event)
        }
    }

    override fun onFocusChangeToSrh() {
        binding.search.requestFocus()
    }

    override fun callDetail(movie: Movie) {
        openDetailsFragment(movie)
    }


    fun TopNavAll() {
        binding.tvAll.requestFocus()
    }

    fun TopNavRecent() {
        binding.tvRecent.requestFocus()
    }

    fun TopNavFav() {
        binding.tvWList.requestFocus()
    }

    fun TopNavSrh() {
        binding.search.requestFocus()
    }


    private fun updateAppearanceText(textView: TextView, hasFoucs: Boolean) {
        if (hasFoucs) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.black))
            textView.setBackgroundResource(R.drawable.button_focused)
        } else {
            textView.setTextColor(ContextCompat.getColor(this, R.color.white))
            textView.setBackgroundResource(R.drawable.button_unfocused)
        }
    }

    private fun updateAppearanceImage(imageView: ImageView, hasFocus: Boolean) {
        if (hasFocus) {
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.black))
            imageView.setBackgroundResource(R.drawable.button_focused)
        } else {
//            imageView.clearColorFilter()
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.white))
            imageView.setBackgroundResource(R.drawable.button_unfocused)
        }
    }

    private fun handleNavi(view: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    val nextFocus = view.focusSearch(View.FOCUS_LEFT)
                    nextFocus?.requestFocus()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val nextFocus = view.focusSearch(View.FOCUS_RIGHT)
                    nextFocus?.requestFocus()
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    val nextFocus = view.focusSearch(View.FOCUS_LEFT)
                    nextFocus?.requestFocus()
                    return true
                }

                else -> return false
            }
        }
        return false
    }

    fun openDetailsFragment(movie: Movie) {
        Log.e("detail", "ch")
        binding.LLnavigation.visibility = View.GONE
        val detailFragment = DetailFragment()
        detailFragment.setSelectedMovie(movie)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    fun backNavi() {
        supportFragmentManager.popBackStack()
        binding.LLnavigation.visibility = View.VISIBLE
//        saveFocustoMain(-1,-1)
    }

    override fun onFocusChangeToRec() {
        binding.tvRecent.requestFocus()
    }

    override fun onFocusChangeToAll() {
        binding.tvAll.requestFocus()
    }

    override fun onFocusChangeToFav() {
        Log.e("lete", "mainFoc")
        binding.tvWList.requestFocus()
    }

    override fun saveFocustoMain(vert: Int, hori: Int) {
        lastVerti = vert
        lastHori = hori
        focusValue = FocusValue(lastVerti, lastHori)
    }

    fun getPositionData(): FocusValue? {
        return focusValue
    }

    override fun onItemClick(movie: Movie) {
        openDetailsFragment(movie)
    }

    override fun onNavigateUp(sourceFragment: String) {
        when (sourceFragment) {
            "RecentFragment" -> TopNavRecent()
            "FavFragment" -> TopNavFav()
            "SearchFragment" -> TopNavSrh()
        }
    }

    interface SearchCall {
        fun handleFocus()
    }

}
