package com.example.mviapp

class TodoRepo {
    private val todoList = mutableListOf(
        TodoItem(1, "item 1"),
        TodoItem(2, "item 2"),
        TodoItem(3, "item 3"),
        TodoItem(4, "item 4")
    )

    fun getTodos():List<TodoItem> = todoList
    fun addTodoItem(item: TodoItem) {
        todoList.add(item)
    }
    fun updateTodoItem(item: TodoItem) {
        val index = todoList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            todoList[index] = item
        }
    }
}