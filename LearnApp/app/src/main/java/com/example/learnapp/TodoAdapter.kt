package com.example.learnapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView


class TodoAdapter(
    var todos:List<Todo>
) :RecyclerView.Adapter<TodoAdapter.TodoViewHolder>(){
//    here we need to define define that this adapter is of recycle view
    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    //    it will hold the view that it will display to screen,  The current item
//    adapter need to know what data need to show to this view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
         val view=LayoutInflater.from(parent.context).inflate(R.layout.item_todo,parent, false)
         return TodoViewHolder(view)
    }
    override fun getItemCount(): Int {
        return todos.size


    }
    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.itemView.apply {
          var title = findViewById<TextView>(R.id.tvTitle)
            title.text=todos[position].title
          var cb= findViewById<CheckBox>(R.id.cbDone)
            cb.isChecked=todos[position].isChecked

        }
    }


}