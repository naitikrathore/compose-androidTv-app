import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.videoplayer.R
import com.example.videoplayer.data.Entry
import com.example.videoplayer.databinding.SingleItemBinding
import com.example.videoplayer.ui.AppViewModel
import com.example.videoplayer.ui.VideoActivity
import dagger.hilt.android.AndroidEntryPoint


class EntryAdapter(private var entries: List<Entry>,val viewModel: AppViewModel)
    : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    inner class EntryViewHolder(private val binding: SingleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: Entry) {
            binding.tvName.text = entry.name
            binding.tvUploader.text = entry.uploader
//            entry.profilePicture

            Glide.with(binding.root.context)
                .load(entry.thumbnailPath)
                .placeholder(R.drawable.profile)
                .centerCrop()
                .into(binding.ivThumbnail)

//            // Load profile picture
            Glide.with(binding.root.context)
                .load(entry.profilePicture)
                .placeholder(R.drawable.profile)
                .centerCrop()
                .into(binding.ivProfilePicture)


            binding.cardView.setOnClickListener {
                val intent = Intent(binding.root.context, VideoActivity::class.java)
                Log.e(" nait", "${entry.link.toString()}")
                intent.putExtra("videoUrl", entry.link.toString()) // Pass the video URL as an extra
                binding.root.context.startActivity(intent)
            }

//
            if (entries.get(position).isFav==1) {
                binding.btfav.setBackgroundResource(R.drawable.baseline_favorite_24)
            } else {
                binding.btfav.setBackgroundResource(R.drawable.baseline_favorite_24wh)
            }

//            // Toggle favorite status on button click
            binding.btfav.setOnClickListener {
                val curEntry=entries.get(position)
//                Log.e("nai","${curEntry.name}")
                if (curEntry.isFav==1) {
                    binding.btfav.setBackgroundResource(R.drawable.baseline_favorite_24wh)
                     curEntry.isFav=0
                     viewModel.updaterepo(curEntry)

                } else {
                    curEntry.isFav=1
                    binding.btfav.setBackgroundResource(R.drawable.baseline_favorite_24)
                    viewModel.updaterepo(curEntry)
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = SingleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntryViewHolder(binding)
    }
    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
    }
    override fun getItemCount(): Int {
        return entries.size
    }

    fun updateData(newEntries: List<Entry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    fun getEntryAtPosition(position: Int): Entry {
        return entries[position]
    }
}
