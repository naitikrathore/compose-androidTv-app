import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.DataContact
import com.example.contactapp.databinding.ItemContactBinding

class ContactAdapter : ListAdapter<DataContact, ContactAdapter.ContactViewHolder>(ContactsDiffUtils()) {
    private var contacts = listOf<DataContact>()

    inner class ContactViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: DataContact) {
            binding.tvName.text = contact.name
            binding.tvNum.text = contact.number
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int {
        return contacts.size
    }
    class ContactsDiffUtils: DiffUtil.ItemCallback<DataContact>() {
        override fun areItemsTheSame(oldItem: DataContact, newItem: DataContact): Boolean {
            return oldItem.id==newItem.id
        }

        override fun areContentsTheSame(oldItem: DataContact, newItem: DataContact): Boolean {
            return oldItem ==newItem
        }
    }

    fun setContacts(newContacts: List<DataContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

}


