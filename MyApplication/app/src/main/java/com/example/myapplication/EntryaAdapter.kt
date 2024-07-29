package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EntryaAdapter (
    var mydata:List<Showdata>
):RecyclerView.Adapter<EntryaAdapter.ShowdataViewHolder>() {
    inner class ShowdataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowdataViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.activity_entry,parent,false)
        return ShowdataViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowdataViewHolder, position: Int) {
          holder.itemView.apply {
              var firstname=findViewById<TextView>(R.id.textView)
              firstname.text=mydata[position].firstname
              var secondname=findViewById<TextView>(R.id.textView2)
              secondname.text=mydata[position].lastname
              var country=findViewById<TextView>(R.id.textView3)
              country.text=mydata[position].country
          }
    }

    override fun getItemCount(): Int {
       return mydata.size
    }
}



