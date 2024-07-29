package com.example.mviapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = TodoAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        todoViewModel.viewState.observe(this, Observer { viewState ->
            adapter.submitList(viewState.todos)
        })

        findViewById<Button>(R.id.addButton).setOnClickListener {
            val newTodo = TodoItem(todoViewModel.getNextId(), "New Task", false)
            todoViewModel.processIntent(TodoIntent.AddTodoItem(newTodo))
        }
    }
}