package com.example.videoplayer.ui

import EntryAdapter
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.videoplayer.R
import com.example.videoplayer.data.Entry
import com.example.videoplayer.databinding.ActivityMainBinding
import com.example.videoplayer.databinding.MiniScreenBinding
import com.example.videoplayer.HomeScreenFrag
import com.example.videoplayer.ProfileFrag
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import SearchFrag
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    val viewModel by viewModels<AppViewModel>()
    private lateinit var adapter: EntryAdapter
    private var thumbnailPath: String? = null
    private var profilePicturePath: String? = null
    private var allBroadcast = BroadcastReciever()
//    private lateinit var receiver:DataBroadcastReciever

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        intentFilter.addAction("DATA_LOADED")

        setContentView(binding.root)

        registerReceiver(
            allBroadcast,
            intentFilter, RECEIVER_EXPORTED
        )

//        val viewModelFactory = AppViewModelFactory(applicationContext)
//        viewModel = ViewModelProvider(this).get(AppViewModel::class.java)
        val intent=Intent().apply {
            action="DATA_LOADED"
            putExtra("data","coming from fragment")
        }
         sendBroadcast(intent)
//        Log.d("actlife", "onCreate1")

        val home_screen = HomeScreenFrag()
        val search_frag = SearchFrag()
        val prof_frag = ProfileFrag()
        val favFrag = FavFragment()

        adapter = EntryAdapter(emptyList(), viewModel)

        loadFragment(home_screen)
        Log.d("actlife", "onCreate2")

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.miHome -> {
                    loadFragment(home_screen)
                    true
                }

                R.id.miSearch -> {
                    if (isDataAvailable())
                        loadFragment(search_frag)
                    else
                        Toast.makeText(this, "First Add Some DATA", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.miPerson -> {
                    loadFragment(prof_frag)
                    true
                }

                R.id.miFav -> {
                    if (isDataAvailable())
                        if (viewModel.getfav.value?.isNotEmpty() == true)
                            loadFragment(favFrag)
                        else
                            Toast.makeText(this, "First Add Some DATA", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }
        val fab: FloatingActionButton = findViewById(R.id.fab2)
        fab.setOnClickListener {
            showAddVideoMiniScreen()
        }
    }

    private fun isDataAvailable(): Boolean {
        return viewModel.entries.value?.isNotEmpty() == true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    private fun showAddVideoMiniScreen() {
        val dialogbinding = MiniScreenBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)

        val etVideoName = dialogbinding.etVideoName
        val etVideoLink = dialogbinding.etVideoLink
        val etUploaderName = dialogbinding.etUploaderName
        val btnAddVideo = dialogbinding.btnAddVideo
        val btnSelectThumbnail = dialogbinding.btnSelectThumbnail
        val btnSelectProfilePicture = dialogbinding.btnSelectProfilePicture


        btnSelectThumbnail.setOnClickListener {
            selectImage(THUMBNAIL_REQUEST_CODE)
        }

        btnSelectProfilePicture.setOnClickListener {
            selectImage(PROFILE_PICTURE_REQUEST_CODE)
        }

        btnAddVideo.setOnClickListener {
            val videoName = etVideoName.text.toString()
            val videoLink = etVideoLink.text.toString()
            val uploaderName = etUploaderName.text.toString()

            if (videoName.isNotEmpty() && videoLink.isNotEmpty() && uploaderName.isNotEmpty()) {
                val newEntry = Entry(
                    name = videoName,
                    link = videoLink,
                    uploader = uploaderName,
                    thumbnailPath = thumbnailPath,
                    profilePicture = profilePicturePath,
                    isFav = 0
                )
                viewModel.insertData(newEntry)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.setContentView(dialogbinding.root)
        dialog.show()
    }

    private fun selectImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            val imagePath = selectedImageUri?.toString()

            when (requestCode) {
                THUMBNAIL_REQUEST_CODE -> {
                    thumbnailPath = imagePath
                }

                PROFILE_PICTURE_REQUEST_CODE -> {
                    profilePicturePath = imagePath
                }
            }
        }
    }

    companion object {
        private const val THUMBNAIL_REQUEST_CODE = 1
        private const val PROFILE_PICTURE_REQUEST_CODE = 2
    }

    override fun onStart() {
        super.onStart()
        Log.d("actlife", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("actlife", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("actlife", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("actlife", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
//        unregisterReceiver(receiver)
//        unregisterReceiver(airplaneModeReceiver)
        Log.d("actlife", "onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("actlife", "onRestart")
    }
}